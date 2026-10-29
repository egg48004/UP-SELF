package com.upself.web.controller.followup;

import com.upself.biz.followup.service.IFollowUpService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/follow-ups", "/client/follow-ups"})
@PreAuthorize("@ss.hasPermi('followups:read')")
public class FollowUpController extends BaseController {
  @Autowired private IFollowUpService service;

  @GetMapping
  public TableDataInfo list(
      @RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(required = false) Long studentId) {
    service.refreshCurrent();
    startPage();
    try {
      return getDataTable(service.list(status, studentId));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/summary")
  public AjaxResult summary() {
    service.refreshCurrent();
    return AjaxResult.success(service.summary());
  }

  @Data
  public static class Review {
    @NotNull(message = "Reload the reminder first.")
    @Min(value = 0, message = "Invalid reminder version.")
    private Integer version;
  }

  @PutMapping("/{id}/review")
  public AjaxResult review(@PathVariable Long id, @Valid @RequestBody Review b) {
    service.review(id, b.getVersion());
    return success();
  }
}
