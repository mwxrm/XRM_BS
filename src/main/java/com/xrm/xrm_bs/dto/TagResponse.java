package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagResponse {
    private Long id;
    private String name;
    private String description;
    private String classification;
    private Integer usageCount;
    private Boolean isRecommended;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private String color;
}