package com.upself.biz.assignment.domain;

import lombok.Data;

@Data
public class Assignment {
  private Long id, courseId, createdBy;
  private String title, instructions, status, languageCode, courseTitle, courseStatus;
  private java.util.Date dueAt, createTime, updateTime;
  private Integer maxScore, version;
  private Integer pendingCount;
  private String myStatus;
}
