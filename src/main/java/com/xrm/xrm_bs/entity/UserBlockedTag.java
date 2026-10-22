package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_blocked_tags")
@Data
public class UserBlockedTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}