package com.upself.biz.mentorship.dto;

import javax.validation.constraints.*;
import lombok.*;

public final class NoteBodies {
  private NoteBodies() {}

  @Data
  public static class Create {
    @NotBlank(message = "Write a note before saving.")
    @Size(max = 2000, message = "Use at most 2000 characters.")
    private String content;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Update extends Create {
    @NotNull(message = "Refresh the notes before saving.")
    @Min(value = 0, message = "Invalid note version.")
    private Integer version;
  }

  @Data
  public static class Student {
    private Long id;
    private String username;
    private String name;
  }
}
