package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentViewResponse {
    private Long id;
    private Long itemId;
    private String title;
    private String summary;
    private String authorUsername;
    private LocalDateTime viewedAt;
    private String itemType;
    private Boolean deleted;
}