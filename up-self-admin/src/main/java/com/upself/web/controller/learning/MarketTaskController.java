package com.upself.web.controller.learning;

import com.upself.biz.learning.dto.MarketBodies.*;
import com.upself.biz.learning.service.IMarketTaskService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/market-tasks", "/client/market-tasks"})
public class MarketTaskController extends BaseController {
  @Autowired private IMarketTaskService service;

  @ModelAttribute
  public void privateResponse(javax.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
  }

  @GetMapping
  @PreAuthorize("@ss.hasPermi('market:own') or @ss.hasPermi('market:manage')")
  public TableDataInfo list(@RequestParam(required = false) Long courseId) {
    service.listAccess(courseId);
    startPage();
    try {
      return getDataTable(service.list(courseId));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('market:own') or @ss.hasPermi('market:manage')")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(service.detail(id));
  }

  @PostMapping
  @PreAuthorize("@ss.hasPermi('market:manage')")
  public AjaxResult create(@Valid @RequestBody Content body) {
    return AjaxResult.success(service.create(body));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('market:manage')")
  public AjaxResult update(@PathVariable Long id, @Valid @RequestBody Content body) {
    service.update(id, body);
    return success();
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("@ss.hasPermi('market:manage')")
  public AjaxResult status(@PathVariable Long id, @Valid @RequestBody State body) {
    service.status(id, body);
    return success();
  }

  @PostMapping("/{id}/copy")
  @PreAuthorize("@ss.hasPermi('market:manage')")
  public AjaxResult copy(@PathVariable Long id) {
    return AjaxResult.success(service.copy(id));
  }

  @PostMapping("/{id}/attempts")
  @PreAuthorize("@ss.hasPermi('market:own')")
  public AjaxResult choose(@PathVariable Long id, @Valid @RequestBody Choice body) {
    return AjaxResult.success(service.choose(id, body));
  }

  @GetMapping("/{id}/attempts")
  @PreAuthorize("@ss.hasPermi('market:own') or @ss.hasPermi('market:manage')")
  public TableDataInfo history(
      @PathVariable Long id, @RequestParam(required = false) Long studentId) {
    Long owner = service.historyAccess(id, studentId);
    startPage();
    try {
      return getDataTable(service.history(id, owner));
    } finally {
      clearPage();
    }
  }

  @PutMapping("/{id}/attempts/{attemptId}/reflection")
  @PreAuthorize("@ss.hasPermi('market:own')")
  public AjaxResult reflection(
      @PathVariable Long id, @PathVariable Long attemptId, @Valid @RequestBody Reflection body) {
    service.reflection(id, attemptId, body);
    return success();
  }

  @GetMapping("/{id}/participants")
  @PreAuthorize("@ss.hasPermi('market:manage')")
  public TableDataInfo participants(@PathVariable Long id) {
    service.mentorAccess(id);
    startPage();
    try {
      return getDataTable(service.participants(id));
    } finally {
      clearPage();
    }
  }
}
