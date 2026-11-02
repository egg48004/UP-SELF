package com.upself.web.controller.mentorship;

import com.upself.biz.mentorship.dto.EncouragementBodies.Send;
import com.upself.biz.mentorship.service.IEncouragementService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/encouragement", "/client/encouragement"})
public class EncouragementController extends BaseController {
  @Autowired private IEncouragementService service;

  @GetMapping("/students")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:send')")
  public TableDataInfo students(@RequestParam(required = false) String keyword) {
    service.verifyMentor();
    if (keyword != null && keyword.length() > 100)
      throw new com.upself.common.exception.ServiceException(
          400, "Use at most 100 search characters.");
    startPage();
    try {
      return getDataTable(service.students(keyword));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/students/{id}")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:send')")
  public AjaxResult student(@PathVariable Long id) {
    return AjaxResult.success(service.student(id));
  }

  @PostMapping("/messages")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:send')")
  @com.upself.common.annotation.RateLimiter(
      time = 60,
      count = 20,
      limitType = com.upself.common.enums.LimitType.USER_ID)
  public AjaxResult send(@Valid @RequestBody Send body) {
    return AjaxResult.success(service.send(body));
  }

  @GetMapping("/sent")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:send')")
  public TableDataInfo history() {
    service.verifyMentor();
    startPage();
    try {
      return getDataTable(service.history());
    } finally {
      clearPage();
    }
  }

  @GetMapping("/sent/{id}")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:send')")
  public AjaxResult sent(@PathVariable Long id) {
    return AjaxResult.success(service.sent(id));
  }

  @GetMapping("/inbox")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:read')")
  public TableDataInfo inbox() {
    service.verifyStudent();
    startPage();
    try {
      return getDataTable(service.inbox());
    } finally {
      clearPage();
    }
  }

  @GetMapping("/inbox/{id}")
  @PreAuthorize("@ss.hasPermi('mentorship:encouragement:read')")
  public AjaxResult received(@PathVariable Long id) {
    return AjaxResult.success(service.received(id));
  }
}
