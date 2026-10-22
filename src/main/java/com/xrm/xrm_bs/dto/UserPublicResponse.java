package com.xrm.xrm_bs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserPublicResponse {
    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;
}