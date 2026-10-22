package com.xrm.xrm_bs.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String emailCode;   // 邮箱验证码
    private String mathToken;   // 算术验证码 token
    private int mathAnswer;     // 算术验证码答案
}