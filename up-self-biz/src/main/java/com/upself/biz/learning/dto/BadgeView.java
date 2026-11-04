package com.upself.biz.learning.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/** Own-student projection; contains no source record IDs or other students. */
@Data
public class BadgeView {
  private String code;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT+8")
  private Date awardedAt;
}
