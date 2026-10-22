package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.RecentViewResponse;
import com.xrm.xrm_bs.service.RecentViewService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recent-views")
@RequiredArgsConstructor
public class RecentViewController {

    private final RecentViewService recentViewService;

    @GetMapping
    public ResponseEntity<Page<RecentViewResponse>> getUserRecentViews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewedAt").descending());
        Page<RecentViewResponse> views = recentViewService.getUserRecentViews(userId, pageable);
        return ResponseEntity.ok(views);
    }

    @DeleteMapping
    public ResponseEntity<?> clearHistory() {
        Long userId = SecurityUtil.getCurrentUserId();
        recentViewService.deleteAllByUserId(userId);
        return ResponseEntity.ok("已清空");
    }

}