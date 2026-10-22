package com.xrm.xrm_bs.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private Long parentId; // 可为空
}