package com.upself.biz.mentorship.dto;

import java.util.*;
import javax.validation.constraints.*;
import lombok.Data;

public class EncouragementBodies {
  @Data
  public static class Send {
    @NotBlank(message = "Write an encouraging message.")
    @Size(max = 2000, message = "Use at most 2000 characters.")
    private String body;

    @NotEmpty(message = "Choose at least one student.")
    @Size(max = 100, message = "Choose up to 100 students at a time.")
    private List<
            @NotNull(message = "Choose a valid student.")
            @Min(value = 1, message = "Choose a valid student.") Long>
        studentIds;

    @NotBlank(message = "Refresh before sending.")
    @Pattern(regexp = "[A-Za-z0-9_-]{16,64}", message = "Refresh before sending.")
    private String clientKey;
  }

  @Data
  public static class Recipient {
    private Long id;
    private String name, username;
  }

  @Data
  public static class InboxMessage {
    private Long id;
    private String body, mentorName;
    private Date sentAt;
  }

  @Data
  public static class Actor {
    private Long id, roleId;
    private String status;
  }

  @Data
  public static class SentDetails {
    private com.upself.biz.mentorship.domain.EncouragementMessage message;
    private List<Recipient> recipients;
  }
}
