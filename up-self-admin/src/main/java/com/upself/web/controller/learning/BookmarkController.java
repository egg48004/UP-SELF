package com.upself.web.controller.learning;

import com.upself.biz.learning.service.IBookmarkService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/learning/bookmarks", "/client/learning/bookmarks"})
@PreAuthorize("@ss.hasPermi('learning:bookmark:own')")
public class BookmarkController extends BaseController {
    @Autowired private IBookmarkService service;

    @GetMapping
    public TableDataInfo list() {
        service.verifyStudent();
        startPage();
        try { return getDataTable(service.list()); }
        finally { clearPage(); }
    }

    @PutMapping("/{courseId}")
    public AjaxResult save(@PathVariable Long courseId) {
        service.save(courseId); return success();
    }

    @DeleteMapping("/{courseId}")
    public AjaxResult remove(@PathVariable Long courseId) {
        service.remove(courseId); return success();
    }
}
