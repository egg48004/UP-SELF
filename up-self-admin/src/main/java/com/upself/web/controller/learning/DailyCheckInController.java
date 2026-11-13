package com.upself.web.controller.learning;
import com.upself.biz.learning.service.DailyCheckInService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/client/learning/check-in")
public class DailyCheckInController extends BaseController {
 @Autowired private DailyCheckInService service;
 @ModelAttribute public void noStore(javax.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","private, no-store");}
 @GetMapping @PreAuthorize("@ss.hasPermi('learning:checkin:own')") public AjaxResult read(){return AjaxResult.success(service.read());}
 @PostMapping @PreAuthorize("@ss.hasPermi('learning:checkin:own')") public AjaxResult checkIn(){return AjaxResult.success(service.checkIn());}
}
