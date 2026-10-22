package com.upself.web.controller.assignment;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.AssignmentBodies.*;
import com.upself.biz.assignment.service.*;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/assignments", "/client/assignments"})
public class AssignmentController extends BaseController {
  @Autowired private IAssignmentService service;
  @Autowired private AssignmentFileStorage files;

  @GetMapping
  @PreAuthorize("@ss.hasPermi('assignment:read')")
  public TableDataInfo list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long courseId,
      @RequestParam(defaultValue = "false") boolean pending) {
    startPage();
    try {
      return getDataTable(service.list(keyword, courseId, pending));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('assignment:read')")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(service.detail(id));
  }

  @PostMapping
  @PreAuthorize("@ss.hasPermi('assignment:edit')")
  public AjaxResult create(@Valid @RequestBody AssignmentInput body) {
    return AjaxResult.success(service.create(body));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@ss.hasPermi('assignment:edit')")
  public AjaxResult update(@PathVariable Long id, @Valid @RequestBody AssignmentInput body) {
    service.update(id, body);
    return success();
  }

  @GetMapping("/{id}/submission")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult mine(@PathVariable Long id) {
    return AjaxResult.success(service.mine(id));
  }

  @PutMapping("/{id}/submission")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult draft(@PathVariable Long id, @Valid @RequestBody DraftInput body) {
    return AjaxResult.success(service.saveDraft(id, body));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult submit(@PathVariable Long id, @Valid @RequestBody SubmitInput body) {
    return AjaxResult.success(service.submit(id, body));
  }

  @PostMapping(value = "/{id}/files", consumes = "multipart/form-data")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult upload(
      @PathVariable Long id, @RequestParam int version, @RequestParam MultipartFile file) {
    return AjaxResult.success(service.upload(id, version, file));
  }

  @DeleteMapping("/{id}/files/{fileId}")
  @PreAuthorize("@ss.hasPermi('assignment:submit')")
  public AjaxResult remove(
      @PathVariable Long id, @PathVariable Long fileId, @RequestParam int version) {
    return AjaxResult.success(service.removeFile(id, fileId, version));
  }

  @GetMapping("/{id}/submissions")
  @PreAuthorize("@ss.hasPermi('assignment:review')")
  public TableDataInfo submissions(@PathVariable Long id) {
    service.detail(id);
    startPage();
    try {
      return getDataTable(service.submissions(id));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/{id}/submissions/{submissionId}")
  @PreAuthorize("@ss.hasPermi('assignment:review')")
  public AjaxResult reviewDetail(@PathVariable Long id, @PathVariable Long submissionId) {
    return AjaxResult.success(service.reviewDetail(id, submissionId));
  }

  @PostMapping("/{id}/submissions/{submissionId}/review")
  @PreAuthorize("@ss.hasPermi('assignment:review')")
  public AjaxResult review(
      @PathVariable Long id,
      @PathVariable Long submissionId,
      @Valid @RequestBody ReviewInput body) {
    return AjaxResult.success(service.review(id, submissionId, body));
  }

  @GetMapping("/files/{fileId}/content")
  @PreAuthorize("@ss.hasPermi('assignment:read')")
  public ResponseEntity<Resource> content(@PathVariable Long fileId) {
    SubmissionFile f = service.readFile(fileId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(f.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(MediaType.parseMediaType(f.getMimeType()))
        .body(files.open(f.getStorageKey()));
  }
}
