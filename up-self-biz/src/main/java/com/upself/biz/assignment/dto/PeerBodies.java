package com.upself.biz.assignment.dto;

import javax.validation.constraints.*;
import lombok.Data;

public class PeerBodies {
  @Data
  public static class Feedback {
    @NotNull(message = "Reload the feedback.")
    @Min(0)
    private Integer version;

    @NotBlank(message = "Share one thing that works well.")
    @Size(max = 500, message = "Use at most 500 characters.")
    private String strength;

    @NotNull
    @Size(max = 500, message = "Use at most 500 characters.")
    private String suggestion = "";
  }

  @Data
  public static class Hide {
    @NotNull
    @Min(0)
    private Integer version;

    @NotBlank(message = "Explain why this feedback is hidden.")
    @Size(max = 300, message = "Use at most 300 characters.")
    private String reason;
  }
}
