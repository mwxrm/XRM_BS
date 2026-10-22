package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.CreateSettingRequest;
import com.xrm.xrm_bs.dto.SettingResponse;
import com.xrm.xrm_bs.dto.SettingSearchRequest;
import com.xrm.xrm_bs.dto.UpdateSettingRequest;
import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.service.RecentViewService;
import com.xrm.xrm_bs.service.SettingService;
import com.xrm.xrm_bs.util.SecurityUtil;
import com.xrm.xrm_bs.dto.SimpleSettingResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// SettingController.java
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;
    private final RecentViewService recentViewService;
    private static final Logger logger = LoggerFactory.getLogger(SettingController.class);


    @PostMapping
    public ResponseEntity<?> createSetting(@RequestBody CreateSettingRequest request,
                                           @RequestParam(required = false) String category) {
        Setting.SettingCategory settingCategory;
        if (category == null) {
            settingCategory = Setting.SettingCategory.character; // 默认
        } else {
            try {
                settingCategory = Setting.SettingCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的分类: " + category);
            }
        }
        Long userId = SecurityUtil.getCurrentUserId();
        Setting setting = settingService.createSetting(request, userId, settingCategory);
        return ResponseEntity.ok(SettingResponse.fromSetting(setting));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMySettings() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Setting> settings = settingService.getMySettings(userId);
        return ResponseEntity.ok(settings.stream().map(SettingResponse::fromSetting).collect(Collectors.toList()));
    }

    @GetMapping("/drafts")
    public ResponseEntity<?> getMyDrafts() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Setting> drafts = settingService.getMyDrafts(userId);
        return ResponseEntity.ok(drafts.stream().map(SettingResponse::fromSetting).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSetting(@PathVariable Long id,
                                           @RequestBody UpdateSettingRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Setting setting = settingService.updateSetting(id, request, userId);
        return ResponseEntity.ok(SettingResponse.fromSetting(setting));
    }



    @GetMapping("/published")
    public ResponseEntity<Page<SimpleSettingResponse>> getPublishedSettings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Setting> settings = settingService.getPublishedSettings(pageable);
        return ResponseEntity.ok(settings.map(SimpleSettingResponse::fromSetting));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishSetting(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Setting setting = settingService.publishSetting(id, userId);
        return ResponseEntity.ok(SettingResponse.fromSetting(setting));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSetting(@PathVariable Long id) {
        // 使用新方法获取实体（即使软删除也返回，不存在才返回 null）
        SettingResponse settingResp = settingService.getSettingDetail(id);

// 检查 settingResp 中的 deleted 状态、权限等
        if (settingResp.getDeleted()) {
            return ResponseEntity.ok(settingResp);
        }

// 草稿权限检查（需要 authorId）
        Setting setting = settingService.getSettingEntityById(id); // 这里仍可调用原来的获取，但不用于响应
        if (setting.getStatus() == Setting.SettingStatus.draft) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!setting.getAuthor().getId().equals(currentUserId)) {
                return ResponseEntity.status(403).body("无权查看此文章");
            }
        }

        if (setting == null) {
            return ResponseEntity.notFound().build();
        }

        // 如果设定已被软删除，直接返回响应（不增加浏览量、不记录最近浏览、不检查草稿权限）
        if (setting.getDeleted()) {
            return ResponseEntity.ok(SettingResponse.fromSetting(setting));
        }

        // 记录最近浏览（仅正常设定）
        try {
            Long currentUserId = SecurityUtil.getCurrentUserId();
//            recentViewService.addRecentView(currentUserId, id, "setting");
            recentViewService.addRecentViewSafe(currentUserId, id, "setting");
        } catch (Exception e) {
            logger.warn("记录最近浏览失败: {}", e.getMessage());
        }

        // 草稿权限检查
        if (setting.getStatus() == Setting.SettingStatus.draft) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!setting.getAuthor().getId().equals(currentUserId)) {
                return ResponseEntity.status(403).body("无权查看草稿");
            }
        }

        return ResponseEntity.ok(settingResp);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeSetting(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        settingService.likeSetting(userId, id);
        return ResponseEntity.ok(Map.of("liked", true));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeSetting(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        settingService.unlikeSetting(userId, id);
        return ResponseEntity.ok(Map.of("liked", false));
    }

    @GetMapping("/{id}/like/status")
    public ResponseEntity<?> getLikeStatus(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean liked = settingService.isUserLiked(userId, id);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSetting(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        settingService.deleteSetting(id, userId);
        return ResponseEntity.ok("删除成功");
    }



    @GetMapping("/{id}/children")
    public ResponseEntity<?> getChildren(@PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Setting> childrenPage = settingService.getChildrenByParentId(id, pageable);
        Page<SettingResponse.SimpleSettingInfo> response = childrenPage.map(SettingResponse.SimpleSettingInfo::from);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SettingResponse>> searchSettings(@RequestBody SettingSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy()));
        Page<Setting> settings = settingService.searchSettings(request.getKeyword(), request.getCategory(), request.getTags(), pageable);
        return ResponseEntity.ok(settings.map(SettingResponse::fromSetting));
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<SimpleSettingResponse>> getUserSettings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Setting> settings = settingService.getPublishedSettingsByUserId(userId, pageable);
        return ResponseEntity.ok(settings.map(SimpleSettingResponse::fromSetting));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> reportSettingView(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, Object> payload,
                                               HttpServletRequest request) {
        try {
            // 获取客户端信息
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            Long userId = null;
            try {
                userId = SecurityUtil.getCurrentUserId();
            } catch (Exception e) {
                // 未登录用户，userId 为 null
            }
            boolean counted = settingService.registerView(id, userId, ip, userAgent);
            if (counted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(409).body("今日已计过浏览");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}