package com.xrm.xrm_bs.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateArticleRequest {
    private String title;
    private String content;
    private Set<String> tags;
    private Long parentArticleId;   // 允许修改引用的文章，设为null则解除引用
}