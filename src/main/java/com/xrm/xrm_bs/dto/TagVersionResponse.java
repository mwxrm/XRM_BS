package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagVersionResponse {
    private Long id;
    private Integer version;
    private String description;
    private String color;
    private Long createdBy;
    private String authorUsername;
    private LocalDateTime createdAt;
}