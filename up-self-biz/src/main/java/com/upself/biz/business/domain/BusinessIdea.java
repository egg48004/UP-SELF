package com.upself.biz.business.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import lombok.Data;

@Data
public class BusinessIdea {
  private Long id;
  @JsonIgnore private Long studentId;
  private String title;
  private String offer;
  private String audience;
  private String resources;
  private String nextStep;
  private Integer version;
  private Date createTime;
  private Date updateTime;
}
