package com.upself.biz.assignment.service.impl;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.AssignmentBodies.*;
import com.upself.biz.assignment.mapper.AssignmentMapper;
import com.upself.biz.assignment.service.*;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.*;
import com.upself.system.mapper.SysAuditMapper;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssignmentServiceImpl implements IAssignmentService {
  @Autowired private AssignmentMapper mapper;
  @Autowired private LearningMapper courses;
  @Autowired private AssignmentFileStorage storage;
  @Autowired private SysAuditMapper audit;

  private LoginUser actor() {
    return SecurityUtils.getLoginUser();
  }

  private boolean student() {
    return "student".equals(actor().getRoleKey());
  }

  private void requireRole(String role) {
    if (!role.equals(actor().getRoleKey()))
      throw new ServiceException(403, "This action is not available for your role.");
  }

  private void requireParticipant() {
    if (!student()) requireRole("mentor");
  }

  private void conflict() {
    throw new ServiceException(
        409, "This work has changed. Reload the latest version before trying again.");
  }

  private void version(int actual, Integer expected) {
    if (expected == null || actual != expected) conflict();
  }

  private void record(String action, Long id) {
    audit.insert(actor().getUserId(), action, id.toString(), "SUCCESS");
  }

  private Course course(Long id, boolean lock, boolean write) {
    Course c = lock ? courses.lockCourse(id) : courses.course(id);
    if (c == null) throw new ServiceException(404, "This course is unavailable.");
    requireParticipant();
    if (student()) {
      if (!"PUBLISHED".equals(c.getStatus()) || courses.enrolled(id, actor().getUserId()) == 0)
        throw new ServiceException(403, "You do not have access to this course.");
    } else if (!actor().getUserId().equals(c.getMentorId()))
      throw new ServiceException(403, "Only the assigned mentor can manage this work.");
    if (write && "ARCHIVED".equals(c.getStatus()))
      throw new ServiceException(400, "This course is archived and read-only.");
    return c;
  }

  private Assignment assignment(Long id, boolean write) {
    Assignment a = mapper.assignment(id);
    if (a == null) throw new ServiceException(404, "This assignment is unavailable.");
    course(a.getCourseId(), write, write);
    if (write) a = mapper.lockAssignment(id);
    if (student() && !"PUBLISHED".equals(a.getStatus()))
      throw new ServiceException(403, "This assignment is not published.");
    return a;
  }

  public List<Assignment> list(String keyword, Long courseId, boolean pending) {
    requireParticipant();
    return mapper.list(
        actor().getUserId(), actor().getRoleKey(), keyword, courseId, pending && !student());
  }

  public Assignment detail(Long id) {
    return assignment(id, false);
  }

  private void copy(Assignment a, AssignmentInput b) {
    a.setTitle(b.getTitle().trim());
    a.setInstructions(b.getInstructions());
    a.setStatus(b.getStatus());
    a.setLanguageCode(b.getLanguageCode());
    a.setMaxScore(b.getMaxScore());
    try {
      a.setDueAt(
          b.getDueAt() == null || b.getDueAt().trim().isEmpty()
              ? null
              : Date.from(OffsetDateTime.parse(b.getDueAt()).toInstant()));
    } catch (java.time.DateTimeException e) {
      throw new ServiceException(400, "Choose a valid due date with its time zone.");
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Long create(AssignmentInput b) {
    requireRole("mentor");
    course(b.getCourseId(), true, true);
    Assignment a = new Assignment();
    a.setCourseId(b.getCourseId());
    a.setCreatedBy(actor().getUserId());
    copy(a, b);
    mapper.insertAssignment(a);
    record("ASSIGNMENT_CREATE", a.getId());
    return a.getId();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void update(Long id, AssignmentInput b) {
    requireRole("mentor");
    Assignment a = assignment(id, true);
    if (!a.getCourseId().equals(b.getCourseId()))
      throw new ServiceException(400, "An assignment cannot be moved to another course.");
    version(a.getVersion(), b.getVersion());
    copy(a, b);
    if (mapper.updateAssignment(a) != 1) conflict();
    record("ASSIGNMENT_EDIT", id);
  }

  private Submission empty(Long id) {
    Submission s = new Submission();
    s.setAssignmentId(id);
    s.setStudentId(actor().getUserId());
    s.setAnswerText("");
    s.setStatus("DRAFT");
    s.setVersion(0);
    s.setAttemptNo(0);
    s.setFiles(Collections.emptyList());
    s.setAttempts(Collections.emptyList());
    return s;
  }

  private Submission hydrate(Submission s, boolean own) {
    if (s.getId() == null) return s;
    s.setFiles(own ? mapper.files(s.getId(), null) : Collections.emptyList());
    List<Attempt> history = mapper.attempts(s.getId());
    for (Attempt a : history) a.setFiles(mapper.files(s.getId(), a.getAttemptNo()));
    s.setAttempts(history);
    // Unsubmitted text/files remain private even after a previous attempt was returned.
    if (!own) s.setAnswerText(history.isEmpty() ? null : history.get(0).getAnswerText());
    return s;
  }

  public Submission mine(Long id) {
    requireRole("student");
    assignment(id, false);
    Submission s = mapper.submission(id, actor().getUserId());
    return s == null ? empty(id) : hydrate(s, true);
  }

  private Submission draft(Long id) {
    Submission s = mapper.lockSubmission(id, actor().getUserId());
    if (s == null) {
      s = empty(id);
      mapper.createDraft(s);
      s = mapper.lockSubmission(id, actor().getUserId());
    }
    return s;
  }

  private void editable(Submission s) {
    if (!"DRAFT".equals(s.getStatus()) && !"RETURNED".equals(s.getStatus()))
      throw new ServiceException(
          409, "This attempt has already been submitted. Wait for your mentor's feedback.");
  }

  private void save(Submission s) {
    if (mapper.updateSubmission(s) != 1) conflict();
    s.setVersion(s.getVersion() + 1);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Submission saveDraft(Long id, DraftInput b) {
    requireRole("student");
    assignment(id, true);
    Submission s = draft(id);
    editable(s);
    version(s.getVersion(), b.getVersion());
    s.setAnswerText(b.getAnswerText());
    save(s);
    return hydrate(s, true);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Submission submit(Long id, SubmitInput b) {
    requireRole("student");
    Assignment a = assignment(id, true);
    Submission s = draft(id);
    if (mapper.attemptByKey(s.getId(), b.getRequestKey()) != null) return hydrate(s, true);
    editable(s);
    version(a.getVersion(), b.getAssignmentVersion());
    version(s.getVersion(), b.getVersion());
    if (b.getAnswerText().trim().isEmpty() && mapper.files(s.getId(), null).isEmpty())
      throw new ServiceException(400, "Add your answer or at least one file before submitting.");
    int attemptNo = s.getAttemptNo() + 1;
    Date now = new Date();
    Attempt attempt = new Attempt();
    attempt.setSubmissionId(s.getId());
    attempt.setAttemptNo(attemptNo);
    attempt.setAnswerText(b.getAnswerText());
    attempt.setRequestKey(b.getRequestKey());
    attempt.setSubmittedAt(now);
    attempt.setLate(a.getDueAt() != null && now.after(a.getDueAt()));
    attempt.setMaxScore(a.getMaxScore());
    mapper.insertAttempt(attempt);
    mapper.attachFiles(s.getId(), attemptNo);
    s.setAttemptNo(attemptNo);
    s.setAnswerText(b.getAnswerText());
    s.setStatus("SUBMITTED");
    save(s);
    record("ASSIGNMENT_SUBMIT", s.getId());
    return hydrate(s, true);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Submission upload(Long id, int expectedVersion, MultipartFile file) {
    requireRole("student");
    assignment(id, true);
    Submission s = draft(id);
    editable(s);
    version(s.getVersion(), expectedVersion);
    if (mapper.files(s.getId(), null).size() >= 5)
      throw new ServiceException(400, "You can attach up to five files to one attempt.");
    AssignmentFileStorage.Stored stored = storage.save(file);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) storage.discard(stored.key);
          }
        });
    SubmissionFile f = new SubmissionFile();
    f.setSubmissionId(s.getId());
    f.setStorageKey(stored.key);
    f.setOriginalName(stored.name);
    f.setMimeType(stored.mime);
    f.setFileSize(stored.size);
    mapper.insertFile(f);
    save(s);
    return hydrate(s, true);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Submission removeFile(Long id, Long fileId, int expectedVersion) {
    requireRole("student");
    assignment(id, true);
    Submission s = draft(id);
    editable(s);
    version(s.getVersion(), expectedVersion);
    SubmissionFile f = mapper.file(fileId);
    if (f == null || !s.getId().equals(f.getSubmissionId()) || f.getAttemptNo() != null)
      throw new ServiceException(403, "Only your current draft files can be removed.");
    mapper.removeFile(fileId);
    save(s);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            storage.discard(f.getStorageKey());
          }
        });
    return hydrate(s, true);
  }

  public List<Submission> submissions(Long id) {
    requireRole("mentor");
    List<Submission> result = mapper.submissions(id, actor().getUserId());
    for (Submission s : result) s.setAnswerText(null);
    return result;
  }

  private Submission reviewable(Long id, Long submissionId) {
    Submission s = mapper.submissionById(submissionId);
    if (s == null || !id.equals(s.getAssignmentId()) || s.getAttemptNo() == 0)
      throw new ServiceException(404, "This submission is unavailable.");
    return s;
  }

  public Submission reviewDetail(Long id, Long submissionId) {
    requireRole("mentor");
    assignment(id, false);
    return hydrate(reviewable(id, submissionId), false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Submission review(Long id, Long submissionId, ReviewInput b) {
    requireRole("mentor");
    assignment(id, true);
    Submission raw = reviewable(id, submissionId);
    Submission s = mapper.lockSubmission(id, raw.getStudentId());
    version(s.getVersion(), b.getVersion());
    if (!"SUBMITTED".equals(s.getStatus()))
      throw new ServiceException(409, "Only a submitted attempt can be reviewed.");
    Attempt a = mapper.attempts(s.getId()).get(0);
    if ("GRADED".equals(b.getDecision())
        && (b.getScore() == null || b.getScore() > a.getMaxScore()))
      throw new ServiceException(400, "Enter a score within this attempt's maximum score.");
    a.setStatus(b.getDecision());
    a.setScore("GRADED".equals(b.getDecision()) ? b.getScore() : null);
    a.setFeedback(b.getFeedback().trim());
    a.setReviewedBy(actor().getUserId());
    a.setReviewedAt(new Date());
    if (mapper.reviewAttempt(a) != 1) conflict();
    s.setStatus(b.getDecision());
    save(s);
    record("ASSIGNMENT_REVIEW", s.getId());
    return hydrate(s, false);
  }

  public SubmissionFile readFile(Long fileId) {
    requireParticipant();
    SubmissionFile f = mapper.file(fileId);
    if (f == null) throw new ServiceException(404, "This file is unavailable.");
    Submission s = mapper.submissionById(f.getSubmissionId());
    assignment(s.getAssignmentId(), false);
    if (student()) {
      if (!actor().getUserId().equals(s.getStudentId()))
        throw new ServiceException(403, "You can only download your own work.");
    } else if (f.getAttemptNo() == null)
      throw new ServiceException(403, "Draft attachments have not been submitted.");
    return f;
  }
}
