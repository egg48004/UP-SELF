package com.upself.web.controller.mentorship;

import com.upself.biz.mentorship.dto.NoteBodies.*;
import com.upself.biz.mentorship.service.IMentorNoteService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({
  "/mentorship/students/{studentId}/notes",
  "/client/mentorship/students/{studentId}/notes"
})
@PreAuthorize("@ss.hasPermi('mentorship:notes:read')")
public class MentorNoteController extends BaseController {
  @Autowired private IMentorNoteService service;

  @GetMapping("/student")
  public AjaxResult student(@PathVariable Long studentId) {
    return AjaxResult.success(service.verifyStudent(studentId));
  }

  @GetMapping
  public TableDataInfo list(@PathVariable Long studentId) {
    service.verifyStudent(studentId);
    startPage();
    try {
      return getDataTable(service.list(studentId));
    } finally {
      clearPage();
    }
  }

  @PostMapping
  @PreAuthorize("@ss.hasPermi('mentorship:notes:write')")
  public AjaxResult create(@PathVariable Long studentId, @Valid @RequestBody Create b) {
    return AjaxResult.success(service.create(studentId, b));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('mentorship:notes:write')")
  public AjaxResult edit(
      @PathVariable Long studentId, @PathVariable Long id, @Valid @RequestBody Update b) {
    service.edit(studentId, id, b);
    return success();
  }
}
