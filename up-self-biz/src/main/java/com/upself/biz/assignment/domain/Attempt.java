package com.upself.biz.assignment.domain;

import lombok.Data;

@Data
public class Attempt {
  private Long id, submissionId, reviewedBy;
  private Integer attemptNo, score, maxScore;
  private String answerText, status, feedback, requestKey;
  private Boolean late;
  private java.util.Date submittedAt, reviewedAt;
  private java.util.List<SubmissionFile> files;
}
