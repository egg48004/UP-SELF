package com.upself.biz.followup.domain;

import java.util.Date;
import lombok.Data;

@Data
public class FollowUpReminder {
  private Long id, mentorId, studentId, scopeKey, courseId, assignmentId;
  private String kind, reason, eventDate, studentName, username, courseTitle;
  private boolean active, reviewed;
  private Integer version;
  private Date detectedAt, reviewedAt;
}
