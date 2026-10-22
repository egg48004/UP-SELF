
package com.upself.web.controller.learning;

import com.upself.biz.learning.domain.LearningResource;
import com.upself.biz.learning.service.*;
import com.upself.common.core.domain.AjaxResult;
import com.upself.framework.web.service.LearningMediaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

@RestController
public class LearningMediaController {
    @Autowired private ILearningService learning;
    @Autowired private LearningMediaService media;
    @Autowired private LearningFileStorage storage;

    @PostMapping({"/learning/resources/{id}/access", "/client/learning/resources/{id}/access"})
    @PreAuthorize("@ss.hasPermi('learning:resource:query')")
    public AjaxResult access(@PathVariable Long id) {
        return AjaxResult.success(media.issue(id));
    }

    @GetMapping({"/learning/resources/{id}/content", "/client/learning/resources/{id}/content"})
    @PreAuthorize("@ss.hasPermi('learning:resource:query')")
    public ResponseEntity<Resource> content(@PathVariable Long id) {
        return response(learning.resource(id));
    }

    @GetMapping({"/learning/media/{ticket}", "/client/learning/media/{ticket}"})
    public ResponseEntity<Resource> media(@PathVariable String ticket, HttpServletRequest request) {
        String channel = request.getRequestURI().startsWith("/client/") ? "student" : "admin";
        return response(media.read(ticket, channel));
    }

    private ResponseEntity<Resource> response(LearningResource r) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(r.getOriginalName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(r.getMimeType()))
                .body(storage.open(r.getStorageKey()));
    }
}
