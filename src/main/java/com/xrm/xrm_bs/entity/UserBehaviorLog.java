package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_behavior_logs")
@Data
public class UserBehaviorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;            // 触发行为的用户

    @Column(name = "item_id", nullable = false)
    private Long itemId;            // 目标内容ID（文章ID、设定ID等）

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;        // article / setting

    @Column(name = "action", nullable = false, length = 30)
    private String action;          // view / like / favorite / comment / publish 等

    @CreationTimestamp
    @Column(name = "action_time", updatable = false)
    private LocalDateTime actionTime;  // 行为发生时间
}