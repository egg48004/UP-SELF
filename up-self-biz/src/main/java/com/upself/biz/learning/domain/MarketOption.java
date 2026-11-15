package com.upself.biz.learning.domain;

import lombok.Data;

@Data
public class MarketOption {
  private Long id, taskId;
  private Integer position;
  private String label, outcome, explanation;
}
