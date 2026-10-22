package com.upself.biz.assignment.domain;

import lombok.Data;

@Data
public class SubmissionFile {
  private Long id, submissionId;
  private Integer attemptNo;
  private String originalName, mimeType;
  @com.fasterxml.jackson.annotation.JsonIgnore private String storageKey;
  private Long fileSize;
}
