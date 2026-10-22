package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.Article;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SimpleArticleResponse {
    private Long id;
    private String title;
    private String summary; // 内容摘要
    private String authorUsername;
    private Article.ArticleStatus status;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Integer likeCount;

    public static SimpleArticleResponse fromArticle(Article article) {
        SimpleArticleResponse response = new SimpleArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());

        // 生成摘要：取前100个字符
        String content = article.getContent();
        String plainText = content.replaceAll("<[^>]*>", ""); // 正则去除标签
        String summary = plainText.length() > 100
                ? plainText.substring(0, 100) + "..."
                : plainText;
        response.setSummary(summary);

        response.setAuthorUsername(article.getAuthor().getUsername());
        response.setStatus(article.getStatus());
        response.setTags(article.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet()));
        response.setCreatedAt(article.getCreatedAt());
        response.setViewCount(article.getViewCount());
        response.setLikeCount(article.getLikeCount());
        return response;
    }
}