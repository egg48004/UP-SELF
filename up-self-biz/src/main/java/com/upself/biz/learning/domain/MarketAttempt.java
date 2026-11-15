package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class MarketAttempt {
  private Long id, taskId, studentId, optionId;
  private String requestKey, reflection, label, outcome, explanation;
  private Integer version;
  private Boolean firstAttempt;
  private java.util.Date createTime;
}
