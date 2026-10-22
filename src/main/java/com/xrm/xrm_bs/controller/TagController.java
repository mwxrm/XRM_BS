// TagController.java
package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.*;
import com.xrm.xrm_bs.entity.Tag;
import com.xrm.xrm_bs.entity.TagVersion;
import com.xrm.xrm_bs.repository.UserRepository;
import com.xrm.xrm_bs.service.TagService;
import com.xrm.xrm_bs.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<TagResponse>> searchTags(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String classification) {  // 不再有 defaultValue
        if (classification != null && !classification.isEmpty()) {
            try {
                Tag.Classification clazz = Tag.Classification.valueOf(classification.toLowerCase());
                return ResponseEntity.ok(tagService.searchTagsByClassification(keyword, clazz));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的分类: " + classification);
            }
        } else {
            // 不传分类则搜索所有分类的标签
            return ResponseEntity.ok(tagService.searchAllTags(keyword));
        }
    }


    // 获取热门标签
    @GetMapping("/popular")
    public ResponseEntity<List<TagResponse>> getPopularTags(
            @RequestParam(required = false, defaultValue = "article") String classification,
            @RequestParam(required = false, defaultValue = "10") int size) {
        Tag.Classification clazz = Tag.Classification.valueOf(classification.toLowerCase());
        List<Tag> tags = tagService.getPopularTagsByClassification(clazz, size);
        return ResponseEntity.ok(tags.stream().map(this::convertToResponse).collect(Collectors.toList()));
    }



    // 获取所有标签（分页）
    @GetMapping
    public ResponseEntity<Page<TagResponse>> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "usageCount") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return ResponseEntity.ok(tagService.getAllTags(pageable));
    }

    // 创建标签（管理员权限）
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> createTag(@RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(tagService.createTag(request));
    }

    // 更新标签（管理员权限）
    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable Long id,
            @RequestBody UpdateTagRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TagResponse response = tagService.updateTag(id, request, userId);
        return ResponseEntity.ok(response);
    }

    private TagResponse convertToResponse(Tag tag) {
        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setDescription(tag.getDescription());
        response.setClassification(tag.getClassification().name()); // 枚举转字符串
        response.setUsageCount(tag.getUsageCount());
        response.setIsRecommended(tag.getIsRecommended());
        response.setCreatedAt(tag.getCreatedAt());
        response.setLastUsedAt(tag.getLastUsedAt());
        response.setColor(tag.getColor());
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTag(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        return ResponseEntity.ok(convertToResponse(tag));
    }


    @GetMapping("/{id}/versions")
    public ResponseEntity<List<TagVersionResponse>> getTagVersions(@PathVariable Long id) {
        List<TagVersion> versions = tagService.getTagVersions(id);
        List<TagVersionResponse> responses = versions.stream()
                .map(this::convertToVersionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private TagVersionResponse convertToVersionResponse(TagVersion version) {
        TagVersionResponse resp = new TagVersionResponse();
        resp.setId(version.getId());
        resp.setVersion(version.getVersion());
        resp.setDescription(version.getDescription());
        resp.setColor(version.getColor());
        resp.setCreatedBy(version.getCreatedBy());
        resp.setCreatedAt(version.getCreatedAt());
        // 获取修改者用户名
        userRepository.findById(version.getCreatedBy()).ifPresent(user ->
                resp.setAuthorUsername(user.getUsername()));
        return resp;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<TagResponse>> searchTags(@RequestBody TagSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Tag> tags = tagService.searchTags(request.getKeyword(), request.getClassification(), pageable);
        return ResponseEntity.ok(tags.map(this::convertToResponse));
    }


    @PostMapping("/user-create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TagResponse> userCreateTag(@RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(tagService.createTag(request));
    }

}