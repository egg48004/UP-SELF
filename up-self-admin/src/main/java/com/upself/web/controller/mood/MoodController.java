package com.upself.web.controller.mood;

import com.upself.biz.mood.dto.MoodBodies.*;
import com.upself.biz.mood.service.IMoodService;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/moods", "/client/moods"})
public class MoodController extends BaseController {
  @Autowired private IMoodService service;
  @Autowired private com.upself.biz.recording.service.RecordingStorage recordings;

  @PostMapping(value="/entries/recording", consumes="multipart/form-data")
  @PreAuthorize("@ss.hasPermi('mood:own:record')")
  public AjaxResult recordAudio(@Valid @ModelAttribute EntryInput body, @RequestParam org.springframework.web.multipart.MultipartFile file) {
    return AjaxResult.success(service.recordWithAudio(body, file));
  }

  @GetMapping("/recordings/{id}/content")
  @PreAuthorize("@ss.hasPermi('mood:own:read') or @ss.hasPermi('mood:students:read')")
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> recording(@PathVariable Long id) {
    com.upself.biz.mood.domain.MoodEntry entry = service.readRecording(id);
    return org.springframework.http.ResponseEntity.ok()
        .header("Cache-Control", "private, no-store").header("X-Content-Type-Options", "nosniff")
        .contentType(org.springframework.http.MediaType.parseMediaType(entry.getRecordingMime()))
        .body(recordings.open(entry.getRecordingKey()));
  }

  @GetMapping("/current")
  @PreAuthorize("@ss.hasPermi('mood:own:read')")
  public AjaxResult current() {
    return AjaxResult.success(service.current());
  }

  @PostMapping("/acknowledgment")
  @PreAuthorize("@ss.hasPermi('mood:own:record')")
  public AjaxResult acknowledge(@Valid @RequestBody Acknowledgment body) {
    return AjaxResult.success(service.acknowledge(body));
  }

  @PostMapping("/entries")
  @PreAuthorize("@ss.hasPermi('mood:own:record')")
  public AjaxResult record(@Valid @RequestBody EntryInput body) {
    return AjaxResult.success(service.record(body));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermi('mood:own:read')")
  public TableDataInfo history() {
    service.verifyStudent();
    startPage();
    try {
      return getDataTable(service.ownHistory());
    } finally {
      clearPage();
    }
  }

  @GetMapping("/trend")
  @PreAuthorize("@ss.hasPermi('mood:own:read')")
  public AjaxResult trend(@RequestParam(defaultValue = "12") int weeks) {
    return AjaxResult.success(service.ownTrend(weeks));
  }

  @GetMapping("/students")
  @PreAuthorize("@ss.hasPermi('mood:students:read')")
  public TableDataInfo students(@RequestParam(required = false) String keyword) {
    startPage();
    try {
      return getDataTable(service.students(keyword));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/students/{studentId}")
  @PreAuthorize("@ss.hasPermi('mood:students:read')")
  public AjaxResult student(@PathVariable Long studentId) {
    return AjaxResult.success(service.student(studentId));
  }

  @GetMapping("/students/{studentId}/history")
  @PreAuthorize("@ss.hasPermi('mood:students:read')")
  public TableDataInfo mentorHistory(@PathVariable Long studentId) {
    service.verifyMentorAccess(studentId);
    startPage();
    try {
      return getDataTable(service.mentorHistory(studentId));
    } finally {
      clearPage();
    }
  }

  @GetMapping("/students/{studentId}/trend")
  @PreAuthorize("@ss.hasPermi('mood:students:read')")
  public AjaxResult mentorTrend(
      @PathVariable Long studentId, @RequestParam(defaultValue = "12") int weeks) {
    return AjaxResult.success(service.mentorTrend(studentId, weeks));
  }
}
