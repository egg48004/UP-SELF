package com.upself.biz.selfefficacy.dto;

import java.util.List;
import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;

public class SelfEfficacyBodies {
    @Data
    public static class Acknowledgment {
        @NotBlank(message = "Reload the sharing notice.")
        private String noticeVersion;
        @NotNull(message = "Confirm who can view your check-ins.")
        @AssertTrue(message = "Confirm who can view your check-ins.")
        private Boolean accepted;
    }

    @Data
    public static class EntryInput {
        @NotBlank(message = "Reload the current check-in.")
        private String assessmentDate;
        @NotBlank(message = "Reload the questionnaire.")
        private String instrumentVersion;
        @NotNull(message = "Answer all three statements.")
        @Size(min = 3, max = 3, message = "Answer all three statements.")
        private List<@NotNull @Min(1) @Max(5) BigDecimal> answers;
    }
}
