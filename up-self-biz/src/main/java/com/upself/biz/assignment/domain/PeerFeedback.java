package com.upself.biz.assignment.domain;

import lombok.Data;

@Data
public class PeerFeedback {
  private Long id, assignmentId, reviewerId, submissionId;
  private Integer attemptNo, version;
  private String strength, suggestion, hideReason, reviewerName, studentName;
  private Boolean hidden, earlierSubmission;
  private java.util.Date sentAt;
  private String answerText;
  private java.util.List<SubmissionFile> files;
}
