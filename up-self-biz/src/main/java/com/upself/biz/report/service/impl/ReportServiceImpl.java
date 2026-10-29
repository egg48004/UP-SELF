package com.upself.biz.report.service.impl;

import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.IChatService;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.report.domain.UserReport;
import com.upself.biz.report.dto.ReportBodies.*;
import com.upself.biz.report.mapper.ReportMapper;
import com.upself.biz.report.service.IReportService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements IReportService {
  @Autowired private ReportMapper mapper;
  @Autowired private ChatMapper chat;
  @Autowired private IChatService access;
  @Autowired private LearningMapper learning;
  @Autowired private SysAuditMapper audit;

  public void participant() {
    String r = SecurityUtils.getLoginUser().getRoleKey();
    if (!"student".equals(r) && !"mentor".equals(r))
      throw new ServiceException(403, "Reports can be submitted by students and mentors only.");
  }

  public void administrator() {
    if (!"platform_admin".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Only administrators can review reports.");
  }

  @Transactional
  public Long create(Create b) {
    participant();
    ChatMessage m = chat.message(b.getMessageId());
    if (m == null) throw new ServiceException(404, "This message is unavailable.");
    ChatGroup g = access.detail(m.getGroupId());
    Long actor = SecurityUtils.getUserId();
    if (actor.equals(m.getSenderId()))
      throw new ServiceException(400, "Choose another person's message to report.");
    if (!Arrays.asList("HURTFUL", "SPAM", "INAPPROPRIATE", "OTHER").contains(b.getReason()))
      throw new ServiceException(400, "Choose a listed reason.");
    UserReport r = new UserReport();
    r.setReporterId(actor);
    r.setAccusedId(m.getSenderId());
    r.setMessageId(m.getId());
    r.setReporterName(mapper.name(actor));
    r.setAccusedName(mapper.name(m.getSenderId()));
    r.setCourseTitle(learning.course(g.getCourseId()).getTitle());
    r.setGroupTitle(g.getTitle());
    r.setMessageBody(m.getBody());
    StringBuilder files = new StringBuilder();
    for (ChatFile f : chat.messageFiles(m.getId())) {
      if (files.length() > 0) files.append("\n");
      files
          .append(f.getOriginalName())
          .append(" (")
          .append(f.getMimeType())
          .append(", ")
          .append(f.getSizeBytes())
          .append(" bytes)");
    }
    r.setAttachmentSummary(files.toString());
    r.setReason(b.getReason());
    r.setDetails(b.getDetails() == null ? "" : b.getDetails().trim());
    try {
      mapper.insert(r);
    } catch (DuplicateKeyException e) {
      throw new ServiceException(
          409, "Your report for this message is already pending. Check My reports.");
    }
    audit.insert(actor, "REPORT_SUBMIT", r.getId().toString(), "SUCCESS");
    return r.getId();
  }

  public List<Own> mine() {
    participant();
    return mapper.mine(SecurityUtils.getUserId());
  }

  public List<UserReport> list(String status) {
    administrator();
    if (status != null
        && !status.isEmpty()
        && !Arrays.asList("PENDING", "RESOLVED", "DISMISSED").contains(status))
      throw new ServiceException(400, "Choose a report status.");
    return mapper.list(status);
  }

  public UserReport detail(Long id) {
    administrator();
    UserReport r = mapper.detail(id);
    if (r == null) throw new ServiceException(404, "This report is unavailable.");
    return r;
  }

  @Transactional
  public void review(Long id, Review b) {
    administrator();
    if (!Arrays.asList("RESOLVED", "DISMISSED").contains(b.getStatus()))
      throw new ServiceException(400, "Choose Resolve or Dismiss.");
    String note = b.getResolution() == null ? "" : b.getResolution().trim();
    if (note.isEmpty() || note.length() > 1000)
      throw new ServiceException(400, "Enter a processing note using 1 to 1000 characters.");
    if (mapper.review(id, SecurityUtils.getUserId(), b.getStatus(), note, b.getVersion()) != 1) {
      detail(id);
      throw new ServiceException(
          409, "This report has already changed. Reload it before reviewing.");
    }
    audit.insert(SecurityUtils.getUserId(), "REPORT_REVIEW", id.toString(), b.getStatus());
  }
}
