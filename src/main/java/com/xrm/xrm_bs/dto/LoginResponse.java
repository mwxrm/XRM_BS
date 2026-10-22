package com.xrm.xrm_bs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.xrm.xrm_bs.entity.User;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String message;
//    private User user;
    private UserResponse user;  // 改为 UserResponse
}