package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class LearningResource {
    private Long id;
    private Long chapterId;
    private String kind;
    private String title;
    @com.fasterxml.jackson.annotation.JsonIgnore private String storageKey;
    private String originalName;
    private String mimeType;
    private Long fileSize;
    private String altText;
    private Long captionResourceId;
    private String transcript;
    private String languageCode;
    private String status;
    private Long createdBy;
    private java.util.Date updateTime;
}
