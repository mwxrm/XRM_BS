package com.xrm.xrm_bs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTagRequest {
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 20, message = "标签名称长度必须在1-20个字符之间")
    private String name;

    @Size(max = 100, message = "描述不能超过100个字符")
    private String description;

    private String category;
    private String color;
    private String settingCategory;   // 设定标签的具体种类（仅当 category = "setting" 时有效）

}