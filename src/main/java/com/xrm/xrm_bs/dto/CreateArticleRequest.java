package com.xrm.xrm_bs.dto;

import lombok.Data;

import java.util.Set;

@Data
public class CreateArticleRequest {
    private String title;
    private String content;
    private Set<String> tags; // 标签名称集合
    private Long parentArticleId;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }


    public Long getParentArticleId() {
        return parentArticleId;
    }


}