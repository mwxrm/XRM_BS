package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Tag.java - 添加新字段
@Entity
@Table(name = "tags")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @EqualsAndHashCode.Include
    private String name;


    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classification classification = Classification.article;

    @Column(columnDefinition = "LONGTEXT")
    private String description;


    @ManyToMany(mappedBy = "tags")
    @ToString.Exclude   // 防止循环引用
    private Set<Article> articles = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    // 新增字段：标签使用次数统计
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer usageCount = 0;

    // 新增字段：是否是热门/推荐标签
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isRecommended = false;



    // 新增字段：标签分类
    @Column(length = 50)
    private String category;

    // 新增字段：标签图标/颜色
    @Column(length = 20)
    private String color;

    // 新增字段：最后使用时间
    private LocalDateTime lastUsedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType type = TagType.article;  // 默认故事标签

    @Enumerated(EnumType.STRING)
    private Setting.SettingCategory settingCategory;  // 仅当 type = setting 时使用

    public enum TagType {
        article, setting
    }

    //排除 articles 避免循环引用
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", usageCount=" + usageCount +
                '}';
    }

    public enum Classification {
        article,        // 故事标签（用于文章）
        character,      // 人物设定标签
        item,           // 物品设定标签
        location,       // 地区设定标签
        background,     // 背景设定标签
        rule,           // 规则设定标签
        custom          // 自定义设定标签
    }


}