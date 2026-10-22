package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.*;
import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestDataService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final UserLikeRepository userLikeRepository;
    private final UserSettingLikeRepository userSettingLikeRepository;
    private final CommentRepository commentRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleSettingRepository articleSettingRepository;

    // 收藏相关 Repository 按需引入
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final FavoriteItemRepository favoriteItemRepository;

    private final Random random = new Random();

    @Transactional
    public Map<String, Object> generateTestData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 创建标签
        System.out.println("=== 开始生成测试数据 ===");
        List<Tag> tags = createTags(20);
        System.out.println("标签创建数量: " + tags.size());
        result.put("tags", tags.size());

        // 2. 创建用户
        List<User> users = createUsers(15);
        System.out.println("用户创建数量: " + users.size());
        result.put("users", users.size());

        // 3. 每个用户创建文章和设定
        List<Article> allArticles = new ArrayList<>();
        List<Setting> allSettings = new ArrayList<>();
        for (User user : users) {
            int articleCount = 3 + random.nextInt(5);
            int settingCount = 2 + random.nextInt(3);
            for (int i = 0; i < articleCount; i++) {
                allArticles.add(createArticle(user, tags));
            }
            for (int i = 0; i < settingCount; i++) {
                allSettings.add(createSetting(user, tags));
            }
        }
        result.put("articles", allArticles.size());
        result.put("settings", allSettings.size());
        System.out.println("文章总数: " + allArticles.size());
        System.out.println("设定总数: " + allSettings.size());


        // 4. 随机产生互动
        int likeCount = createRandomLikes(users, allArticles, allSettings);
        int favoriteCount = createRandomFavorites(users, allArticles, allSettings);
        int commentCount = createRandomComments(users, allArticles, allSettings);
        int followCount = createRandomFollows(users);
        int linkCount = createRandomArticleSettingLinks(allArticles, allSettings);

        result.put("likes", likeCount);
        System.out.println("点赞数: " + likeCount);
        result.put("favorites", favoriteCount);
        System.out.println("收藏数: " + favoriteCount);
        result.put("comments", commentCount);
        System.out.println("评论数: " + commentCount);
        result.put("follows", followCount);
        System.out.println("关注数: " + followCount);
        result.put("articleSettingLinks", linkCount);
        System.out.println("文章设定链接数: " + linkCount);
        return result;
    }

    private List<Tag> createTags(int count) {
        String[] sampleNames = {"科幻", "奇幻", "武侠", "都市", "历史", "悬疑", "轻小说", "人物设定", "物品设定",
                "世界观", "魔法", "修仙", "系统", "穿越", "重生", "游戏", "动漫", "影视", "原创", "同人"};
        List<Tag> tags = new ArrayList<>();
        for (String name : Arrays.copyOf(sampleNames, count)) {
            String finalName = name;
            // 如果存在，加随机数字后缀
            while (tagRepository.existsByName(finalName)) {
                finalName = name + "_" + random.nextInt(1000);
            }
            Tag tag = new Tag();
            tag.setName(finalName);
            tag.setType(Tag.TagType.article);
            tag.setClassification(Tag.Classification.article);
            tag.setUsageCount(0);
            tags.add(tagRepository.save(tag));
        }
        return tags;
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String username = "testuser" + i;
            String email = "testuser" + i + "@example.com";
            // 如果用户名或邮箱已存在，加随机数
            while (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
                username = "testuser" + i + "_" + random.nextInt(10000);
                email = username + "@example.com";
            }
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole("user");
            users.add(userRepository.save(user));
        }
        return users;
    }

    private Article createArticle(User author, List<Tag> tags) {
        Article article = new Article();
        article.setTitle(generateTitle());
        article.setContent(generateContent());
        article.setAuthor(author);
        article.setStatus(Article.ArticleStatus.published);
        article.setDeleted(false);
        article.setViewCount(random.nextInt(500));
        article.setLikeCount(0); // 实际点赞数由互动表统计
        article.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));
        article.setTags(new HashSet<>(getRandomTags(tags, 2, 4)));
        return articleRepository.save(article);
    }

    private Setting createSetting(User author, List<Tag> tags) {
        Setting setting = new Setting();
        setting.setTitle("设定-" + UUID.randomUUID().toString().substring(0, 6));
        setting.setContent("本设定是关于" + setting.getTitle() + "的详细描述...\n包含背景、规则等。");
        setting.setCategory(Setting.SettingCategory.values()[random.nextInt(Setting.SettingCategory.values().length)]);
        setting.setAuthor(author);
        setting.setStatus(Setting.SettingStatus.published);
        setting.setDeleted(false);
        setting.setViewCount(random.nextInt(300));
        setting.setLikeCount(0);
        setting.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));
        setting.setTags(new HashSet<>(getRandomTags(tags, 2, 4)));
        return settingRepository.save(setting);
    }

    private int createRandomLikes(List<User> users, List<Article> articles, List<Setting> settings) {
        int count = 0;
        for (User user : users) {
            List<Article> sampledArticles = getRandomItems(articles, random.nextInt(10));
            for (Article a : sampledArticles) {
                if (userLikeRepository.existsByUserIdAndArticleId(user.getId(), a.getId())) continue;
                UserLike like = new UserLike();
                like.setUserId(user.getId());
                like.setArticleId(a.getId());
                userLikeRepository.save(like);
                count++;
            }
            List<Setting> sampledSettings = getRandomItems(settings, random.nextInt(5));
            for (Setting s : sampledSettings) {
                if (userSettingLikeRepository.existsByUserIdAndSettingId(user.getId(), s.getId())) continue;
                UserSettingLike like = new UserSettingLike();
                like.setUserId(user.getId());
                like.setSettingId(s.getId());
                userSettingLikeRepository.save(like);
                count++;
            }
        }
        return count;
    }

    private int createRandomFavorites(List<User> users, List<Article> articles, List<Setting> settings) {
        int count = 0;
        for (User user : users) {
            // 获取或创建默认收藏夹
            FavoriteFolder folder = getOrCreateDefaultFolder(user.getId());
            for (Article a : getRandomItems(articles, random.nextInt(5))) {
                if (favoriteItemRepository.existsByFolderIdAndItemTypeAndItemId(folder.getId(), "article", a.getId())) continue;
                FavoriteItem item = new FavoriteItem();
                item.setFolder(folder);
                item.setItemType("article");
                item.setItemId(a.getId());
                favoriteItemRepository.save(item);
                count++;
            }
            for (Setting s : getRandomItems(settings, random.nextInt(3))) {
                if (favoriteItemRepository.existsByFolderIdAndItemTypeAndItemId(folder.getId(), "setting", s.getId())) continue;
                FavoriteItem item = new FavoriteItem();
                item.setFolder(folder);
                item.setItemType("setting");
                item.setItemId(s.getId());
                favoriteItemRepository.save(item);
                count++;
            }
        }
        return count;
    }

    private FavoriteFolder getOrCreateDefaultFolder(Long userId) {
        return favoriteFolderRepository.findByUserIdAndName(userId, "默认收藏")
                .orElseGet(() -> {
                    FavoriteFolder folder = new FavoriteFolder();
                    folder.setUserId(userId);
                    folder.setName("默认收藏");
                    folder.setDescription("系统默认收藏夹");
                    return favoriteFolderRepository.save(folder);
                });
    }

