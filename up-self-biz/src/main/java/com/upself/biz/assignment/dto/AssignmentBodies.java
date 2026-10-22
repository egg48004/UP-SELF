package com.upself.biz.assignment.dto;

import javax.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class AssignmentBodies {
  @Data
  public static class AssignmentInput {
    @NotNull(message = "Choose a course.")
    private Long courseId;

    @NotBlank(message = "Enter a title.")
    @Size(max = 150, message = "Use at most 150 characters for the title.")
    private String title;

    @NotBlank(message = "Enter the instructions.")
    @Size(max = 20000, message = "Instructions are too long.")
    private String instructions;

    @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "Choose a valid status.")
    @NotNull(message = "Choose a status.")
    private String status = "DRAFT";

    @Size(max = 40, message = "Invalid due date.")
    private String dueAt;

    @NotNull(message = "Enter the maximum score.")
    @Min(value = 1, message = "Maximum score must be at least 1.")
    @Max(value = 1000, message = "Maximum score cannot exceed 1000.")
    private Integer maxScore = 100;

    @NotNull(message = "Reload the assignment.")
    @Min(value = 0, message = "Reload the latest version.")
    private Integer version = 0;

    @Pattern(regexp = "[a-zA-Z]{2,8}([-_][a-zA-Z0-9]{1,8})*", message = "Invalid language.")
    @NotNull(message = "Choose a language.")
    private String languageCode = "en";
  }

  @Data
  public static class DraftInput {
    @NotNull(message = "Reload your draft.")
    @Min(value = 0, message = "Reload the latest version.")
    private Integer version = 0;

    @Size(max = 20000, message = "Your answer is too long.")
    @NotNull(message = "Enter an answer or attach a file.")
    private String answerText = "";
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SubmitInput extends DraftInput {
    @NotNull(message = "Reload the assignment.")
    @Min(value = 0, message = "Reload the latest version.")
    private Integer assignmentVersion;

    @NotBlank(message = "Reload and try again.")
    @Pattern(regexp = "[a-fA-F0-9-]{32,36}", message = "Invalid submission request.")
    private String requestKey;
  }

  @Data
  public static class ReviewInput {
    @NotNull(message = "Reload the submission.")
    @Min(value = 0, message = "Reload the latest version.")
    private Integer version;

    @NotBlank(message = "Write feedback for the student.")
    @Size(max = 10000, message = "Feedback is too long.")
    private String feedback;

    @NotNull(message = "Choose a review decision.")
    @Pattern(regexp = "GRADED|RETURNED", message = "Invalid review decision.")
    private String decision;

    @Min(value = 0, message = "Score cannot be negative.")
    @Max(value = 1000, message = "Score cannot exceed 1000.")
    private Integer score;
  }
}
