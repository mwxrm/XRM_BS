package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userRepository.count());
        data.put("articleCount", articleRepository.count());
        data.put("settingCount", settingRepository.count());
        data.put("tagCount", tagRepository.count());
        data.put("commentCount", commentRepository.count());
        // 可扩展更多统计
        return ResponseEntity.ok(data);
    }
}