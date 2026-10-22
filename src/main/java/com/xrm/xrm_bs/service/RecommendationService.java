package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.*;
import com.xrm.xrm_bs.repository.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final RecentViewRepository recentViewRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserPreferredTagRepository userPreferredTagRepository;
    private final UserBlockedTagRepository userBlockedTagRepository;
    private final HotRankService hotRankService;
    private final UserBehaviorLogRepository behaviorLogRepository;

    private final RedisTemplate<String, String> redisTemplate;

    private static final int PRE_GENERATE_COUNT = 50;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(10);

    /**
     * 获取推荐列表（分页，可选按类型过滤）
     */
    public List<Map<String, Object>> getRecommendations(Long userId, String itemType, int offset, int size, boolean refresh) {
        String cacheKey = "recommend:" + userId + (itemType != null ? ":" + itemType : "");


        if (refresh) {
            redisTemplate.delete(cacheKey);
        }

        // 从 Redis 取缓存
        List<String> cachedIds = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cachedIds == null || cachedIds.isEmpty()) {
            // 重新生成推荐候选（带类型过滤）
            List<ScoredItem> scoredItems = generateRecommendations(userId, itemType, PRE_GENERATE_COUNT);
            if (scoredItems.isEmpty()) return Collections.emptyList();

            // 存入 Redis
            List<String> idScorePairs = scoredItems.stream()
                    .map(item -> item.getType() + ":" + item.getId() + ":" + item.getScore())
                    .collect(Collectors.toList());
            redisTemplate.opsForList().rightPushAll(cacheKey, idScorePairs);
            redisTemplate.expire(cacheKey, CACHE_DURATION);
        }

        // 重新获取缓存（已存在或刚刚创建）
        List<String> allItems = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (allItems == null || allItems.isEmpty()) return Collections.emptyList();

        // 截取当前页
        int fromIndex = Math.min(offset, allItems.size());
        int toIndex = Math.min(offset + size, allItems.size());
        List<String> pageItems = allItems.subList(fromIndex, toIndex);

        // 解析并填充详情，同时过滤无效内容
        return parseCacheToDetails(pageItems);
    }

    /**
     * 生成推荐候选（支持按类型过滤）
     */
    private List<ScoredItem> generateRecommendations(Long userId, String filterType, int maxCount) {
        List<Article> articles = articleRepository.findAllByStatusAndDeletedFalse(Article.ArticleStatus.published);
        List<Setting> settings = settingRepository.findAllByStatusAndDeletedFalse(Setting.SettingStatus.published);

        Set<Long> preferredTagIds = userPreferredTagRepository.findByUserId(userId).stream()
                .map(ut -> ut.getTag().getId()).collect(Collectors.toSet());
        Set<Long> blockedTagIds = userBlockedTagRepository.findByUserId(userId).stream()
                .map(bt -> bt.getTag().getId()).collect(Collectors.toSet());
        Set<Long> followeeIds = new HashSet<>(userFollowRepository.findFolloweeIdsByFollowerId(userId));

        Map<Long, Double> articleHotScores = hotRankService.getArticleHotScoreMap();
        Map<Long, Double> settingHotScores = hotRankService.getSettingHotScoreMap();

        List<ScoredItem> items = new ArrayList<>();

        // 处理文章
        if (filterType == null || "article".equals(filterType)) {
            for (Article a : articles) {
                if (isBlockedByTag(a.getTags(), blockedTagIds)) continue;
                if (recentViewRepository.existsByUserIdAndItemIdAndItemType(userId, a.getId(), "article")) continue;

                double hotScore = articleHotScores.getOrDefault(a.getId(), 0.0);
                double tagScore = calculateTagScore(a.getTags(), preferredTagIds);
                double followScore = followeeIds.contains(a.getAuthor().getId()) ? 1.0 : 0.0;
                double finalScore = hotScore * 0.4 + tagScore * 0.4 + followScore * 0.2;

                // 点击反馈
                long clickCount = behaviorLogRepository.countRecentRecommendClicks(a.getId(), "article", LocalDateTime.now().minusHours(24));
                finalScore += Math.log(clickCount + 1) * 0.5;

                items.add(new ScoredItem("article", a.getId(), finalScore));
            }
        }

        // 处理设定
        if (filterType == null || "setting".equals(filterType)) {
            for (Setting s : settings) {
                if (isBlockedByTag(s.getTags(), blockedTagIds)) continue;
                if (recentViewRepository.existsByUserIdAndItemIdAndItemType(userId, s.getId(), "setting")) continue;

                double hotScore = settingHotScores.getOrDefault(s.getId(), 0.0);
                double tagScore = calculateTagScore(s.getTags(), preferredTagIds);
                double followScore = followeeIds.contains(s.getAuthor().getId()) ? 1.0 : 0.0;
                double finalScore = hotScore * 0.4 + tagScore * 0.4 + followScore * 0.2;

                long clickCount = behaviorLogRepository.countRecentRecommendClicks(s.getId(), "setting", LocalDateTime.now().minusHours(24));
                finalScore += Math.log(clickCount + 1) * 0.5;

                items.add(new ScoredItem("setting", s.getId(), finalScore));
            }
        }

        items.sort(Comparator.comparing(ScoredItem::getScore).reversed());
        List<ScoredItem> topItems = new ArrayList<>(items.subList(0, Math.min(items.size(), maxCount)));
        Collections.shuffle(topItems);
//        return items.subList(0, Math.min(items.size(), maxCount));
        return topItems;
    }


    /**
     * 解析缓存条目并填充详细信息，同时过滤无效内容
     */
    private List<Map<String, Object>> parseCacheToDetails(List<String> idScorePairs) {
        List<Long> articleIds = new ArrayList<>();
        List<Long> settingIds = new ArrayList<>();
        List<Map<String, Object>> rawResults = new ArrayList<>();

        for (String item : idScorePairs) {
            String[] parts = item.split(":");
            if (parts.length != 3) continue;
            String type = parts[0];
            Long id = Long.parseLong(parts[1]);
            double score = Double.parseDouble(parts[2]);

            Map<String, Object> map = new HashMap<>();
            map.put("type", type);
            map.put("id", id);
            map.put("score", score);
            rawResults.add(map);

            if ("article".equals(type)) articleIds.add(id);
            else if ("setting".equals(type)) settingIds.add(id);
        }

        // 批量查询实体
        Map<Long, Article> articleMap = articleIds.isEmpty() ? Collections.emptyMap() :
                articleRepository.findAllById(articleIds).stream().collect(Collectors.toMap(Article::getId, a -> a));
        Map<Long, Setting> settingMap = settingIds.isEmpty() ? Collections.emptyMap() :
                settingRepository.findAllById(settingIds).stream().collect(Collectors.toMap(Setting::getId, s -> s));

        List<Map<String, Object>> validResults = new ArrayList<>();
        for (Map<String, Object> map : rawResults) {
            Long id = (Long) map.get("id");
            if ("article".equals(map.get("type"))) {
                Article a = articleMap.get(id);
                if (a == null || a.getDeleted() || a.getStatus() != Article.ArticleStatus.published) continue;
                map.put("title", a.getTitle());
                map.put("authorUsername", a.getAuthor().getUsername());
                map.put("viewCount", a.getViewCount());
                map.put("likeCount", a.getLikeCount());
            } else {
                Setting s = settingMap.get(id);
                if (s == null || s.getDeleted() || s.getStatus() != Setting.SettingStatus.published) continue;
                map.put("title", s.getTitle());
                map.put("authorUsername", s.getAuthor().getUsername());
                map.put("viewCount", s.getViewCount());
                map.put("likeCount", s.getLikeCount());
            }
            validResults.add(map);
        }
        return validResults;
    }

    private boolean isBlockedByTag(Set<Tag> tags, Set<Long> blockedTagIds) {
        return tags.stream().anyMatch(t -> blockedTagIds.contains(t.getId()));
    }

    private double calculateTagScore(Set<Tag> tags, Set<Long> preferredTagIds) {
        if (preferredTagIds.isEmpty() || tags.isEmpty()) return 0.0;
        long matchCount = tags.stream().filter(t -> preferredTagIds.contains(t.getId())).count();
        return (double) matchCount / tags.size(); // 匹配率
    }

    @Data
    @AllArgsConstructor
    private static class ScoredItem {
        private String type;
        private Long id;
        private double score;
    }
}