package com.xrm.xrm_bs.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTagRequest {
    @Size(max = 100, message = "描述不能超过100个字符")
    private String description;

    private String category;
    private String color;
    private Boolean isRecommended;
}
