package com.upself.web.controller.goal;

import com.upself.biz.goal.dto.GoalBodies.*;
import com.upself.biz.goal.service.IGoalService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/goals")
@PreAuthorize("@ss.hasPermi('goals:own')")
public class GoalController extends BaseController {
    @Autowired private IGoalService service;

    @GetMapping
    public TableDataInfo list(@RequestParam(required = false) String status) {
        service.verifyStudent();
        startPage();
        try { return getDataTable(service.list(status)); }
        finally { clearPage(); }
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return AjaxResult.success(service.detail(id)); }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody Create body) {
        return AjaxResult.success(service.create(body));
    }

    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Valid @RequestBody Update body) {
        service.edit(id, body); return success();
    }

    @PutMapping("/{id}/completion")
    public AjaxResult complete(@PathVariable Long id, @Valid @RequestBody Completion body) {
        service.complete(id, body); return success();
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id, @RequestParam Integer version) {
        service.delete(id, version); return success();
    }
}
