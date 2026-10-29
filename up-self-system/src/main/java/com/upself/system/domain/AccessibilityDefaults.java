package com.upself.system.domain;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class AccessibilityDefaults {
  @NotNull(message = "Choose a text size.")
  @Pattern(regexp = "standard|large|extraLarge", message = "Choose a valid text size.")
  private String size;

  @NotNull(message = "Choose a contrast setting.")
  @Pattern(regexp = "standard|highContrast", message = "Choose a valid contrast setting.")
  private String contrast;

  @NotNull(message = "Choose whether to reduce motion.")
  private Boolean motion;

  @NotNull(message = "Reload the settings before saving.")
  @Min(value = 0, message = "Reload the settings before saving.")
  private Integer version;
}
