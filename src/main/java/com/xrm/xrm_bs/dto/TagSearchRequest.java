package com.xrm.xrm_bs.dto;

import lombok.Data;

@Data
public class TagSearchRequest {
    private String keyword;
    private String classification;   // article, character, item, location, background, rule, custom
    private int page = 0;
    private int size = 20;
}