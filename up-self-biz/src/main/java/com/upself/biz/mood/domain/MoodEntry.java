package com.upself.biz.mood.domain;

import lombok.Data;

/** Owner-only view. Never use this type for mentor responses. */
@Data
public class MoodEntry {
  private Long id, studentId;
  private String weekStart, privateNote;
  private Integer moodLevel;
  private java.util.Date createTime;
}
