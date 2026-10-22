package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.Setting;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SimpleSettingResponse {
    private Long id;
    private String title;
    private String summary;
    private String authorUsername;
    private Integer viewCount;
    private Integer likeCount;
    private Set<String> tags;
    private LocalDateTime createdAt;

    public static SimpleSettingResponse fromSetting(Setting setting) {
        SimpleSettingResponse response = new SimpleSettingResponse();
        response.setId(setting.getId());
        response.setTitle(setting.getTitle());
        String plainText = setting.getContent().replaceAll("<[^>]*>", "");
        response.setSummary(plainText.length() > 100 ? plainText.substring(0, 100) + "..." : plainText);
        response.setAuthorUsername(setting.getAuthor().getUsername());
        response.setViewCount(setting.getViewCount());
        response.setLikeCount(setting.getLikeCount());
        response.setTags(setting.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet()));
        response.setCreatedAt(setting.getCreatedAt());
        return response;
    }
}