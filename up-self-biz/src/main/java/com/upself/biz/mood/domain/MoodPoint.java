package com.upself.biz.mood.domain;

import lombok.Data;

/** Public-to-current-mentor projection: deliberately has no private-note property. */
@Data
public class MoodPoint {
  private String weekStart;
  private Integer moodLevel;

  public MoodPoint() {}

  public MoodPoint(String weekStart, Integer moodLevel) {
    this.weekStart = weekStart;
    this.moodLevel = moodLevel;
  }
}
