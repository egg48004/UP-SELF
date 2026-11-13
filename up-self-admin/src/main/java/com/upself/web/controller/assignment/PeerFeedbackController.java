package com.upself.web.controller.assignment;

import com.upself.biz.assignment.dto.PeerBodies;
import com.upself.biz.assignment.service.PeerFeedbackService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/assignments/{id}/peer-feedback", "/client/assignments/{id}/peer-feedback"})
public class PeerFeedbackController extends BaseController {
  @Autowired private PeerFeedbackService service;

  @ModelAttribute
  public void privateResponse(javax.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
  }

  @GetMapping
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult state(@PathVariable Long id) {
    return AjaxResult.success(service.state(id));
  }

  @PostMapping("/allocate")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult allocate(@PathVariable Long id) {
    return AjaxResult.success(service.allocate(id));
  }

  @PostMapping
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult send(@PathVariable Long id, @Valid @RequestBody PeerBodies.Feedback body) {
    service.send(id, body);
    return success();
  }

  @GetMapping("/manage")
  @PreAuthorize("@ss.hasPermi('assignment:review')")
  public TableDataInfo manage(@PathVariable Long id) {
    service.mentorAccess(id);
    startPage();
    try {
      return getDataTable(service.managed(id));
    } finally {
      clearPage();
    }
  }

  @PutMapping("/{feedbackId}/hide")
  @PreAuthorize("@ss.hasPermi('assignment:review')")
  public AjaxResult hide(
      @PathVariable Long id,
      @PathVariable Long feedbackId,
      @Valid @RequestBody PeerBodies.Hide body) {
    service.hide(id, feedbackId, body);
    return success();
  }
}
