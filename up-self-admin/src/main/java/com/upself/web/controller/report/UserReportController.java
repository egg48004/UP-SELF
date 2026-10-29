package com.upself.web.controller.report;

import com.upself.biz.report.dto.ReportBodies.*;
import com.upself.biz.report.service.IReportService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/reports", "/client/reports"})
@PreAuthorize("@ss.hasPermi('reports:own')")
public class UserReportController extends BaseController {
  @Autowired private IReportService service;

  @PostMapping
  public AjaxResult create(@Valid @RequestBody Create b) {
    return AjaxResult.success(service.create(b));
  }

  @GetMapping("/mine")
  public TableDataInfo mine() {
    service.participant();
    startPage();
    try {
      return getDataTable(service.mine());
    } finally {
      clearPage();
    }
  }
}
