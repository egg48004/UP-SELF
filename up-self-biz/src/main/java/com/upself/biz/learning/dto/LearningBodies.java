package com.upself.biz.learning.dto;

import lombok.Data;

import javax.validation.constraints.*;

public final class LearningBodies {
    @Data
    public static class CourseInput {
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.tooLong}")
        private String title;

        @NotNull(message = "{field.required}")
        @Size(max = 20000, message = "{field.tooLong}")
        private String description;

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*", message = "{language.invalid}")
        @Size(max = 35, message = "{field.tooLong}")
        private String languageCode = "en";
    }

    @Data
    public static class MentorInput {
        @NotNull(message = "{field.required}")
        private Long mentorId;
    }

    @Data
    public static class EnrollmentInput {
        @NotNull(message = "{field.required}")
        private Long studentId;
    }

    @Data
    public static class StateInput {
        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "{state.invalid}")
        private String status;
    }

    @Data
    public static class ChapterInput {
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.tooLong}")
        private String title;

        @NotNull(message = "{field.required}")
        @Size(max = 20000, message = "{field.tooLong}")
        private String body;

        @NotNull(message = "{field.required}")
        @Min(value = 0, message = "{order.invalid}")
        @Max(value = 99999, message = "{order.invalid}")
        private Integer sortOrder = 0;

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "DRAFT|PUBLISHED", message = "{state.invalid}")
        private String status = "DRAFT";

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*", message = "{language.invalid}")
        @Size(max = 35, message = "{field.tooLong}")
        private String languageCode = "en";
    }

    @Data
    public static class ResourceInput {
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.tooLong}")
        private String title;

        @Size(max = 500, message = "{field.tooLong}")
        private String altText;

        private Long captionResourceId;

        @Size(max = 20000, message = "{field.tooLong}")
        private String transcript;

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "DRAFT|PUBLISHED", message = "{state.invalid}")
        private String status = "DRAFT";

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*", message = "{language.invalid}")
        @Size(max = 35, message = "{field.tooLong}")
        private String languageCode = "en";
    }

    @Data
    public static class PostInput {
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.tooLong}")
        private String title;

        @NotBlank(message = "{field.required}")
        @Size(max = 20000, message = "{field.tooLong}")
        private String body;

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "ANNOUNCEMENT|MEETING", message = "{post.type.invalid}")
        private String type = "ANNOUNCEMENT";

        @Size(max = 2000, message = "{field.tooLong}")
        private String meetingUrl;

        @Size(max = 40, message = "{field.tooLong}")
        private String meetingStartsAt;

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "DRAFT|PUBLISHED", message = "{state.invalid}")
        private String status = "DRAFT";

        @NotBlank(message = "{field.required}")
        @Pattern(regexp = "[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*", message = "{language.invalid}")
        @Size(max = 35, message = "{field.tooLong}")
        private String languageCode = "en";
    }
}
