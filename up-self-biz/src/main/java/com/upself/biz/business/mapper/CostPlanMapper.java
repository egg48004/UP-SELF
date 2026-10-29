package com.upself.biz.business.mapper;

import com.upself.biz.business.domain.CostPlan;
import org.apache.ibatis.annotations.Param;

public interface CostPlanMapper {
  CostPlan own(@Param("studentId") Long studentId);

  int insert(CostPlan plan);

  int edit(CostPlan plan);
}
