package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.Setting;
import com.xrm.xrm_bs.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SettingResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String templateData;
    private Long authorId;
    private String authorUsername;
    private String status;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer likeCount;


    private Boolean deleted;

    private SimpleSettingInfo parentSetting;
    private List<SimpleSettingInfo> children;

    public static SettingResponse fromSetting(Setting setting) {
        SettingResponse response = new SettingResponse();
        response.setId(setting.getId());
        response.setTitle(setting.getTitle());
        response.setContent(setting.getContent());
        response.setCategory(setting.getCategory().name());
        response.setTemplateData(setting.getTemplateData());
        response.setAuthorId(setting.getAuthor().getId());
        response.setAuthorUsername(setting.getAuthor().getUsername());
        response.setStatus(setting.getStatus().name());
        response.setTags(setting.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
        response.setCreatedAt(setting.getCreatedAt());
        response.setUpdatedAt(setting.getUpdatedAt());
        response.setViewCount(setting.getViewCount());
        response.setLikeCount(setting.getLikeCount());
        response.setDeleted(setting.getDeleted());

        // 父设定信息（仅当已发布时显示）
        if (setting.getParentSetting() != null && setting.getParentSetting().getStatus() == Setting.SettingStatus.published) {
            response.setParentSetting(SimpleSettingInfo.from(setting.getParentSetting()));
        }

        // 子设定列表（仅已发布的）
        if (setting.getChildren() != null && !setting.getChildren().isEmpty()) {
            List<SimpleSettingInfo> children = setting.getChildren().stream()
                    .filter(child -> child.getStatus() == Setting.SettingStatus.published)
                    .map(SimpleSettingInfo::from)
                    .collect(Collectors.toList());
            response.setChildren(children);
        }

        return response;
    }


    @Data
    public static class SimpleSettingInfo {
        private Long id;
        private String title;
        private String authorUsername;
        private LocalDateTime createdAt;
        private Boolean deleted;

        public static SimpleSettingInfo from(Setting setting) {
            SimpleSettingInfo info = new SimpleSettingInfo();
            info.setId(setting.getId());
            info.setTitle(setting.getTitle());
            info.setAuthorUsername(setting.getAuthor().getUsername());
            info.setCreatedAt(setting.getCreatedAt());
            info.setDeleted(setting.getDeleted());
            return info;
        }
    }




}