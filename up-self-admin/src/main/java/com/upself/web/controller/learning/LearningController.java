package com.upself.web.controller.learning;

import com.upself.biz.learning.dto.LearningBodies.*;
import com.upself.biz.learning.service.ILearningService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping({"/learning", "/client/learning"})
public class LearningController extends BaseController {
    @Autowired private ILearningService service;

    @GetMapping("/courses")
    @PreAuthorize("@ss.hasPermi('learning:course:query')")
    public TableDataInfo courses(@RequestParam(required = false) String keyword) {
        startPage();
        try {
            return getDataTable(service.courses(keyword));
        } finally {
            clearPage();
        }
    }

    @GetMapping("/courses/{id}")
    @PreAuthorize("@ss.hasPermi('learning:course:query')")
    public AjaxResult course(@PathVariable Long id) {
        return AjaxResult.success(service.course(id));
    }

    @PostMapping("/courses")
    @PreAuthorize("@ss.hasPermi('learning:course:add')")
    public AjaxResult create(@Valid @RequestBody CourseInput b) {
        return AjaxResult.success(service.createCourse(b));
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize("@ss.hasPermi('learning:course:edit')")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody CourseInput b) {
        service.updateCourse(id, b);
        return AjaxResult.success();
    }

    @PutMapping("/courses/{id}/mentor")
    @PreAuthorize("@ss.hasPermi('learning:course:assign')")
    public AjaxResult mentor(@PathVariable Long id, @Valid @RequestBody MentorInput b) {
        service.assignMentor(id, b.getMentorId());
        return AjaxResult.success();
    }

    @PutMapping("/courses/{id}/status")
    @PreAuthorize("@ss.hasPermi('learning:course:publish')")
    public AjaxResult state(@PathVariable Long id, @Valid @RequestBody StateInput b) {
        service.courseState(id, b.getStatus());
        return AjaxResult.success();
    }

    @PostMapping("/courses/{id}/students")
    @PreAuthorize("@ss.hasPermi('learning:course:assign')")
    public AjaxResult enroll(@PathVariable Long id, @Valid @RequestBody EnrollmentInput b) {
        service.enroll(id, b.getStudentId());
        return AjaxResult.success();
    }

    @DeleteMapping("/courses/{id}/students/{studentId}")
    @PreAuthorize("@ss.hasPermi('learning:course:assign')")
    public AjaxResult unenroll(@PathVariable Long id, @PathVariable Long studentId) {
        service.unenroll(id, studentId);
        return AjaxResult.success();
    }

    @GetMapping("/courses/{id}/students")
    @PreAuthorize("@ss.hasPermi('learning:course:members')")
    public AjaxResult members(@PathVariable Long id) {
        return AjaxResult.success(service.members(id));
    }

    @GetMapping("/courses/{id}/chapters")
    @PreAuthorize("@ss.hasPermi('learning:chapter:query')")
    public AjaxResult chapters(@PathVariable Long id) {
        return AjaxResult.success(service.chapters(id));
    }

    @GetMapping("/chapters/{id}")
    @PreAuthorize("@ss.hasPermi('learning:chapter:query')")
    public AjaxResult chapter(@PathVariable Long id) {
        return AjaxResult.success(service.chapter(id));
    }

    @PostMapping("/courses/{id}/chapters")
    @PreAuthorize("@ss.hasPermi('learning:chapter:edit')")
    public AjaxResult createChapter(@PathVariable Long id, @Valid @RequestBody ChapterInput b) {
        return AjaxResult.success(service.createChapter(id, b));
    }

    @PutMapping("/chapters/{id}")
    @PreAuthorize("@ss.hasPermi('learning:chapter:edit')")
    public AjaxResult updateChapter(@PathVariable Long id, @Valid @RequestBody ChapterInput b) {
        service.updateChapter(id, b);
        return AjaxResult.success();
    }

    @DeleteMapping("/chapters/{id}")
    @PreAuthorize("@ss.hasPermi('learning:chapter:edit')")
    public AjaxResult deleteChapter(@PathVariable Long id) {
        service.deleteChapter(id);
        return AjaxResult.success();
    }

    @GetMapping("/chapters/{id}/resources")
    @PreAuthorize("@ss.hasPermi('learning:resource:query')")
    public AjaxResult resources(@PathVariable Long id) {
        return AjaxResult.success(service.resources(id));
    }

    @GetMapping("/resources/{id}")
    @PreAuthorize("@ss.hasPermi('learning:resource:query')")
    public AjaxResult resource(@PathVariable Long id) {
        return AjaxResult.success(service.resource(id));
    }

    @PostMapping(value = "/chapters/{id}/resources", consumes = "multipart/form-data")
    @PreAuthorize("@ss.hasPermi('learning:resource:upload')")
    public AjaxResult upload(
            @PathVariable Long id,
            @RequestParam MultipartFile file,
            @RequestParam String kind,
            @Valid @ModelAttribute ResourceInput metadata) {
        return AjaxResult.success(service.upload(id, file, kind, metadata));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("@ss.hasPermi('learning:resource:upload')")
    public AjaxResult updateResource(@PathVariable Long id, @Valid @RequestBody ResourceInput b) {
        service.updateResource(id, b);
        return AjaxResult.success();
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("@ss.hasPermi('learning:resource:upload')")
    public AjaxResult deleteResource(@PathVariable Long id) {
        service.deleteResource(id);
        return AjaxResult.success();
    }

    @GetMapping("/courses/{id}/posts")
    @PreAuthorize("@ss.hasPermi('learning:post:query')")
    public AjaxResult posts(@PathVariable Long id) {
        return AjaxResult.success(service.posts(id));
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("@ss.hasPermi('learning:post:query')")
    public AjaxResult post(@PathVariable Long id) {
        return AjaxResult.success(service.post(id));
    }

    @PostMapping("/courses/{id}/posts")
    @PreAuthorize("@ss.hasPermi('learning:post:add')")
    public AjaxResult createPost(@PathVariable Long id, @Valid @RequestBody PostInput b) {
        return AjaxResult.success(service.createPost(id, b));
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("@ss.hasPermi('learning:post:edit')")
    public AjaxResult updatePost(@PathVariable Long id, @Valid @RequestBody PostInput b) {
        service.updatePost(id, b);
        return AjaxResult.success();
    }

    @PutMapping("/posts/{id}/offline")
    @PreAuthorize("@ss.hasPermi('learning:post:offline')")
    public AjaxResult offline(@PathVariable Long id) {
        service.offlinePost(id);
        return AjaxResult.success();
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("@ss.hasPermi('learning:post:edit')")
    public AjaxResult deletePost(@PathVariable Long id) {
        service.deletePost(id);
        return AjaxResult.success();
    }
}
