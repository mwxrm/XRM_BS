// Setting.java
package com.xrm.xrm_bs.entity;

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
@Table(name = "settings")
@Data
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingCategory category;

    @Column(columnDefinition = "JSON")
    private String templateData;  // JSON 字符串，存储分类专属字段

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingStatus status = SettingStatus.draft;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Integer viewCount = 0;
    private Integer likeCount = 0;

    @ManyToMany
    @JoinTable(
            name = "setting_tags",
            joinColumns = @JoinColumn(name = "setting_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_setting_id")
    private Setting parentSetting;

    @OneToMany(mappedBy = "parentSetting")
    private List<Setting> children = new ArrayList<>();

    @Column(nullable = false)
    private Boolean deleted = false;

    public enum SettingCategory {
        character, item, location, background, rule, custom
    }

    public enum SettingStatus {
        draft, published
    }



}