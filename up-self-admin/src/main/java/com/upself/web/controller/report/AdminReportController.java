package com.upself.web.controller.report;

import com.upself.biz.report.dto.ReportBodies.Review;
import com.upself.biz.report.service.IReportService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/moderation/reports", "/client/moderation/reports"})
@PreAuthorize("@ss.hasPermi('reports:review')")
public class AdminReportController extends BaseController {
  @Autowired private IReportService service;

  @GetMapping
  public TableDataInfo list(@RequestParam(defaultValue = "PENDING") String status) {
    service.administrator();
    startPage();
    try {
      return getDataTable(service.list(status));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/{id}")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(service.detail(id));
  }

  @PutMapping("/{id}")
  public AjaxResult review(@PathVariable Long id, @Valid @RequestBody Review b) {
    service.review(id, b);
    return success();
  }
}
