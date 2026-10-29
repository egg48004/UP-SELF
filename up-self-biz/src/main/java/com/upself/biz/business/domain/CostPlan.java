package com.upself.biz.business.domain;

import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class CostPlan {
  @JsonIgnore private Long studentId;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal materialsCost;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal otherCosts;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal pricePerItem;

  private Integer quantity;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal totalCost;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal expectedRevenue;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private BigDecimal estimatedProfit;

  private String currency = "MYR";
  private Integer version = 0;
  private boolean saved;
  private Date updateTime;
}
