package com.xrm.xrm_bs.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "articles")
@Data
public class
Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private Boolean deleted = false;


    @Enumerated(EnumType.STRING)
//    @Convert(converter = ArticleStatusConverter.class)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.draft;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @ManyToMany
    @JoinTable(
            name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();


    // 父文章（当前文章引用的文章）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_article_id")
    private Article parentArticle;

    // 子文章列表（引用当前文章的所有文章）
//    @OneToMany(mappedBy = "parentArticle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "parentArticle")
    private List<Article> children = new ArrayList<>();



    // Article.java - 使用小写枚举
    public enum ArticleStatus {
        draft,      // 小写，与数据库完全匹配
        published;  // 小写

        // 如果需要显示为大写
        public String getDisplayName() {
            return this.name().toUpperCase();
        }

        // JSON 序列化：返回小写
        @JsonValue
        public String getValue() {
            return this.name();
        }

        // JSON 反序列化：不区分大小写
        @JsonCreator
        public static ArticleStatus fromValue(String value) {
            if (value == null) {
                return null;
            }
            try {
                // 转换为小写匹配
                return ArticleStatus.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown enum value: " + value);
            }
        }

        // Hibernate 使用的方法
        public static ArticleStatus fromString(String value) {
            return fromValue(value);
        }
    }

    @ManyToMany
    @JoinTable(
            name = "article_settings",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "setting_id")
    )
    private Set<Setting> settings = new HashSet<>();

}