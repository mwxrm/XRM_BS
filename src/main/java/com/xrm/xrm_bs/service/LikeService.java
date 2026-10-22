package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.Comment;
import com.xrm.xrm_bs.entity.UserLike;
import com.xrm.xrm_bs.repository.ArticleRepository;
import com.xrm.xrm_bs.repository.UserLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final UserLikeRepository likeRepository;
    private final ArticleRepository articleRepository;
    private final NotificationService notificationService;
    private final UserBehaviorLogService behaviorLogService;

    @Transactional
    public void likeArticle(Long userId, Long articleId) {
        // 检查是否已经点赞
        if (likeRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new RuntimeException("已经点赞过");
        }

        // 创建点赞记录
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        likeRepository.save(like);

        // 增加文章的点赞计数
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setLikeCount(article.getLikeCount() + 1);
        articleRepository.save(article);

        // 点赞后，给文章作者发送通知
        notificationService.createNotification(
                article.getAuthor().getId(),
                "like_article",
                userId,
                articleId,
                "article",
                "赞了你的文章《" + article.getTitle() + "》"
        );

        behaviorLogService.log(userId, articleId, "article", "like");
    }

    @Transactional
    public void unlikeArticle(Long userId, Long articleId) {
        // 删除点赞记录
        likeRepository.deleteByUserIdAndArticleId(userId, articleId);

        // 减少文章的点赞计数
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setLikeCount(article.getLikeCount() - 1);
        articleRepository.save(article);
    }

    @Transactional(readOnly = true)
    public boolean isUserLiked(Long userId, Long articleId) {
        return likeRepository.existsByUserIdAndArticleId(userId, articleId);
    }
}