package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private Long sourceUserId;
    private String sourceUsername;
    private Long targetId;
    private String targetType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long commentId;
}