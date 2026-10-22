package com.xrm.xrm_bs.dto;

import lombok.Data;

@Data
public class UserSearchRequest {
    private String keyword;
    private int page = 0;
    private int size = 10;
}