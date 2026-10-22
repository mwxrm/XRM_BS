package com.xrm.xrm_bs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFolderRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;
}