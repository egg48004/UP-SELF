package com.upself.biz.knowledge.dto;

import javax.validation.constraints.*;
import lombok.Data;

public class KnowledgeBodies {
    @Data
    public static class QuestionInput {
        @NotBlank(message = "Enter a question.")
        @Size(max = 500, message = "Use at most 500 characters for the question.")
        private String questionText;
        @NotNull(message = "Choose a question status.")
        @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "Choose Draft, Published or Archived.")
        private String status = "DRAFT";
        @NotNull(message = "Refresh the question before saving.")
        @Min(value = 0, message = "Refresh the question before saving.")
        private Integer version = 0;
    }

    @Data
    public static class AnswerInput {
        @NotNull(message = "Choose Yes, No or Not sure.")
        @Pattern(regexp = "YES|NO|NOT_SURE", message = "Choose Yes, No or Not sure.")
        private String choice;
        @NotNull(message = "Refresh the question before answering.")
        @Min(value = 0, message = "Refresh the question before answering.")
        private Integer questionVersion;
    }
}
