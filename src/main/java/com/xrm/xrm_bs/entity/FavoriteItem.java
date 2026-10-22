package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_items")
@Data
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private FavoriteFolder folder;

    @Column(name = "item_type", nullable = false)
    private String itemType;  // "article" 或 "setting"

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}