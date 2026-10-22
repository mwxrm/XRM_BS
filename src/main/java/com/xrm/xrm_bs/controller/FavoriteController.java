package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.*;
import com.xrm.xrm_bs.service.FavoriteService;
import com.xrm.xrm_bs.util.SecurityUtil;
import jakarta.validation.Valid;
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
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // ---------- 文件夹管理 ----------
    @PostMapping("/folders")
    public ResponseEntity<FolderResponse> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        FolderResponse folder = favoriteService.createFolder(userId, request);
        return ResponseEntity.ok(folder);
    }

    @PutMapping("/folders/{folderId}")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody UpdateFolderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        FolderResponse folder = favoriteService.updateFolder(userId, folderId, request);
        return ResponseEntity.ok(folder);
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long folderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.deleteFolder(userId, folderId);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderResponse>> getUserFolders() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FolderResponse> folders = favoriteService.getUserFolders(userId);
        return ResponseEntity.ok(folders);
    }

    // 添加收藏（自动存入默认文件夹）
    @PostMapping("/{itemType}/{itemId}")
    public ResponseEntity<?> addFavorite(@PathVariable String itemType, @PathVariable Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.addFavorite(userId, itemType, itemId);
        return ResponseEntity.ok(Map.of("favorited", true));
    }

    // 取消收藏（从默认文件夹移除）
    @DeleteMapping("/{itemType}/{itemId}")
    public ResponseEntity<?> removeFavorite(@PathVariable String itemType, @PathVariable Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.removeFavorite(userId, itemType, itemId);
        return ResponseEntity.ok(Map.of("favorited", false));
    }


    @GetMapping("/{itemType}/{itemId}/status")
    public ResponseEntity<Map<String, Boolean>> getFavoriteStatus(@PathVariable String itemType, @PathVariable Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean favorited = favoriteService.isItemFavorited(userId, itemType, itemId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @PostMapping("/folders/{folderId}/{itemType}/{itemId}")
    public ResponseEntity<?> addItemToFolder(@PathVariable Long folderId, @PathVariable String itemType, @PathVariable Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.addItemToFolder(userId, folderId, itemType, itemId);
        return ResponseEntity.ok(Map.of("message", "收藏成功"));
    }


    @DeleteMapping("/folders/{folderId}/{itemType}/{itemId}")
    public ResponseEntity<?> removeItemFromFolder(@PathVariable Long folderId, @PathVariable String itemType, @PathVariable Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.removeItemFromFolder(userId, folderId, itemType, itemId);
        return ResponseEntity.ok(Map.of("message", "取消收藏成功"));
    }

    @GetMapping("/folders/{folderId}/items")
    public ResponseEntity<Page<FavoriteItemResponse>> getFolderItems(
            @PathVariable Long folderId,
            @RequestParam(required = false) String itemType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FavoriteItemResponse> items = favoriteService.getFolderItems(userId, folderId, itemType, pageable);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/folders/with-status")
    public ResponseEntity<List<FolderWithStatusResponse>> getFoldersWithItemStatus(
            @RequestParam String itemType, @RequestParam Long itemId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FolderWithStatusResponse> folders = favoriteService.getUserFoldersWithItemStatus(userId, itemType, itemId);
        return ResponseEntity.ok(folders);
    }




}
