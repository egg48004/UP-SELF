package com.upself.biz.report.domain;

import java.util.Date;
import lombok.Data;

@Data
public class UserReport {
  private Long id, reporterId, accusedId, messageId, reviewedBy;
  private String reporterName,
      accusedName,
      courseTitle,
      groupTitle,
      messageBody,
      attachmentSummary,
      reason,
      details,
      status,
      resolution;
  private Integer version;
  private Date createTime, reviewedAt;
}
