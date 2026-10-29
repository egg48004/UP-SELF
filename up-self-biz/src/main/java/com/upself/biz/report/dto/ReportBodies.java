package com.upself.biz.report.dto;

import java.util.Date;
import javax.validation.constraints.*;
import lombok.Data;

public final class ReportBodies {
  @Data
  public static class Create {
    @NotNull(message = "Choose a message to report.")
    @Min(value = 1, message = "Choose a valid message.")
    private Long messageId;

    @NotBlank(message = "Choose a reason.")
    @Pattern(regexp = "HURTFUL|SPAM|INAPPROPRIATE|OTHER", message = "Choose a listed reason.")
    private String reason;

    @Size(max = 500, message = "Use at most 500 characters for details.")
    private String details;
  }

  @Data
  public static class Review {
    @NotBlank(message = "Choose Resolve or Dismiss.")
    @Pattern(regexp = "RESOLVED|DISMISSED", message = "Choose Resolve or Dismiss.")
    private String status;

    @NotBlank(message = "Enter a short processing note.")
    @Size(max = 1000, message = "Use at most 1000 characters.")
    private String resolution;

    @NotNull(message = "Reload the report before reviewing.")
    @Min(value = 0, message = "Invalid report version.")
    private Integer version;
  }

  @Data
  public static class Own {
    private Long id;
    private String accusedName, reason, status;
    private Date createTime, reviewedAt;
  }
}
