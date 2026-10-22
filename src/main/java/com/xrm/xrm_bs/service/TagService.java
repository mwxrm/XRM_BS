package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.dto.CreateTagRequest;
import com.xrm.xrm_bs.dto.TagResponse;
import com.xrm.xrm_bs.dto.UpdateTagRequest;
import com.xrm.xrm_bs.entity.Tag;
import com.xrm.xrm_bs.entity.TagVersion;
import com.xrm.xrm_bs.repository.TagRepository;
import com.xrm.xrm_bs.repository.TagVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagVersionRepository tagVersionRepository;

    // 获取热门标签
    @Transactional(readOnly = true)
    public List<TagResponse> getPopularTags() {
        List<Tag> tags = tagRepository.findTop10ByOrderByUsageCountDesc();
        return tags.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 获取所有标签（分页）
    @Transactional(readOnly = true)
    public Page<TagResponse> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable).map(this::convertToResponse);
    }

    // 创建标签
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {

        // 确定 classification
        Tag.TagType tagType;
        Tag.Classification classification;
        if ("article".equals(request.getCategory())) {
            tagType = Tag.TagType.article;
            classification = Tag.Classification.article;
        } else if ("setting".equals(request.getCategory())) {
            tagType = Tag.TagType.setting;
//            classification = Tag.Classification.valueOf(request.getSettingCategory());
            try {
                classification = Tag.Classification.valueOf(request.getSettingCategory());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的设定标签分类: " + request.getSettingCategory());
            }
        } else {
            throw new RuntimeException("无效的标签类型");
        }

        // 检查同名标签是否已存在（同一分类下）
        if (tagRepository.existsByNameAndClassification(request.getName(), classification)) {
            throw new RuntimeException("该分类下已存在同名标签");
        }

        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setType(tagType);
        tag.setDescription(request.getDescription());
        tag.setClassification(classification);
        tag.setColor(request.getColor());
        tag.setUsageCount(0);
        tag.setIsRecommended(false);
        tag.setCreatedAt(LocalDateTime.now());

        Tag savedTag = tagRepository.save(tag);
        return convertToResponse(savedTag);
    }

    // 更新标签
    @Transactional
    public TagResponse updateTag(Long tagId, UpdateTagRequest request, Long userId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 保存旧版本快照（无论谁编辑都记录）
        TagVersion version = new TagVersion();
        version.setTagId(tag.getId());
        version.setVersion(tagVersionRepository.countByTagId(tagId) + 1);
        version.setName(tag.getName());
        version.setDescription(tag.getDescription());
        version.setClassification(tag.getClassification().name());
        version.setColor(tag.getColor());
        version.setCreatedBy(userId);  // 记录修改者
        tagVersionRepository.save(version);

        // 更新标签内容
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        // 注意：标签名称和分类不允许修改，保持原样

        Tag updatedTag = tagRepository.save(tag);
        return convertToResponse(updatedTag);
    }

    // 增加标签使用次数
    @Transactional
    public void incrementTagUsage(Long tagId) {
        tagRepository.incrementUsageCount(tagId);
    }

    // 批量增加标签使用次数
    @Transactional
    public void incrementTagsUsage(List<Long> tagIds) {
        tagIds.forEach(this::incrementTagUsage);
    }

    // 实体转响应
    private TagResponse convertToResponse(Tag tag) {
        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setDescription(tag.getDescription());
        response.setClassification(tag.getClassification().name());
        response.setUsageCount(tag.getUsageCount());
        response.setIsRecommended(tag.getIsRecommended());
        response.setCreatedAt(tag.getCreatedAt());
        response.setLastUsedAt(tag.getLastUsedAt());
        response.setColor(tag.getColor());
        return response;
    }

    // 获取指定分类的推荐标签
    public List<TagResponse> getRecommendedTagsByClassification(Tag.Classification classification) {
        List<Tag> tags = tagRepository.findTop10ByClassificationOrderByUsageCountDesc(classification);
        return tags.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 搜索指定分类的标签
    public List<TagResponse> searchTagsByClassification(String keyword, Tag.Classification classification) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getRecommendedTagsByClassification(classification);
        }
        List<Tag> tags = tagRepository.searchByKeywordAndClassification(keyword.trim(), classification);
        return tags.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public Tag getTagById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
    }

    public List<Tag> getPopularTagsByClassification(Tag.Classification classification, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tagRepository.findByClassificationOrderByUsageCountDesc(classification, pageable);
    }

    public List<TagVersion> getTagVersions(Long tagId) {
        return tagVersionRepository.findByTagIdOrderByVersionDesc(tagId);
    }

    public Page<Tag> searchTags(String keyword, String classification, Pageable pageable) {
        Specification<Tag> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }
            if (StringUtils.hasText(classification)) {
                try {
                    Tag.Classification clazz = Tag.Classification.valueOf(classification);
                    predicates.add(cb.equal(root.get("classification"), clazz));
                } catch (IllegalArgumentException e) {
                    // 忽略无效分类
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return tagRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> searchAllTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPopularTags();  // 无关键词时返回热门标签
        }
        List<Tag> tags = tagRepository.searchByKeyword(keyword.trim());
        return tags.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
}