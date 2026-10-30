package com.upself.web.controller.chat;

import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.*;
import com.upself.biz.chat.service.*;
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
@RequestMapping({"/chat", "/client/chat"})
@PreAuthorize("@ss.hasPermi('chat:read')")
public class ChatController extends BaseController {
  @Autowired private IChatService service;
  @Autowired private ChatFileStorage files;

  @GetMapping("/courses/{courseId}/groups")
  public TableDataInfo groups(@PathVariable Long courseId) {
    service.verifyCourse(courseId);
    startPage();
    try {
      return getDataTable(service.groups(courseId));
    } finally {
      clearPage();
    }
  }

  @PostMapping("/courses/{courseId}/groups")
  @PreAuthorize("@ss.hasPermi('chat:manage')")
  public AjaxResult create(@PathVariable Long courseId, @Valid @RequestBody GroupInput b) {
    return AjaxResult.success(service.create(courseId, b));
  }

  @GetMapping("/groups/{id}")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(service.detail(id));
  }

  @PutMapping("/groups/{id}")
  @PreAuthorize("@ss.hasPermi('chat:manage')")
  public AjaxResult update(@PathVariable Long id, @Valid @RequestBody GroupInput b) {
    service.update(id, b);
    return success();
  }

  @GetMapping("/groups/{id}/members")
  public AjaxResult members(@PathVariable Long id) {
    return AjaxResult.success(service.members(id));
  }

  @GetMapping("/groups/{id}/candidates")
  @PreAuthorize("@ss.hasPermi('chat:manage')")
  public AjaxResult candidates(@PathVariable Long id) {
    return AjaxResult.success(service.candidates(id));
  }

  @PostMapping("/groups/{id}/members")
  @PreAuthorize("@ss.hasPermi('chat:manage')")
  public AjaxResult add(@PathVariable Long id, @Valid @RequestBody MemberInput b) {
    service.addMember(id, b.getStudentId());
    return success();
  }

  @DeleteMapping("/groups/{id}/members/{studentId}")
  @PreAuthorize("@ss.hasPermi('chat:manage')")
  public AjaxResult remove(@PathVariable Long id, @PathVariable Long studentId) {
    service.removeMember(id, studentId);
    return success();
  }

  @GetMapping("/groups/{id}/sync")
  public AjaxResult sync(@PathVariable Long id, @RequestParam(required = false) Long afterId) {
    return AjaxResult.success(service.sync(id, afterId));
  }

  @PutMapping("/groups/{id}/hidden-peers/{studentId}")
  @PreAuthorize("@ss.hasPermi('chat:hide')")
  public AjaxResult hide(@PathVariable Long id, @PathVariable Long studentId) {
    service.hidePeer(id, studentId);
    return success();
  }

  @DeleteMapping("/groups/{id}/hidden-peers/{studentId}")
  @PreAuthorize("@ss.hasPermi('chat:hide')")
  public AjaxResult show(@PathVariable Long id, @PathVariable Long studentId) {
    service.showPeer(id, studentId);
    return success();
  }

  @GetMapping("/groups/{id}/messages")
  public AjaxResult messages(
      @PathVariable Long id,
      @RequestParam(required = false) Long afterId,
      @RequestParam(required = false) Long beforeId) {
    return AjaxResult.success(service.messages(id, afterId, beforeId));
  }

  @PostMapping("/groups/{id}/messages")
  @PreAuthorize("@ss.hasPermi('chat:send')")
  @com.upself.common.annotation.RateLimiter(
      time = 60,
      count = 60,
      limitType = com.upself.common.enums.LimitType.USER_ID)
  public AjaxResult send(@PathVariable Long id, @Valid @RequestBody SendInput b) {
    return AjaxResult.success(service.send(id, b));
  }

  @GetMapping("/groups/{id}/draft-files")
  public AjaxResult drafts(@PathVariable Long id) {
    return AjaxResult.success(service.drafts(id));
  }

  @PostMapping(value = "/groups/{id}/files", consumes = "multipart/form-data")
  @PreAuthorize("@ss.hasPermi('chat:send')")
  @com.upself.common.annotation.RateLimiter(
      time = 60,
      count = 20,
      limitType = com.upself.common.enums.LimitType.USER_ID)
  public AjaxResult upload(
      @PathVariable Long id,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String altText) {
    return AjaxResult.success(service.upload(id, file, altText));
  }

  @DeleteMapping("/files/{id}")
  @PreAuthorize("@ss.hasPermi('chat:send')")
  public AjaxResult delete(@PathVariable Long id) {
    service.deleteFile(id);
    return success();
  }

  @GetMapping("/files/{id}/content")
  public ResponseEntity<Resource> content(@PathVariable Long id) {
    ChatFile f = service.readFile(id);
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
