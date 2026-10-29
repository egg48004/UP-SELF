package com.upself.biz.mentorship.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import lombok.Data;

@Data
public class MentorNote {
  private Long id;
  @JsonIgnore private Long mentorId;
  @JsonIgnore private Long studentId;
  private String content;
  private Integer version;
  private Date createTime;
  private Date updateTime;
}
