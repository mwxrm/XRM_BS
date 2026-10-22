package com.xrm.xrm_bs.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FolderWithStatusResponse extends FolderResponse {
    private Boolean hasItem;
}