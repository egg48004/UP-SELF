package com.upself.biz.learning.dto;

import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

public class MarketBodies {
  @Data
  public static class Option {
    @NotBlank(message = "Enter an option.")
    @Size(max = 300, message = "Use at most 300 characters per option.")
    private String label;

    @NotBlank(message = "Explain what happens.")
    @Size(max = 1000, message = "Use at most 1000 characters for the outcome.")
    private String outcome;

    @NotBlank(message = "Explain why it matters.")
    @Size(max = 1000, message = "Use at most 1000 characters for the explanation.")
    private String explanation;
  }

  @Data
  public static class Content {
    @NotNull(message = "Choose a course.")
    private Long courseId;

    @NotBlank(message = "Enter a task title.")
    @Size(max = 150, message = "Use at most 150 characters for the title.")
    private String title;

    @NotBlank(message = "Enter a learning goal.")
    @Size(max = 300, message = "Use at most 300 characters for the learning goal.")
    private String objective;

    @NotBlank(message = "Describe the situation.")
    @Size(max = 2000, message = "Use at most 2000 characters for the situation.")
    private String situation;

    @NotBlank(message = "Enter a decision question.")
    @Size(max = 300, message = "Use at most 300 characters for the question.")
    private String question;

    @NotBlank(message = "Enter a reflection prompt.")
    @Size(max = 300, message = "Use at most 300 characters for the reflection prompt.")
    private String reflectionPrompt;

    @NotNull(message = "Add two or three options.")
    @Size(min = 2, max = 3, message = "Add two or three options.")
    @Valid
    private java.util.List<@NotNull Option> options;

    @NotNull(message = "Reload the task.")
    @Min(0)
    private Integer version = 0;
  }

  @Data
  public static class State {
    @NotNull
    @Min(0)
    private Integer version;

    @NotNull(message = "Choose a task status.")
    @Pattern(regexp = "DRAFT|PUBLISHED|CLOSED", message = "Choose a valid task status.")
    private String status;
  }

  @Data
  public static class Choice {
    @NotNull(message = "Choose a response.")
    private Long optionId;

    @NotNull(message = "Reload the task.")
    @Min(0)
    private Integer version;

    @NotBlank(message = "Reload and try again.")
    @Pattern(regexp = "[a-fA-F0-9-]{36}", message = "Invalid attempt request.")
    private String requestKey;
  }

  @Data
  public static class Reflection {
    @NotNull
    @Min(0)
    private Integer version;

    @NotNull
    @Size(max = 1000, message = "Use at most 1000 characters for your reflection.")
    private String reflection;
  }
}
