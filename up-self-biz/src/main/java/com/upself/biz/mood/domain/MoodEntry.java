package com.upself.biz.mood.domain;

import lombok.Data;

/** Owner view. Mentor responses use a separate projection for shared diaries. */
@Data
public class MoodEntry {
  private Long id, studentId;
  private String weekStart, privateNote;
  private Integer moodLevel;
  private boolean noteShared;
  @com.fasterxml.jackson.annotation.JsonIgnore private String recordingKey;
  private Long recordingId, recordingSize;
  private String recordingName, recordingMime;
  private java.util.Date createTime;
}
