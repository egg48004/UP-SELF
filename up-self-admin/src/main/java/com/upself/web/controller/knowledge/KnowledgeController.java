package com.upself.web.controller.knowledge;

import com.upself.biz.knowledge.dto.KnowledgeBodies.*;
import com.upself.biz.knowledge.service.IKnowledgeService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/knowledge", "/client/knowledge"})
public class KnowledgeController extends BaseController {
    @Autowired private IKnowledgeService service;

    @GetMapping("/questions")
    @PreAuthorize("@ss.hasPermi('knowledge:own:read')")
    public TableDataInfo own(@RequestParam(required = false) Long courseId) {
        service.verifyStudent(); startPage();
        try { return getDataTable(service.ownQuestions(courseId)); }
        finally { clearPage(); }
    }
    @GetMapping("/questions/manage")
    @PreAuthorize("@ss.hasPermi('knowledge:questions:manage')")
    public TableDataInfo manage(@RequestParam(required = false) Long courseId) {
        service.verifyMentor(); startPage();
        try { return getDataTable(service.mentorQuestions(courseId)); }
        finally { clearPage(); }
    }
    @PostMapping("/courses/{courseId}/questions")
    @PreAuthorize("@ss.hasPermi('knowledge:questions:manage')")
    public AjaxResult create(@PathVariable Long courseId, @Valid @RequestBody QuestionInput body) {
        return AjaxResult.success(service.create(courseId, body));
    }
    @PutMapping("/questions/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:questions:manage')")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody QuestionInput body) {
        service.update(id, body); return success();
    }
    @DeleteMapping("/questions/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:questions:manage')")
    public AjaxResult delete(@PathVariable Long id, @RequestParam Integer version) {
        service.delete(id, version); return success();
    }
    @PutMapping("/questions/{id}/answer")
    @PreAuthorize("@ss.hasPermi('knowledge:own:record')")
    public AjaxResult answer(@PathVariable Long id, @Valid @RequestBody AnswerInput body) {
        return AjaxResult.success(service.answer(id, body));
    }
    @GetMapping("/questions/{id}/answers")
    @PreAuthorize("@ss.hasPermi('knowledge:answers:read')")
    public TableDataInfo answers(@PathVariable Long id) {
        service.verifyAnswers(id); startPage();
        try { return getDataTable(service.answers(id)); }
        finally { clearPage(); }
    }
}
