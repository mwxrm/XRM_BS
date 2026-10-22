package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;


@Entity
@Table(name = "recent_views")
@Data
public class RecentView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @CreationTimestamp
    private LocalDateTime viewedAt;


}