package com.upself.web.controller.learning;

import com.upself.biz.learning.service.IBadgeService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/learning/badges")
public class BadgeController extends BaseController {
  @Autowired private IBadgeService service;

  @GetMapping
  @PreAuthorize("@ss.hasPermi('learning:badges:own')")
  public AjaxResult mine() {
    return AjaxResult.success(service.mine());
  }
}
