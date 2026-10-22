package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "article_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"article_id", "setting_id"})
})
@Data
public class ArticleSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "setting_id", nullable = false)
    private Long settingId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}