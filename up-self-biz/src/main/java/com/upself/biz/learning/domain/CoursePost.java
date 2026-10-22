package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class CoursePost {
    private Long id;
    private Long courseId;
    private String title;
    private String body;
    private String type;
    private String meetingUrl;
    private String meetingStartsAt;
    private String languageCode;
    private String status;
    private Long createdBy;
    private java.util.Date createTime;
    private java.util.Date updateTime;
}
