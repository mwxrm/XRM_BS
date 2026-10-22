package com.xrm.xrm_bs.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xrm.xrm_bs.entity.Article;
import com.xrm.xrm_bs.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;

    @JsonSerialize(using = ToStringSerializer.class)  // 序列化为字符串
    private Article.ArticleStatus status;

    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer likeCount;

    private Boolean deleted;


    // 父文章信息
    private SimpleArticleInfo parentArticle;
    // 子文章列表（分页）
    private List<SimpleArticleInfo> children;

    private List<SettingResponse.SimpleSettingInfo> settings; // 用于返回关联设定列表


    public static ArticleResponse fromArticle(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setContent(article.getContent());
        response.setAuthorId(article.getAuthor().getId());
        response.setAuthorUsername(article.getAuthor().getUsername());
        response.setStatus(article.getStatus());
        response.setTags(article.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet()));
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        response.setViewCount(article.getViewCount());
        response.setLikeCount(article.getLikeCount());
        response.setDeleted(article.getDeleted());

        // 设置父文章
        if (article.getParentArticle() != null) {
            response.setParentArticle(SimpleArticleInfo.from(article.getParentArticle()));
        }

        // 设置子文章列表（简单获取全部，也可以考虑分页）
        if (article.getChildren() != null && !article.getChildren().isEmpty()) {
            List<SimpleArticleInfo> filteredChildren = article.getChildren().stream()
                    .filter(child -> child.getStatus() == Article.ArticleStatus.published) // 仅已发布
                    .map(SimpleArticleInfo::from)
                    .collect(Collectors.toList());
            response.setChildren(filteredChildren);
        }

        List<SettingResponse.SimpleSettingInfo> settingInfos = article.getSettings().stream()
                .filter(s -> !s.getDeleted())
                .map(SettingResponse.SimpleSettingInfo::from)
                .collect(Collectors.toList());
        response.setSettings(settingInfos);

        return response;
    }

    // 内部类：简单的文章信息
    @Data
    public static class SimpleArticleInfo {
        private Long id;
        private String title;
        private String authorUsername;
        private LocalDateTime createdAt;
        private Boolean deleted;

        public static SimpleArticleInfo from(Article article) {
            SimpleArticleInfo info = new SimpleArticleInfo();
            info.setId(article.getId());
            info.setTitle(article.getTitle());
            info.setAuthorUsername(article.getAuthor().getUsername());
            info.setCreatedAt(article.getCreatedAt());
            info.setDeleted(article.getDeleted());
            return info;
        }
    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public String getContent() {
//        return content;
//    }
//
//    public void setContent(String content) {
//        this.content = content;
//    }
//
//    public Long getAuthorId() {
//        return authorId;
//    }
//
//    public void setAuthorId(Long authorId) {
//        this.authorId = authorId;
//    }
//
//    public String getAuthorUsername() {
//        return authorUsername;
//    }
//
//    public void setAuthorUsername(String authorUsername) {
//        this.authorUsername = authorUsername;
//    }
//
//    public Article.ArticleStatus getStatus() {
//        return status;
//    }
//
//    public void setStatus(Article.ArticleStatus status) {
//        this.status = status;
//    }
//
//    public Set<String> getTags() {
//        return tags;
//    }
//
//    public void setTags(Set<String> tags) {
//        this.tags = tags;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public LocalDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(LocalDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//
//    public Integer getViewCount() {
//        return viewCount;
//    }
//
//    public void setViewCount(Integer viewCount) {
//        this.viewCount = viewCount;
//    }
//
//    public Integer getLikeCount() {
//        return likeCount;
//    }
//
//    public void setLikeCount(Integer likeCount) {
//        this.likeCount = likeCount;
//    }



}