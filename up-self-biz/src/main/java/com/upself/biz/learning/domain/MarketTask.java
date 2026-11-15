package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class MarketTask {
  private Long id, courseId, createdBy;
  private String title,
      objective,
      situation,
      question,
      reflectionPrompt,
      status,
      courseTitle,
      courseStatus;
  private Integer version, attemptCount;
  private Boolean locked;
  private java.util.List<MarketOption> options;
}
