package com.upself.web.controller.system;

import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.system.domain.AccessibilityDefaults;
import com.upself.system.service.IAccessibilityService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/accessibility/defaults", "/client/accessibility/defaults"})
public class AccessibilityController extends BaseController {
  @Autowired private IAccessibilityService service;

  // Only this small, non-personal GET is public; writes retain normal authentication.
  @GetMapping
  public ResponseEntity<AjaxResult> read() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(AjaxResult.success(service.read()));
  }

  @PutMapping
  @PreAuthorize("@ss.hasPermi('settings:accessibility:edit')")
  public AjaxResult update(@Valid @RequestBody AccessibilityDefaults body) {
    return AjaxResult.success(service.update(body));
  }
}
