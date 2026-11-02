package com.upself.biz.mentorship.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import lombok.Data;

@Data
public class EncouragementMessage {
  private Long id;
  @JsonIgnore private Long mentorId;
  @JsonIgnore private String clientKey;
  private String body;
  private int recipientCount;
  private Date sentAt;
}
