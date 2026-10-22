package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.service.RecommendationService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> recommend(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean refresh) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Map<String, Object>> items = recommendationService.getRecommendations(userId, type, page * size, size, refresh);
        return ResponseEntity.ok(items);
    }
}