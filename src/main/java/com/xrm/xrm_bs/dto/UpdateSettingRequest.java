package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.Setting;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateSettingRequest {
    private String title;
    private String content;
    private Set<String> tags;
    private Long parentSettingId;
}