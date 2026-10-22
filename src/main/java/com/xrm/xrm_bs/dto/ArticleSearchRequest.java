package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.util.Set;

@Data
public class ArticleSearchRequest {
    private String keyword;           // 标题或内容模糊搜索
    private Set<String> tags;         // 标签名称集合（交集）
    private String sortBy;            // viewCount, likeCount, createdAt, referenceCount
    private String sortDirection;     // ASC, DESC
    private int page = 0;
    private int size = 10;
}