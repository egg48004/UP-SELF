package com.upself.biz.learning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Questionnaire {
  private Long id;
  @JsonIgnore private Long createdBy;
  private String title, url;
  private Boolean visible;
  private Integer version;
}
