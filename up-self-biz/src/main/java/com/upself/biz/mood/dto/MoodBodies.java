package com.upself.biz.mood.dto;

import javax.validation.constraints.*;
import lombok.Data;

public class MoodBodies {
  @Data
  public static class Acknowledgment {
    @NotBlank(message = "Reload the sharing notice.")
    private String noticeVersion;

    @NotNull(message = "Confirm that you understand who can view your mood.")
    @AssertTrue(message = "Confirm that you understand who can view your mood.")
    private Boolean accepted;
  }

  @Data
  public static class EntryInput {
    @NotBlank(message = "Reload the current week.")
    @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}", message = "Reload the current week.")
    private String weekStart;

    @NotNull(message = "Choose a mood.")
    @Min(value = 1, message = "Choose a mood from the five options.")
    @Max(value = 5, message = "Choose a mood from the five options.")
    private Integer moodLevel;

    @NotNull(message = "Use an empty note if you do not want to add one.")
    @Size(max = 2000, message = "Use at most 2,000 characters for your diary.")
    private String privateNote = "";
  }
}
