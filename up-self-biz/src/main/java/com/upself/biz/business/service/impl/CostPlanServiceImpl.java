package com.upself.biz.business.service.impl;

import com.upself.biz.business.domain.CostPlan;
import com.upself.biz.business.dto.CostBodies.*;
import com.upself.biz.business.mapper.CostPlanMapper;
import com.upself.biz.business.service.ICostPlanService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CostPlanServiceImpl implements ICostPlanService {
  @Autowired private CostPlanMapper mapper;

  private Long owner() {
    if (!"student".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Cost plans are available to students only.");
    return SecurityUtils.getUserId();
  }

  private CostPlan results(CostPlan p) {
    p.setTotalCost(
        p.getMaterialsCost().add(p.getOtherCosts()).setScale(2, RoundingMode.UNNECESSARY));
    p.setExpectedRevenue(
        p.getPricePerItem()
            .multiply(BigDecimal.valueOf(p.getQuantity()))
            .setScale(2, RoundingMode.UNNECESSARY));
    p.setEstimatedProfit(p.getExpectedRevenue().subtract(p.getTotalCost()));
    return p;
  }

  private BigDecimal money(BigDecimal x) {
    if (x == null
        || x.signum() < 0
        || x.compareTo(new BigDecimal("9999999999.99")) > 0
        || x.stripTrailingZeros().scale() > 2)
      throw new ServiceException(
          400, "Enter a non-negative amount using at most two decimal places.");
    return x.setScale(2, RoundingMode.UNNECESSARY);
  }

  public CostPlan calculate(Input b) {
    Long id = owner();
    CostPlan p = new CostPlan();
    p.setStudentId(id);
    p.setMaterialsCost(money(b.getMaterialsCost()));
    p.setOtherCosts(money(b.getOtherCosts()));
    p.setPricePerItem(money(b.getPricePerItem()));
    try {
      p.setQuantity(b.getQuantity().intValueExact());
    } catch (Exception e) {
      throw new ServiceException(400, "Enter a whole-number quantity from 1 to 1000000.");
    }
    if (p.getQuantity() < 1 || p.getQuantity() > 1000000)
      throw new ServiceException(400, "Enter a whole-number quantity from 1 to 1000000.");
    return results(p);
  }

  public CostPlan current() {
    CostPlan p = mapper.own(owner());
    if (p == null) return new CostPlan();
    p.setSaved(true);
    return results(p);
  }

  @Transactional
  public CostPlan save(Save b) {
    CostPlan p = calculate(b);
    if (b.getVersion() == null || b.getVersion() < 0)
      throw new ServiceException(400, "Refresh the saved plan before saving.");
    p.setVersion(b.getVersion());
    try {
      if ((b.getVersion() == 0 ? mapper.insert(p) : mapper.edit(p)) != 1) throw stale();
    } catch (DuplicateKeyException e) {
      throw stale();
    }
    return current();
  }

  private ServiceException stale() {
    return new ServiceException(
        409, "This plan has changed. Reload the saved plan before saving again.");
  }
}
