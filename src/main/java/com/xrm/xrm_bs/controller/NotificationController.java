package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.NotificationGroupResponse;
import com.xrm.xrm_bs.dto.NotificationResponse;
import com.xrm.xrm_bs.service.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read")
    public ResponseEntity<?> markAsRead(@RequestBody List<Long> ids) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAsRead(userId, ids);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllNotifications() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok().build();
    }


    // 获取分组内详情
    @GetMapping("/groups/{groupKey}/items")
    public ResponseEntity<List<NotificationResponse>> getGroupItems(@PathVariable String groupKey) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationResponse> items = notificationService.getGroupItems(userId, groupKey);
        return ResponseEntity.ok(items);
    }



    @GetMapping
    public ResponseEntity<Page<NotificationGroupResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getGroupedNotifications(userId, filter, pageable));
    }



}