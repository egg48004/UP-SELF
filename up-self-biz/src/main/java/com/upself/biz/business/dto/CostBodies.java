package com.upself.biz.business.dto;

import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.*;

public final class CostBodies {
  private CostBodies() {}

  @Data
  public static class Input {
    @NotNull(message = "Enter materials cost.")
    @DecimalMin(value = "0.00", message = "Use an amount of zero or more.")
    @Digits(
        integer = 10,
        fraction = 2,
        message = "Use up to ten integer digits and two decimal places.")
    private BigDecimal materialsCost;

    @NotNull(message = "Enter other costs.")
    @DecimalMin(value = "0.00", message = "Use an amount of zero or more.")
    @Digits(
        integer = 10,
        fraction = 2,
        message = "Use up to ten integer digits and two decimal places.")
    private BigDecimal otherCosts;

    @NotNull(message = "Enter a price per item.")
    @DecimalMin(value = "0.00", message = "Use an amount of zero or more.")
    @Digits(
        integer = 10,
        fraction = 2,
        message = "Use up to ten integer digits and two decimal places.")
    private BigDecimal pricePerItem;

    @NotNull(message = "Enter a whole-number quantity.")
    @DecimalMin(value = "1", message = "Use a quantity of at least one.")
    @DecimalMax(value = "1000000", message = "Use a quantity no greater than 1000000.")
    @Digits(integer = 7, fraction = 0, message = "Enter a whole-number quantity.")
    private BigDecimal quantity;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Save extends Input {
    @NotNull(message = "Refresh the saved plan before saving.")
    @Min(value = 0, message = "Invalid cost plan version.")
    private Integer version;
  }
}
