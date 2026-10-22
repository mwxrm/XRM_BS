package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.util.Set;

@Data
public class SettingSearchRequest {
    private String keyword;
    private String category;          // 设定分类（character, item, ...）
    private Set<String> tags;         // 标签名称集合（交集）
    private String sortBy;            // createdAt, viewCount, likeCount
    private String sortDirection;     // ASC, DESC
    private int page = 0;
    private int size = 10;
}