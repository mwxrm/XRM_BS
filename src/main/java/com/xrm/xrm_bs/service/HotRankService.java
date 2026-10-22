package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


import java.util.Set;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotRankService {

    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final UserFollowRepository followRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final CommentRepository commentRepository;
    private final ArticleSettingRepository articleSettingRepository;

    private final RedisTemplate<String, String> redisTemplate;

    private static final double W1 = 3;   // 点赞权重
    private static final double W2 = 5;   // 收藏权重
    private static final double W3 = 8;   // 被引用权重
    private static final double W4 = 1;   // 浏览权重
    private static final double W5 = 10;  // 评论权重
    private static final double W6 = 6;   // 被文章关联权重（仅设定）
    private static final double G = 1.5;

    private static final String REDIS_KEY_ARTICLE = "hotrank:articles";
    private static final String REDIS_KEY_SETTING = "hotrank:settings";
    private static final int TOP_N = 100;
    private final UserLikeRepository userLikeRepository;
    private final UserSettingLikeRepository userSettingLikeRepository;

    /**
     * 每小时自动计算热榜
     */
    @Scheduled(cron = "0 0 * * * ?")
//    @Scheduled(fixedRate = 120_000)       //两分钟
    @Transactional(readOnly = true)
    public void computeHotRanks() {
        computeArticleRank();
        computeSettingRank();
    }

    private void computeArticleRank() {
        List<Article> articles = articleRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(
                Article.ArticleStatus.published);
        Map<Long, Double> scores = new HashMap<>();

        System.out.println("开始计算文章热榜，共 " + articles.size() + " 篇文章");

        // 批量收集 ID
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());

        // 批量统计指标
        Map<Long, Long> likeCounts = getLikeCounts(articleIds, "article");
        Map<Long, Long> favCounts = getFavCounts(articleIds, "article");
        Map<Long, Long> commentCounts = getCommentCounts(articleIds, "article");
        Map<Long, Long> childCounts = getChildArticleCounts(articleIds);
        Map<Long, Long> viewCounts = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a.getViewCount().longValue()));

        // 作者粉丝数缓存
        Map<Long, Long> followerCounts = getFollowerCounts(
                articles.stream().map(a -> a.getAuthor().getId()).distinct().collect(Collectors.toList()));

        for (Article a : articles) {
            long id = a.getId();
            long followers = followerCounts.getOrDefault(a.getAuthor().getId(), 0L);
            double base = Math.log(followers + 1) * 10;

            long likes = likeCounts.getOrDefault(id, 0L);
            long favs = favCounts.getOrDefault(id, 0L);
            long comments = commentCounts.getOrDefault(id, 0L);
            long children = childCounts.getOrDefault(id, 0L);
            long views = viewCounts.getOrDefault(id, 0L);

            double numerator = base + W1 * likes + W2 * favs + W3 * children + W4 * views + W5 * comments;
            long hours = Duration.between(a.getCreatedAt(), LocalDateTime.now()).toHours();
            double denominator = Math.pow(hours + 2, G);
            double score = numerator / denominator;
            scores.put(id, score);
        }

        System.out.println("文章热榜计算完成，分数数量：" + scores.size());
        storeToRedis(REDIS_KEY_ARTICLE, scores);
        System.out.println("已存入 Redis，key: " + REDIS_KEY_ARTICLE);

        storeToRedis(REDIS_KEY_ARTICLE, scores);
    }

    private void computeSettingRank() {
        List<Setting> settings = settingRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(
                Setting.SettingStatus.published);
        Map<Long, Double> scores = new HashMap<>();

        System.out.println("开始计算设定热榜，共 " + settings.size() + " 个设定");

        List<Long> settingIds = settings.stream().map(Setting::getId).collect(Collectors.toList());

        Map<Long, Long> likeCounts = getLikeCounts(settingIds, "setting");
        Map<Long, Long> favCounts = getFavCounts(settingIds, "setting");
        Map<Long, Long> commentCounts = getCommentCounts(settingIds, "setting");
        Map<Long, Long> childCounts = getChildSettingCounts(settingIds);
        Map<Long, Long> relatedCounts = getArticleSettingCounts(settingIds);
        Map<Long, Long> viewCounts = settings.stream()
                .collect(Collectors.toMap(Setting::getId, s -> s.getViewCount().longValue()));

        Map<Long, Long> followerCounts = getFollowerCounts(
                settings.stream().map(s -> s.getAuthor().getId()).distinct().collect(Collectors.toList()));

        for (Setting s : settings) {
            long id = s.getId();
            long followers = followerCounts.getOrDefault(s.getAuthor().getId(), 0L);
            double base = Math.log(followers + 1) * 10;

            long likes = likeCounts.getOrDefault(id, 0L);
            long favs = favCounts.getOrDefault(id, 0L);
            long comments = commentCounts.getOrDefault(id, 0L);
            long children = childCounts.getOrDefault(id, 0L);
            long related = relatedCounts.getOrDefault(id, 0L);
            long views = viewCounts.getOrDefault(id, 0L);

            double numerator = base + W1 * likes + W2 * favs + W3 * children + W4 * views + W5 * comments + W6 * related;
            long hours = Duration.between(s.getCreatedAt(), LocalDateTime.now()).toHours();
            double denominator = Math.pow(hours + 2, G);
            double score = numerator / denominator;
            scores.put(id, score);
        }

        System.out.println("文章热榜计算完成，分数数量：" + scores.size());
        storeToRedis(REDIS_KEY_SETTING, scores);
        System.out.println("已存入 Redis，key: " + REDIS_KEY_SETTING);


        storeToRedis(REDIS_KEY_SETTING, scores);
    }

    // ============ 批量统计辅助方法 ============

    private Map<Long, Long> getLikeCounts(List<Long> ids, String type) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : ids) {
            long count = "article".equals(type)
                    ? userLikeRepository.countByArticleId(id)
                    : userSettingLikeRepository.countBySettingId(id);
            result.put(id, count);
        }
        return result;
    }

    private Map<Long, Long> getFavCounts(List<Long> ids, String itemType) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : ids) {
            long count = favoriteItemRepository.countByItemTypeAndItemId(itemType, id);
            result.put(id, count);
        }
        return result;
    }

    private Map<Long, Long> getCommentCounts(List<Long> ids, String targetType) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : ids) {
            long count = commentRepository.countByTarget(targetType, id);
            result.put(id, count);
        }
        return result;
    }

    private Map<Long, Long> getChildArticleCounts(List<Long> ids) {
        // 根据 parentArticleId 统计
        Map<Long, Long> map = new HashMap<>();
        for (Long id : ids) {
            long count = articleRepository.countByParentArticleIdAndStatus(id, Article.ArticleStatus.published);
            map.put(id, count);
        }
        return map;
    }

    private Map<Long, Long> getChildSettingCounts(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        for (Long id : ids) {
            long count = settingRepository.countByParentSettingIdAndStatus(id, Setting.SettingStatus.published);
            map.put(id, count);
        }
        return map;
    }

    private Map<Long, Long> getArticleSettingCounts(List<Long> settingIds) {
        Map<Long, Long> map = new HashMap<>();
        for (Long sid : settingIds) {
            long count = articleSettingRepository.countBySettingId(sid);
            map.put(sid, count);
        }
        return map;
    }

    private Map<Long, Long> getFollowerCounts(List<Long> authorIds) {
        Map<Long, Long> map = new HashMap<>();
        for (Long authorId : authorIds) {
            long count = followRepository.countByFolloweeId(authorId);
            map.put(authorId, count);
        }
        return map;
    }

    private void storeToRedis(String key, Map<Long, Double> scores) {
        redisTemplate.delete(key);
        if (scores.isEmpty()) return;
        // 取前 TOP_N
        scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_N)
                .forEach(entry -> redisTemplate.opsForZSet().add(key, entry.getKey().toString(), entry.getValue()));
        // 设置过期时间（2小时，避免下次更新前被删除）
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    // 提供给控制器的查询方法
    public List<Map<String, Object>> getArticleHotRank(int offset, int count) {
        return getHotRankFromRedis(REDIS_KEY_ARTICLE, offset, count);
    }

    public List<Map<String, Object>> getSettingHotRank(int offset, int count) {
        return getHotRankFromRedis(REDIS_KEY_SETTING, offset, count);
    }

    private List<Map<String, Object>> getHotRankFromRedis(String key, int offset, int count) {
        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, offset + count - 1);
        if (tuples == null || tuples.isEmpty()) return Collections.emptyList();
        return tuples.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", Long.parseLong(t.getValue()));
            map.put("score", t.getScore());
            return map;
        }).collect(Collectors.toList());
    }





    // ========== 提供含简要信息的接口 ==========

    public List<Map<String, Object>> getArticleHotRankDetails(int offset, int count) {
        List<Map<String, Object>> rankList = getArticleHotRank(offset, count);
        if (rankList.isEmpty()) return Collections.emptyList();
        List<Long> ids = rankList.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
        Map<Long, Article> articleMap = articleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : rankList) {
            Long id = (Long) entry.get("id");
            Article a = articleMap.get(id);
            if (a == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("author", a.getAuthor().getUsername());
            item.put("authorId", a.getAuthor().getId());
            item.put("viewCount", a.getViewCount());
            item.put("likeCount", a.getLikeCount());
            item.put("createdAt", a.getCreatedAt().toString());
            item.put("score", entry.get("score"));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getSettingHotRankDetails(int offset, int count) {
        List<Map<String, Object>> rankList = getSettingHotRank(offset, count);
        if (rankList.isEmpty()) return Collections.emptyList();
        List<Long> ids = rankList.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
        Map<Long, Setting> settingMap = settingRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Setting::getId, s -> s));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : rankList) {
            Long id = (Long) entry.get("id");
            Setting s = settingMap.get(id);
            if (s == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("title", s.getTitle());
            item.put("author", s.getAuthor().getUsername());
            item.put("authorId", s.getAuthor().getId());
            item.put("viewCount", s.getViewCount());
            item.put("likeCount", s.getLikeCount());
            item.put("createdAt", s.getCreatedAt().toString());
            item.put("score", entry.get("score"));
            // 也可加上分类
            item.put("category", s.getCategory().name());
            result.add(item);
        }
        return result;
    }


    public Map<Long, Double> getArticleHotScoreMap() {
        return getScoresFromRedis(REDIS_KEY_ARTICLE);
    }

    public Map<Long, Double> getSettingHotScoreMap() {
        return getScoresFromRedis(REDIS_KEY_SETTING);
    }

    private Map<Long, Double> getScoresFromRedis(String key) {
        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
        if (tuples == null) return Collections.emptyMap();
        return tuples.stream()
                .collect(Collectors.toMap(
                        t -> Long.parseLong(t.getValue()),
                        t -> t.getScore()
                ));
    }



    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        computeHotRanks();
    }


}