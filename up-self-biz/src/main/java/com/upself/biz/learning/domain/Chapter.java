package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class Chapter {
    private Long id;
    private Long courseId;
    private String title;
    private String body;
    private Integer sortOrder;
    private String status;
    private String languageCode;
    private java.util.Date updateTime;
}
