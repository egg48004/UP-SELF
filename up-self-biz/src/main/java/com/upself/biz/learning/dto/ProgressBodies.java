package com.upself.biz.learning.dto;
import lombok.Data;
import javax.validation.constraints.NotNull;
public final class ProgressBodies {
  @Data public static class Completion {
    @NotNull(message = "{field.required}") private Boolean completed;
  }
}
