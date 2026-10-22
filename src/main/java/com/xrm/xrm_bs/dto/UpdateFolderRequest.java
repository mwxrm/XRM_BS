package com.xrm.xrm_bs.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFolderRequest {
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;
}