private int createRandomComments(List<User> users, List<Article> articles, List<Setting> settings) {
    int count = 0;
    if (articles.isEmpty() && settings.isEmpty()) return count;
    for (int i = 0; i < 80; i++) {
        User user = users.get(random.nextInt(users.size()));
        Comment comment = new Comment();
        comment.setUserId(user.getId());
        // 确保至少有一个可评论的目标
        if (!articles.isEmpty() && (random.nextBoolean() || settings.isEmpty())) {
            Article a = articles.get(random.nextInt(articles.size()));
            comment.setTargetType("article");
            comment.setTargetId(a.getId());
        } else if (!settings.isEmpty()) {
            Setting s = settings.get(random.nextInt(settings.size()));
            comment.setTargetType("setting");
            comment.setTargetId(s.getId());
        } else {
            continue; // 没有可评论的内容，跳过
        }
        comment.setContent("这是一条测试评论，自动生成 #" + i);
        comment.setStatus(1);
        comment.setLikeCount(0);
        commentRepository.save(comment);
        count++;
    }
    return count;
}

    private int createRandomFollows(List<User> users) {
        int count = 0;
        for (User follower : users) {
            List<User> followees = getRandomItems(users, random.nextInt(5));
            for (User followee : followees) {
                if (follower.getId().equals(followee.getId())) continue;
                if (userFollowRepository.existsByFollowerIdAndFolloweeId(follower.getId(), followee.getId())) continue;
                UserFollow follow = new UserFollow();
                follow.setFollowerId(follower.getId());
                follow.setFolloweeId(followee.getId());
                userFollowRepository.save(follow);
                count++;
            }
        }
        return count;
    }

    private int createRandomArticleSettingLinks(List<Article> articles, List<Setting> settings) {
        int count = 0;
        for (Article a : articles) {
            int linkCount = random.nextInt(4);
            List<Setting> sampled = getRandomItems(settings, linkCount);
            for (Setting s : sampled) {
                if (articleSettingRepository.existsByArticleIdAndSettingId(a.getId(), s.getId())) continue;
                ArticleSetting link = new ArticleSetting();
                link.setArticleId(a.getId());
                link.setSettingId(s.getId());
                articleSettingRepository.save(link);
                count++;
            }
        }
        return count;
    }

    private <T> List<T> getRandomItems(List<T> list, int n) {
        if (n <= 0) return Collections.emptyList();
        List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(n, shuffled.size()));
    }

    private Set<Tag> getRandomTags(List<Tag> allTags, int min, int max) {
        int count = min + random.nextInt(max - min + 1);
        return new HashSet<>(getRandomItems(allTags, count));
    }

    private String generateTitle() {
        String[] titles = {"异世界冒险", "都市传说", "最后的法师", "机械纪元", "星空下的约定",
                "重生之商业帝国", "修仙日记", "末日求生", "游戏世界", "星辰大海", "时间旅行者", "剑与魔法",
                "武林秘史", "超能力学院", "梦境师", "未来世界", "洪荒神话", "都市兵王", "系统降临", "穿越异界"};
        return titles[random.nextInt(titles.length)] + " " + random.nextInt(100);
    }

    private String generateContent() {
        return "这是自动生成的测试文章内容。本文描述了一个发生在" +
                LocalDateTime.now().minusDays(random.nextInt(30)).toLocalDate() +
                "的故事，涉及多个角色和复杂的情节。文章包含丰富的细节，用于测试系统的各项功能。" +
                "\n\n" + "这是一段补充文字。".repeat(random.nextInt(3) + 1);
    }
}