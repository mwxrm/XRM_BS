package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.service.LikeService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // 点赞
    @PostMapping("/{articleId}")
    public ResponseEntity<?> likeArticle(@PathVariable Long articleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        likeService.likeArticle(userId, articleId);
        return ResponseEntity.ok(Map.of("liked", true));
    }

    // 取消点赞
    @DeleteMapping("/{articleId}")
    public ResponseEntity<?> unlikeArticle(@PathVariable Long articleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        likeService.unlikeArticle(userId, articleId);
        return ResponseEntity.ok(Map.of("liked", false));
    }

    // 检查当前用户是否已点赞
    @GetMapping("/{articleId}/status")
    public ResponseEntity<?> getLikeStatus(@PathVariable Long articleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean liked = likeService.isUserLiked(userId, articleId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}