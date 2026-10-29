package com.upself.web.controller.reporting;

import com.upself.biz.reporting.service.IExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/exports", "/client/exports"})
@PreAuthorize("@ss.hasPermi('exports:read')")
public class ExportController {
  @Autowired private IExportService service;

  @GetMapping("/{type}")
  public ResponseEntity<byte[]> download(
      @PathVariable String type,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long courseId) {
    byte[] csv = service.export(type, keyword, role, status, courseId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header("X-Content-Type-Options", "nosniff")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("up-self-" + type + ".csv").build().toString())
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(csv);
  }
}
