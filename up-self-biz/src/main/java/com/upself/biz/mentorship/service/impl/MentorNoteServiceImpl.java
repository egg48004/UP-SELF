package com.upself.biz.mentorship.service.impl;

import com.upself.biz.mentorship.domain.MentorNote;
import com.upself.biz.mentorship.dto.NoteBodies.*;
import com.upself.biz.mentorship.mapper.MentorNoteMapper;
import com.upself.biz.mentorship.service.IMentorNoteService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MentorNoteServiceImpl implements IMentorNoteService {
  @Autowired private MentorNoteMapper mapper;
  @Autowired private SysAuditMapper audit;

  private Long mentor() {
    if (!"mentor".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Mentor notes are private to the responsible mentor.");
    return SecurityUtils.getUserId();
  }

  public Student verifyStudent(Long studentId) {
    Student student = mapper.student(mentor(), studentId);
    if (student == null)
      throw new ServiceException(403, "This student is not currently assigned to you.");
    return student;
  }

  public List<MentorNote> list(Long studentId) {
    return mapper.list(mentor(), studentId);
  }

  private String content(Create b) {
    String text = b.getContent() == null ? "" : b.getContent().trim();
    if (text.isEmpty() || text.length() > 2000)
      throw new ServiceException(400, "Write a note using 1 to 2000 characters.");
    return text;
  }

  @Transactional
  public Long create(Long studentId, Create b) {
    verifyStudent(studentId);
    MentorNote n = new MentorNote();
    n.setMentorId(mentor());
    n.setStudentId(studentId);
    n.setContent(content(b));
    if (mapper.insert(n) != 1)
      throw new ServiceException(403, "This student is not currently assigned to you.");
    audit.insert(mentor(), "MENTOR_NOTE_CREATE", n.getId().toString(), "SUCCESS");
    return n.getId();
  }

  @Transactional
  public void edit(Long studentId, Long id, Update b) {
    verifyStudent(studentId);
    Long owner = mentor();
    if (mapper.edit(id, owner, studentId, b.getVersion(), content(b)) != 1) {
      if (mapper.own(id, owner, studentId) == null)
        throw new ServiceException(404, "This note is unavailable.");
      throw new ServiceException(409, "This note has changed. Refresh before editing again.");
    }
    audit.insert(owner, "MENTOR_NOTE_EDIT", id.toString(), "SUCCESS");
  }
}
