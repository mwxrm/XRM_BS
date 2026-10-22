package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteItemResponse {
    private Long id;
    private Long itemId;
    private String title;
    private String summary;
    private String authorUsername;
    private LocalDateTime favoritedAt;
    private String itemType;
    private Boolean deleted;
}