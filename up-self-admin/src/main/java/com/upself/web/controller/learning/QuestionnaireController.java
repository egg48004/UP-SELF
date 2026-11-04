package com.upself.web.controller.learning;

import com.upself.biz.learning.dto.QuestionnaireBodies.*;
import com.upself.biz.learning.service.IQuestionnaireService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/questionnaires", "/client/questionnaires"})
public class QuestionnaireController extends BaseController {
  @Autowired private IQuestionnaireService service;

  @GetMapping
  @PreAuthorize("@ss.hasPermi('questionnaires:read')")
  public TableDataInfo visible(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    service.verifyStudent();
    startPage();
    try {
      return getDataTable(service.visible());
    } finally {
      clearPage();
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('questionnaires:read')")
  public AjaxResult detail(@PathVariable Long id, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return AjaxResult.success(service.visibleById(id));
  }

  @GetMapping("/manage")
  @PreAuthorize("@ss.hasPermi('questionnaires:manage')")
  public TableDataInfo manage(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    service.verifyMentor();
    startPage();
    try {
      return getDataTable(service.authored());
    } finally {
      clearPage();
    }
  }

  @PostMapping
  @PreAuthorize("@ss.hasPermi('questionnaires:manage')")
  public AjaxResult create(@Valid @RequestBody Create body) {
    return AjaxResult.success(service.create(body));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('questionnaires:manage')")
  public AjaxResult update(@PathVariable Long id, @Valid @RequestBody Update body) {
    service.update(id, body);
    return success();
  }

  @PutMapping("/{id}/visibility")
  @PreAuthorize("@ss.hasPermi('questionnaires:manage')")
  public AjaxResult visibility(@PathVariable Long id, @Valid @RequestBody Visibility body) {
    service.visibility(id, body);
    return success();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('questionnaires:manage')")
  public AjaxResult delete(@PathVariable Long id, @RequestParam Integer version) {
    service.delete(id, version);
    return success();
  }
}
