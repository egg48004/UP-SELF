package com.upself.biz.goal.dto;

import javax.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class GoalBodies {
    private GoalBodies() {}

    @Data
    public static class Create {
        @NotBlank(message = "Please enter a goal.")
        @Size(max = 150, message = "Use at most 150 characters for your goal.")
        private String title;
        @Size(max = 2000, message = "Use at most 2000 characters for your notes.")
        private String details;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Update extends Create {
        @NotNull(message = "Refresh your goals and try again.")
        @Min(value = 0, message = "Invalid goal version.")
        private Integer version;
    }

    @Data
    public static class Completion {
        @NotNull(message = "Refresh your goals and try again.")
        @Min(value = 0, message = "Invalid goal version.")
        private Integer version;
        @NotNull(message = "Choose a completion status.")
        private Boolean completed;
    }
}
