package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.ArticleResponse;
import com.xrm.xrm_bs.dto.CreateArticleRequest;
import com.xrm.xrm_bs.dto.UpdateArticleRequest;
import com.xrm.xrm_bs.entity.*;
import com.xrm.xrm_bs.repository.*;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ArticleService {


    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final SettingRepository settingRepository;
    private final ArticleSettingRepository articleSettingRepository;
    private final UserBehaviorLogService behaviorLogService;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;



    @Transactional
    public Article createArticle(CreateArticleRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setAuthor(author);
        article.setStatus(Article.ArticleStatus.draft);

        // 1. 处理标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                Tag tag = findOrCreateTag(tagName);
                tags.add(tag);
            }
            article.setTags(tags);
        }

        // 2. 处理引用关系
        if (request.getParentArticleId() != null) {
            Article parent = articleRepository.findById(request.getParentArticleId())
                    .orElseThrow(() -> new RuntimeException("引用的文章不存在"));
            article.setParentArticle(parent);
        }

        // 3. 统一保存文章
        Article savedArticle = articleRepository.save(article);

        // 4. 更新标签使用次数（如果有标签）
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            updateTagUsageCounts(article.getTags());
        }

        return savedArticle;
    }

    // 独立的标签查找/创建方法
    private Tag findOrCreateTag(String tagName) {
        return tagRepository.findByNameAndType(tagName, Tag.TagType.article)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setType(Tag.TagType.article);
                    newTag.setClassification(Tag.Classification.article);
                    newTag.setUsageCount(0);
                    return tagRepository.save(newTag);
                });
    }

    // 使用新的事务更新标签使用次数
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTagUsageCounts(Set<Tag> tags) {
        List<Long> tagIds = tags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());

        if (tagService != null) {
            tagService.incrementTagsUsage(tagIds);
        }
    }

    @Transactional
    public Article updateArticle(Long articleId, UpdateArticleRequest request, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 检查权限：只有作者可以修改
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权修改此文章");
        }

        article.setTitle(request.getTitle());
        article.setContent(request.getContent());

        // 更新标签
        if (request.getTags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            return tagRepository.save(newTag);
                        });
                tags.add(tag);
            }
            // 清除旧标签，添加新标签
            article.getTags().clear();
            article.getTags().addAll(tags);
        }

        // 处理引用关系：仅当请求中明确传递了 parentArticleId 时才修改
        // 注意：request.getParentArticleId() 在字段缺失时返回 null
        if (request.getParentArticleId() != null) {
            // 不能引用自己
            if (request.getParentArticleId().equals(articleId)) {
                throw new RuntimeException("不能引用自己的文章");
            }
            Article newParent = articleRepository.findById(request.getParentArticleId())
                    .orElseThrow(() -> new RuntimeException("引用的文章不存在"));
            // 检查是否会造成循环引用
            if (isCircularReference(newParent, articleId)) {
                throw new RuntimeException("不能形成循环引用");
            }
            article.setParentArticle(newParent);
        }
        // 如果请求中没有 parentArticleId，则保持原有父文章不变（什么都不做）

        return articleRepository.save(article);
    }

    @Transactional
    public Article publishArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 检查权限：只有作者可以发布
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权发布此文章");
        }

        article.setStatus(Article.ArticleStatus.published);
        behaviorLogService.log(userId, articleId, "article", "publish");
        return articleRepository.save(article);
    }

    @Transactional
    public void deleteArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权删除此文章");
        }
        article.setDeleted(true);
        articleRepository.save(article);
    }

    @Transactional(readOnly = true)
    public Article getArticleEntityById(Long id) {
        return articleRepository.findByIdWithAll(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Article> getArticlesByTag(String tagName) {
        return articleRepository.findByTagName(tagName);
    }

    @Transactional(readOnly = true)
    public List<Article> getMyArticles(Long userId) {
        return articleRepository.findByAuthorIdAndDeletedFalse(userId);
    }


    @Transactional(readOnly = true)
    public List<Article> getMyDrafts(Long userId) {
        return articleRepository.findByAuthorIdAndStatusAndDeletedFalse(userId, Article.ArticleStatus.draft);
    }

    // 分页查询方法
    @Transactional(readOnly = true)
    public Page<Article> getPublishedArticles(Pageable pageable) {
        return articleRepository.findByStatus(Article.ArticleStatus.published, pageable);
    }

    private boolean isCircularReference(Article article, Long targetId) {
        Article current = article;
        while (current != null) {
            if (current.getId().equals(targetId)) {
                return true;
            }
            current = current.getParentArticle();
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Page<Article> getChildrenByParentId(Long parentId, Pageable pageable) {
        return articleRepository.findByParentArticleIdAndStatus(parentId, Article.ArticleStatus.published, pageable);
    }

    public Page<Article> searchArticles(String keyword, Set<String> tags, Pageable pageable) {
        Specification<Article> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Article.ArticleStatus.published));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (StringUtils.hasText(keyword)) {
                jakarta.persistence.criteria.Predicate titleLike = cb.like(root.get("title"), "%" + keyword + "%");
                jakarta.persistence.criteria.Predicate contentLike = cb.like(root.get("content"), "%" + keyword + "%");
                predicates.add(cb.or(titleLike, contentLike));
            }
            if (tags != null && !tags.isEmpty()) {
                for (String tagName : tags) {
                    Join<Article, Tag> tagJoin = root.join("tags");
                    predicates.add(cb.equal(tagJoin.get("name"), tagName));
                }
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return articleRepository.findAll(spec, pageable);
    }

    public Page<Article> getPublishedArticlesByUserId(Long userId, Pageable pageable) {
        return articleRepository.findByAuthorIdAndStatusAndDeletedFalse(userId, Article.ArticleStatus.published, pageable);
    }

    /**
     * 注册浏览，返回 true 表示本次计入有效浏览，false 表示重复不计
     */
    public boolean registerView(Long articleId, Long userId, String ip, String userAgent) {
        // 生成去重 key：view:article:{articleId}:{date}:{ip}:{userAgentHash}
        String date = LocalDate.now().toString();
        String uaHash = DigestUtils.md5DigestAsHex(userAgent.getBytes()); // 简化 User-Agent 哈希
        String key = String.format("view:article:%d:%s:%s:%s", articleId, date, ip, uaHash);
        // 如果已存在则直接返回 false
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(1));
        if (Boolean.TRUE.equals(success)) {
            // 增加文章浏览量
            articleRepository.incrementViewCount(articleId);
            return true;
        }
        return false;
    }


    // 获取文章关联的设定列表
    @Transactional(readOnly = true)
    public List<Setting> getArticleSettings(Long articleId) {
        List<ArticleSetting> links = articleSettingRepository.findByArticleId(articleId);
        List<Long> settingIds = links.stream().map(ArticleSetting::getSettingId).collect(Collectors.toList());
        if (settingIds.isEmpty()) return Collections.emptyList();
        return settingRepository.findAllById(settingIds);
    }

    // 添加关联设定
    @Transactional
    public void addArticleSettings(Long articleId, List<Long> settingIds) {
        if (settingIds.size() > 20) throw new RuntimeException("最多关联20个设定");
        settingIds = settingIds.stream().distinct().collect(Collectors.toList());
        for (Long settingId : settingIds) {
            if (!articleSettingRepository.existsByArticleIdAndSettingId(articleId, settingId)) {
                ArticleSetting link = new ArticleSetting();
                link.setArticleId(articleId);
                link.setSettingId(settingId);
                articleSettingRepository.save(link);
            }
        }
    }

    @Transactional
    public void updateArticleSettings(Long articleId, List<Long> settingIds) {
        List<ArticleSetting> oldLinks = articleSettingRepository.findByArticleId(articleId);
        articleSettingRepository.deleteAll(oldLinks);
        articleSettingRepository.flush(); // 确保删除立即生效
        addArticleSettings(articleId, settingIds);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getArticleDetail(Long id) {
        Article article = articleRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 手动初始化 children 及其 author（已在同一事务内，不会报错）
        if (article.getChildren() != null) {
            article.getChildren().size();
            for (Article child : article.getChildren()) {
                Hibernate.initialize(child.getAuthor());
            }
        }
        return ArticleResponse.fromArticle(article);
    }



}