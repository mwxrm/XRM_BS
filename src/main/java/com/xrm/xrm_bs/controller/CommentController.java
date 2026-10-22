package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.CommentRequest;
import com.xrm.xrm_bs.dto.CommentResponse;
import com.xrm.xrm_bs.entity.Comment;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.repository.CommentRepository;
import com.xrm.xrm_bs.service.CommentService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentRepository commentRepository;

    @PostMapping("/{targetType}/{targetId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody CommentRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        CommentResponse comment = commentService.createComment(targetType, targetId, request, userId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CommentResponse> comments = commentService.getComments(targetType, targetId, pageable, userId);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = SecurityUtil.getCurrentUserDetails().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
        commentService.deleteComment(commentId, userId, isAdmin);
        return ResponseEntity.ok("删除成功");
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long commentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(Map.of("liked", true));
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<?> unlikeComment(@PathVariable Long commentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        commentService.unlikeComment(commentId, userId);
        return ResponseEntity.ok(Map.of("liked", false));
    }

    @GetMapping("/{id}/parent")
    public ResponseEntity<Map<String, Object>> getCommentParent(@PathVariable Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("targetType", comment.getTargetType());
        result.put("targetId", comment.getTargetId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/page")
    public ResponseEntity<Map<String, Integer>> getCommentPage(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "10") int size) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        int index = commentRepository.countByTargetTypeAndTargetIdAndStatusAndCreatedAtAfter(
                comment.getTargetType(), comment.getTargetId(), 1, comment.getCreatedAt());

        int page = index / size;  // 0-based
        return ResponseEntity.ok(Map.of("page", page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long id) {
        CommentResponse response = commentService.getCommentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/context")
    public ResponseEntity<Map<String, Object>> getCommentContext(@PathVariable Long id) {
        Map<String, Object> context = commentService.getCommentContext(id);
        return ResponseEntity.ok(context);
    }
}