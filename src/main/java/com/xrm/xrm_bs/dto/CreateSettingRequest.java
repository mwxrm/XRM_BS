package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.Setting;
import lombok.Data;

import java.util.Set;

@Data
public class CreateSettingRequest {
    private String title;
    private String content;
    private Setting.SettingCategory category;
    private String templateData;  // JSON 字符串
    private Set<String> tags;
    private Long parentSettingId;
}