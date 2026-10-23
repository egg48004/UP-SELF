package com.upself.biz.learning.dto;
import java.util.*;
import lombok.Data;
public final class ProgressViews {
  @Data public static class Item {
    private Long chapterId;
    private Long courseId;
    private String chapterTitle;
    private String courseTitle;
    private Date completedAt;
    private Date lastVisitedAt;
  }
  @Data public static class Summary {
    private int total;
    private int completed;
    private int percent;
    private Long lastChapterId;
    private List<Item> chapters;
  }
  @Data public static class Overview {
    private Long studentId;
    private String studentName;
    private String username;
    private Long courseId;
    private String courseTitle;
    private int totalChapters;
    private int completedChapters;
    private int assignmentCount;
    private int submittedCount;
    private int gradedCount;
    private int returnedCount;
    private Integer moodLevel;
    private String moodWeek;
  }
}
