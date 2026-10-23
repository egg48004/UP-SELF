package com.upself.web.controller.learning;
import com.upself.biz.learning.dto.ProgressBodies.Completion;
import com.upself.biz.learning.service.IProgressService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping({"/learning","/client/learning"})
public class ProgressController extends BaseController {
 @Autowired private IProgressService service;
 @GetMapping("/courses/{id}/progress") @PreAuthorize("@ss.hasPermi('learning:progress:own')")
 public AjaxResult progress(@PathVariable Long id){return AjaxResult.success(service.summary(id));}
 @PostMapping("/chapters/{id}/visit") @PreAuthorize("@ss.hasPermi('learning:progress:own')")
 public AjaxResult visit(@PathVariable Long id){service.visit(id);return AjaxResult.success();}
 @PutMapping("/chapters/{id}/completion") @PreAuthorize("@ss.hasPermi('learning:progress:own')")
 public AjaxResult complete(@PathVariable Long id,@Valid @RequestBody Completion body){service.complete(id,body.getCompleted());return AjaxResult.success();}
 @GetMapping("/resume") @PreAuthorize("@ss.hasPermi('learning:progress:own')")
 public AjaxResult resume(){return AjaxResult.success(service.resume());}
 @GetMapping("/history") @PreAuthorize("@ss.hasPermi('learning:progress:own')")
 public TableDataInfo history(){service.verifyStudent();startPage();try{return getDataTable(service.history());}finally{clearPage();}}
 @GetMapping("/students/overview") @PreAuthorize("@ss.hasPermi('learning:students:overview')")
 public TableDataInfo overview(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long courseId){
  service.verifyMentor();startPage();try{return getDataTable(service.overview(keyword,courseId));}finally{clearPage();}
 }
}
