package com.upself.web.controller.business;

import com.upself.biz.business.dto.*;
import com.upself.biz.business.service.*;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/business", "/client/business"})
@PreAuthorize("@ss.hasPermi('business:own')")
public class BusinessController extends BaseController {
  @Autowired private IBusinessIdeaService ideas;
  @Autowired private ICostPlanService costs;

  @GetMapping("/ideas")
  public TableDataInfo list() {
    ideas.verifyStudent();
    startPage();
    try {
      return getDataTable(ideas.list());
    } finally {
      clearPage();
    }
  }

  @GetMapping("/ideas/{id}")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(ideas.detail(id));
  }

  @PostMapping("/ideas")
  public AjaxResult create(@Valid @RequestBody IdeaBodies.Create b) {
    return AjaxResult.success(ideas.create(b));
  }

  @PutMapping("/ideas/{id}")
  public AjaxResult edit(@PathVariable Long id, @Valid @RequestBody IdeaBodies.Update b) {
    ideas.edit(id, b);
    return success();
  }

  @GetMapping("/cost-plan")
  public AjaxResult current() {
    return AjaxResult.success(costs.current());
  }

  @PostMapping("/cost-plan/calculate")
  public AjaxResult calculate(@Valid @RequestBody CostBodies.Input b) {
    return AjaxResult.success(costs.calculate(b));
  }

  @PutMapping("/cost-plan")
  public AjaxResult save(@Valid @RequestBody CostBodies.Save b) {
    return AjaxResult.success(costs.save(b));
  }
}
