package com.xrm.xrm_bs.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String authorUsername;
    private String authorAvatar;
    private Long parentId;
    private Integer likeCount;
    private Boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies; // 二级评论
    private Long commentId;
}