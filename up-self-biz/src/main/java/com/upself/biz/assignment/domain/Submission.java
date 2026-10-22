package com.upself.biz.assignment.domain;

import lombok.Data;

@Data
public class Submission {
  private Long id, assignmentId, studentId;
  private String answerText, status, studentName, studentUsername;
  private Integer version, attemptNo;
  private java.util.Date updateTime;
  private java.util.List<SubmissionFile> files;
  private java.util.List<Attempt> attempts;
}
