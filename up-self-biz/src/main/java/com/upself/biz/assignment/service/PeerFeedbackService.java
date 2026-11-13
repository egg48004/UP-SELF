package com.upself.biz.assignment.service;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.PeerBodies;
import com.upself.biz.assignment.mapper.*;
import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeerFeedbackService {
  @Autowired private AssignmentMapper assignments;
  @Autowired private PeerFeedbackMapper mapper;
  @Autowired private LearningMapper courses;
  @Autowired private ChatContentFilter filter;
  @Autowired private SysAuditMapper audit;

  private Long user() {
    return SecurityUtils.getLoginUser().getUserId();
  }

  private boolean student() {
    return "student".equals(SecurityUtils.getLoginUser().getRoleKey());
  }

  public void role(boolean isStudent) {
    if (!(isStudent ? "student" : "mentor").equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "This action is not available for your role.");
  }

  private Assignment access(Long id, boolean write) {
    Assignment a = assignments.assignment(id);
    if (a == null) throw new ServiceException(404, "This assignment is unavailable.");
    Course c = write ? courses.lockCourse(a.getCourseId()) : courses.course(a.getCourseId());
    if (c == null) throw new ServiceException(404, "This course is unavailable.");
    if (student()) {
      if (!"PUBLISHED".equals(c.getStatus())
          || !"PUBLISHED".equals(a.getStatus())
          || courses.enrolled(c.getId(), user()) == 0)
        throw new ServiceException(403, "You do not have access to this work.");
    } else {
      role(false);
      if (!user().equals(c.getMentorId()))
        throw new ServiceException(403, "Only the assigned mentor can manage this work.");
    }
    if (write) {
      if ("ARCHIVED".equals(c.getStatus()))
        throw new ServiceException(400, "This course is archived and read-only.");
      a = assignments.lockAssignment(id);
    }
    return a;
  }

  public void checkEnable(Assignment a, Boolean enabled) {
    if (Boolean.TRUE.equals(enabled)
        && !Boolean.TRUE.equals(a.getPeerFeedbackConfigured())
        && mapper.attempts(a.getId()) > 0)
      throw new ServiceException(
          409,
          "Peer feedback cannot be enabled after students have submitted. Create a new assignment"
              + " instead.");
  }

  private void enabled(Assignment a) {
    if (!Boolean.TRUE.equals(a.getPeerFeedback()))
      throw new ServiceException(409, "Peer feedback is closed for this assignment.");
  }

  private PeerFeedback safe(PeerFeedback p) {
    if (p == null) return null;
    if (p.getFiles() != null) p.getFiles().forEach(f -> f.setSubmissionId(null));
    p.setReviewerId(null);
    p.setSubmissionId(null);
    p.setReviewerName(null);
    p.setStudentName(null);
    if (Boolean.TRUE.equals(p.getHidden())) {
      p.setStrength(null);
      p.setSuggestion(null);
      p.setHideReason(null);
    }
    return p;
  }

  public Map<String, Object> state(Long id) {
    role(true);
    Assignment a = access(id, false);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("enabled", a.getPeerFeedback());
    PeerFeedback own = mapper.own(id, user());
    if (own != null) {
      Submission s = assignments.submissionById(own.getSubmissionId());
      // Former classmates no longer have their work exposed after leaving the course.
      if (courses.enrolled(a.getCourseId(), s.getStudentId()) > 0) {
        Attempt snapshot =
            assignments.attempts(s.getId()).stream()
                .filter(x -> x.getAttemptNo().equals(own.getAttemptNo()))
                .findFirst()
                .orElse(null);
        if (snapshot != null) {
          own.setAnswerText(snapshot.getAnswerText());
          own.setFiles(assignments.files(s.getId(), own.getAttemptNo()));
          own.setEarlierSubmission(!s.getAttemptNo().equals(own.getAttemptNo()));
        }
      }
    }
    out.put("outgoing", safe(own));
    List<PeerFeedback> received = mapper.received(id, user());
    received.forEach(this::safe);
    out.put("received", received);
    return out;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> allocate(Long id) {
    role(true);
    Assignment a = access(id, true);
    enabled(a);
    Submission mine = assignments.submission(id, user());
    if (mine == null || mine.getAttemptNo() == 0)
      throw new ServiceException(409, "Submit your own work before reviewing a classmate.");
    if (mapper.own(id, user()) == null) {
      Submission s = mapper.candidate(id, user());
      if (s == null) throw new ServiceException(409, "No work is available to review yet.");
      PeerFeedback p = new PeerFeedback();
      p.setAssignmentId(id);
      p.setReviewerId(user());
      p.setSubmissionId(s.getId());
      p.setAttemptNo(s.getAttemptNo());
      mapper.insert(p);
      record("PEER_ALLOCATE", p.getId());
    }
    return state(id);
  }

  private void record(String action, Long id) {
    audit.insert(user(), action, id.toString(), "SUCCESS");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void send(Long id, PeerBodies.Feedback body) {
    role(true);
    Assignment a = access(id, true);
    enabled(a);
    PeerFeedback p = mapper.own(id, user());
    if (p == null) throw new ServiceException(409, "Choose a classmate's work first.");
    Submission target = assignments.submissionById(p.getSubmissionId());
    if (courses.enrolled(a.getCourseId(), target.getStudentId()) == 0)
      throw new ServiceException(403, "This classmate is no longer in the course.");
    filter.check(body.getStrength(), false);
    filter.check(body.getSuggestion(), false);
    p.setStrength(body.getStrength().trim());
    p.setSuggestion(body.getSuggestion().trim());
    p.setVersion(body.getVersion());
    if (mapper.send(p) != 1)
      throw new ServiceException(
          409, "This feedback has changed or has already been sent. Refresh and try again.");
    record("PEER_SEND", p.getId());
  }

  public List<PeerFeedback> managed(Long id) {
    role(false);
    return mapper.managed(id);
  }

  public void mentorAccess(Long id) {
    role(false);
    access(id, false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void hide(Long id, Long feedbackId, PeerBodies.Hide body) {
    role(false);
    access(id, true);
    PeerFeedback p = mapper.find(feedbackId);
    if (p == null || !id.equals(p.getAssignmentId()))
      throw new ServiceException(404, "This feedback is unavailable.");
    p.setVersion(body.getVersion());
    p.setHideReason(body.getReason().trim());
    if (mapper.hide(p) != 1)
      throw new ServiceException(409, "This feedback has changed. Refresh and try again.");
    record("PEER_HIDE", feedbackId);
  }

  public boolean canReadFile(Submission s, SubmissionFile f) {
    if (!student() || f.getAttemptNo() == null) return false;
    Assignment a = access(s.getAssignmentId(), false);
    return courses.enrolled(a.getCourseId(), s.getStudentId()) > 0
        && mapper.fileGrant(a.getId(), user(), s.getId(), f.getAttemptNo()) > 0;
  }
}
