package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.UserPublicResponse;
import com.xrm.xrm_bs.service.FollowService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // 关注用户（幂等，已关注则直接返回成功）
    @PostMapping("/{userId}")
    public ResponseEntity<?> followUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        followService.followUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("following", true));
    }

    // 取消关注（幂等，未关注则直接返回成功）
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        followService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("following", false));
    }

    // 检查单个用户关注状态
    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getFollowStatus(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        boolean following = followService.isFollowing(currentUserId, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    // 批量查询关注状态
    @PostMapping("/batch-status")
    public ResponseEntity<Map<Long, Boolean>> batchCheckFollowStatus(@RequestBody List<Long> userIds) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Map<Long, Boolean> result = followService.batchCheckFollowing(currentUserId, userIds);
        return ResponseEntity.ok(result);
    }

    // 获取关注数/粉丝数
    @GetMapping("/{userId}/counts")
    public ResponseEntity<Map<String, Long>> getFollowCounts(@PathVariable Long userId) {
        long following = followService.getFollowingCount(userId);
        long followers = followService.getFollowersCount(userId);
        return ResponseEntity.ok(Map.of("following", following, "followers", followers));
    }

    // 关注列表（分页）
    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserPublicResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(followService.getFollowingList(userId, pageable));
    }

    // 粉丝列表（分页）
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserPublicResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(followService.getFollowersList(userId, pageable));
    }
}