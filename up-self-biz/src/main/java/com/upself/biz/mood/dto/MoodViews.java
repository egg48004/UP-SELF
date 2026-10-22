package com.upself.biz.mood.dto;

import com.upself.biz.mood.domain.*;
import java.util.List;
import lombok.Data;

public class MoodViews {
  @Data
  public static class Current {
    private String weekStart, weekEnd, nextWeekStart, timeZone, noticeVersion;
    private boolean acknowledged;
    private MoodEntry entry;
    private List<MoodPerson> viewers;
  }

  @Data
  public static class Trend {
    private String startWeek, endWeek, timeZone;
    private int weeks;
    private List<MoodPoint> points;
  }
}
