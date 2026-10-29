package com.upself.biz.business.service;

import com.upself.biz.business.domain.CostPlan;
import com.upself.biz.business.dto.CostBodies.*;

public interface ICostPlanService {
  CostPlan current();

  CostPlan calculate(Input body);

  CostPlan save(Save body);
}
