package com.upself.biz.learning.dto;

import javax.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class QuestionnaireBodies {
  private QuestionnaireBodies() {}

  @Data
  public static class Create {
    @NotBlank(message = "This field is required.")
    @Size(max = 150, message = "Use at most {max} characters.")
    private String title;

    @NotBlank(message = "This field is required.")
    @Size(max = 2000, message = "Use at most {max} characters.")
    private String url;

    @NotNull(message = "This field is required.")
    private Boolean visible = false;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Update extends Create {
    @NotNull(message = "This field is required.")
    @Min(value = 0, message = "Refresh the questionnaire and try again.")
    private Integer version;
  }

  @Data
  public static class Visibility {
    @NotNull(message = "This field is required.")
    private Boolean visible;

    @NotNull(message = "This field is required.")
    @Min(value = 0, message = "Refresh the questionnaire and try again.")
    private Integer version;
  }
}
