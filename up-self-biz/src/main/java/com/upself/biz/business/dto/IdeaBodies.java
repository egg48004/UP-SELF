package com.upself.biz.business.dto;

import javax.validation.constraints.*;
import lombok.*;

public final class IdeaBodies {
  private IdeaBodies() {}

  @Data
  public static class Create {
    @NotBlank(message = "Enter an idea name.")
    @Size(max = 100, message = "Use at most 100 characters for the name.")
    private String title;

    @Size(max = 500, message = "Use at most 500 characters per answer.")
    private String offer;

    @Size(max = 500, message = "Use at most 500 characters per answer.")
    private String audience;

    @Size(max = 500, message = "Use at most 500 characters per answer.")
    private String resources;

    @Size(max = 500, message = "Use at most 500 characters per answer.")
    private String nextStep;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Update extends Create {
    @NotNull(message = "Refresh the idea before saving.")
    @Min(value = 0, message = "Invalid idea version.")
    private Integer version;
  }
}
