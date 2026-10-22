package com.xrm.xrm_bs.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;    // 改为邮箱
    private String password;
}