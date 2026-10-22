package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.service.HotRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hot")
@RequiredArgsConstructor
public class HotController {

    private final HotRankService hotRankService;

    // 手动触发热榜计算
    @PostMapping("/compute")
    @PreAuthorize("hasRole('ADMIN')")      // 仅管理员可调用
    public ResponseEntity<String> compute() {
        hotRankService.computeHotRanks();
        return ResponseEntity.ok("热榜已更新");
    }

    @GetMapping("/articles")
    public ResponseEntity<List<Map<String, Object>>> hotArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(hotRankService.getArticleHotRankDetails(page * size, size));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<Map<String, Object>>> hotSettings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(hotRankService.getSettingHotRankDetails(page * size, size));
    }



}


