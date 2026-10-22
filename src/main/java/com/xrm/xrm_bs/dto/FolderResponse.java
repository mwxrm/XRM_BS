package com.xrm.xrm_bs.dto;

import com.xrm.xrm_bs.entity.FavoriteFolder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FolderResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer itemCount;  // 文件夹内文章数量

    public static FolderResponse fromFolder(FavoriteFolder folder, Integer itemCount) {
        FolderResponse response = new FolderResponse();
        response.setId(folder.getId());
        response.setName(folder.getName());
        response.setDescription(folder.getDescription());
        response.setCreatedAt(folder.getCreatedAt());
        response.setUpdatedAt(folder.getUpdatedAt());
        response.setItemCount(itemCount);
        return response;
    }
}