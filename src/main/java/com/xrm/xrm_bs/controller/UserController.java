package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.*;
import com.xrm.xrm_bs.entity.Tag;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.service.UserService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;

    @PostMapping("/search")
    public ResponseEntity<Page<UserSearchResponse>> searchUsers(@RequestBody UserSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<User> users = userService.searchUsers(request.getKeyword(), pageable);
        Page<UserSearchResponse> response = users.map(user ->
                new UserSearchResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserPublicResponse> getUserPublicInfo(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserPublicResponse(
                user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt()
        ));
    }

    @GetMapping("/{userId}/likes/count")
    public ResponseEntity<Map<String, Long>> getUserTotalLikes(@PathVariable Long userId) {
        long count = userService.getTotalLikes(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/me/preferences/tags")
    public ResponseEntity<List<TagResponse>> getMyPreferredTags() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Tag> tags = userService.getUserPreferredTags(userId);
        return ResponseEntity.ok(tags.stream().map(this::convertToTagResponse).collect(Collectors.toList()));
    }


    @PutMapping("/me/preferences/tags")
    public ResponseEntity<?> updateMyPreferredTags(@RequestBody List<Long> tagIds) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updatePreferredTags(userId, tagIds);
        return ResponseEntity.ok("更新成功");
    }

    private TagResponse convertToTagResponse(Tag tag) {
        // 复用已有的转换逻辑，或从 TagService 中抽取
        TagResponse resp = new TagResponse();
        resp.setId(tag.getId());
        resp.setName(tag.getName());
        resp.setClassification(tag.getClassification().name());
        resp.setUsageCount(tag.getUsageCount());
        resp.setColor(tag.getColor());
        return resp;
    }


    @GetMapping("/me/preferences/blocked-tags")
    public ResponseEntity<List<TagResponse>> getBlockedTags() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Tag> tags = userService.getBlockedTags(userId);
        return ResponseEntity.ok(tags.stream().map(this::convertToTagResponse).collect(Collectors.toList()));
    }

    @PutMapping("/me/preferences/blocked-tags")
    public ResponseEntity<?> updateBlockedTags(@RequestBody List<Long> tagIds) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updateBlockedTags(userId, tagIds);
        return ResponseEntity.ok("屏蔽标签更新成功");
    }



}
