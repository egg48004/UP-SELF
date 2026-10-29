package com.upself.biz.learning.dto;

import lombok.Data;

public final class LearningStatistics {
  @Data
  public static class Totals {
    private int courseCount;
    private int totalChapters;
    private int completedChapters;
    private int percent;
    private int assignmentCount;
    private int notSubmittedCount;
    private int submittedCount;
    private int gradedCount;
    private int returnedCount;
  }

  @Data
  public static class CourseRow {
    private Long courseId;
    private String courseTitle;
    private int totalChapters;
    private int completedChapters;
    private int percent;
    private int assignmentCount;
    private int notSubmittedCount;
    private int submittedCount;
    private int gradedCount;
    private int returnedCount;
  }
}
