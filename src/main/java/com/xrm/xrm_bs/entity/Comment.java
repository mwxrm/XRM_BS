package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false)
    private String targetType; // article, setting, tag

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    private Integer status = 1; // 1正常 0删除

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 非持久化字段，用于前端展示
    @Transient
    private String authorUsername;
    @Transient
    private String authorAvatar;
    @Transient
    private Boolean likedByCurrentUser;
    @Transient
    private java.util.List<Comment> replies;
}