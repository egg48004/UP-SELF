package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class Course {
    private Long id;
    private String title;
    private String description;
    private String languageCode;
    private Long mentorId;
    private String mentorName;
    private String status;
    private Long createdBy;
    private java.util.Date createTime;
    private java.util.Date updateTime;
}
