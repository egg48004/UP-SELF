package com.upself.biz.learning.service.impl;

import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.MarketBodies.*;
import com.upself.biz.learning.mapper.*;
import com.upself.biz.learning.service.IMarketTaskService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class MarketTaskServiceImpl implements IMarketTaskService {
  @Autowired private MarketTaskMapper mapper;
  @Autowired private LearningMapper courses;
  @Autowired private ChatContentFilter filter;
  @Autowired private SysAuditMapper audit;

  private Long user() {
    return SecurityUtils.getLoginUser().getUserId();
  }

  private String role() {
    return SecurityUtils.getLoginUser().getRoleKey();
  }

  private boolean student() {
    return "student".equals(role());
  }

  private void participant() {
    if (!student()) mentor();
  }

  private void mentor() {
    if (!"mentor".equals(role()))
      throw new ServiceException(403, "This action is available to the course mentor only.");
  }

  private void requireStudent() {
    if (!student()) throw new ServiceException(403, "This action is available to students only.");
  }

  private void conflict() {
    throw new ServiceException(409, "This task has changed. Refresh before trying again.");
  }

  private void version(Integer actual, Integer expected) {
    if (!Objects.equals(actual, expected)) conflict();
  }

  private Course course(Long id, boolean write) {
    participant();
    Course c = write ? courses.lockCourse(id) : courses.course(id);
    if (c == null) throw new ServiceException(404, "This course is unavailable.");
    if (student()) {
      if (!"PUBLISHED".equals(c.getStatus()) || courses.enrolled(id, user()) == 0)
        throw new ServiceException(403, "You do not have access to this course.");
    } else if (!user().equals(c.getMentorId()))
      throw new ServiceException(403, "Only the assigned mentor can manage this course.");
    if (write && "ARCHIVED".equals(c.getStatus()))
      throw new ServiceException(400, "This course is archived and read-only.");
    return c;
  }

  private MarketTask task(Long id, boolean write) {
    participant();
    MarketTask t = mapper.find(id);
    if (t == null) throw new ServiceException(404, "This task is unavailable.");
    course(t.getCourseId(), write);
    if (write) t = mapper.lock(id);
    if (student() && "DRAFT".equals(t.getStatus()))
      throw new ServiceException(404, "This task is unavailable.");
    return t;
  }

  public void listAccess(Long courseId) {
    participant();
    if (courseId != null) course(courseId, false);
  }

  public List<MarketTask> list(Long courseId) {
    return mapper.list(courseId, user(), role());
  }

  public MarketTask detail(Long id) {
    MarketTask t = task(id, false);
    t.setLocked(mapper.attemptCount(id) > 0);
    t.setOptions(mapper.options(id));
    if (student()) {
      t.setCreatedBy(null);
      for (MarketOption o : t.getOptions()) {
        o.setOutcome(null);
        o.setExplanation(null);
      }
    }
    return t;
  }

  private void record(String action, Long id) {
    audit.insert(user(), action, id.toString(), "SUCCESS");
  }

  private void content(MarketTask t, Content b) {
    t.setTitle(b.getTitle().trim());
    t.setObjective(b.getObjective().trim());
    t.setSituation(b.getSituation().trim());
    t.setQuestion(b.getQuestion().trim());
    t.setReflectionPrompt(b.getReflectionPrompt().trim());
    Set<String> labels = new HashSet<>();
    for (Option o : b.getOptions())
      if (!labels.add(o.getLabel().trim().toLowerCase(Locale.ROOT)))
        throw new ServiceException(400, "Use different text for each option.");
  }

  private void options(Long id, List<Option> input) {
    int n = 0;
    for (Option b : input) {
      MarketOption o = new MarketOption();
      o.setTaskId(id);
      o.setPosition(n++);
      o.setLabel(b.getLabel().trim());
      o.setOutcome(b.getOutcome().trim());
      o.setExplanation(b.getExplanation().trim());
      mapper.insertOption(o);
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Long create(Content b) {
    mentor();
    course(b.getCourseId(), true);
    MarketTask t = new MarketTask();
    t.setCourseId(b.getCourseId());
    t.setCreatedBy(user());
    content(t, b);
    mapper.insert(t);
    options(t.getId(), b.getOptions());
    record("MARKET_CREATE", t.getId());
    return t.getId();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void update(Long id, Content b) {
    mentor();
    MarketTask t = task(id, true);
    version(t.getVersion(), b.getVersion());
    if (!t.getCourseId().equals(b.getCourseId()))
      throw new ServiceException(400, "A task cannot be moved to another course.");
    if (mapper.attemptCount(id) > 0)
      throw new ServiceException(
          409, "Students have participated. Copy this task to change its content.");
    content(t, b);
    if (mapper.update(t) != 1) conflict();
    mapper.deleteOptions(id);
    options(id, b.getOptions());
    record("MARKET_EDIT", id);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void status(Long id, State b) {
    mentor();
    MarketTask t = task(id, true);
    version(t.getVersion(), b.getVersion());
    if ("DRAFT".equals(b.getStatus()) && mapper.attemptCount(id) > 0)
      throw new ServiceException(
          409, "Keep participated tasks published or closed so students can read their history.");
    t.setStatus(b.getStatus());
    if (mapper.state(t) != 1) conflict();
    record("MARKET_STATUS", id);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Long copy(Long id) {
    mentor();
    MarketTask t = task(id, true);
    List<MarketOption> old = mapper.options(id);
    String title = t.getTitle();
    t.setTitle(title.substring(0, Math.min(143, title.length())) + " (copy)");
    t.setCreatedBy(user());
    mapper.insert(t);
    for (MarketOption o : old) {
      o.setTaskId(t.getId());
      mapper.insertOption(o);
    }
    record("MARKET_COPY", t.getId());
    return t.getId();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public MarketAttempt choose(Long id, Choice b) {
    requireStudent();
    MarketTask t = task(id, true);
    MarketAttempt previous = mapper.byKey(id, user(), b.getRequestKey());
    if (previous != null) return safe(previous);
    if (!"PUBLISHED".equals(t.getStatus()))
      throw new ServiceException(
          409, "This task is closed. You can still read your past attempts.");
    version(t.getVersion(), b.getVersion());
    if (mapper.options(id).stream().noneMatch(o -> o.getId().equals(b.getOptionId())))
      throw new ServiceException(400, "Choose one of this task's responses.");
    MarketAttempt a = new MarketAttempt();
    a.setTaskId(id);
    a.setStudentId(user());
    a.setOptionId(b.getOptionId());
    a.setRequestKey(b.getRequestKey());
    mapper.insertAttempt(a);
    record("MARKET_ATTEMPT", a.getId());
    return safe(mapper.attempt(a.getId()));
  }

  private MarketAttempt safe(MarketAttempt a) {
    if (student()) {
      a.setStudentId(null);
      a.setRequestKey(null);
    }
    return a;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void reflection(Long id, Long attemptId, Reflection b) {
    requireStudent();
    MarketTask t = task(id, true);
    if (!"PUBLISHED".equals(t.getStatus()))
      throw new ServiceException(409, "This task is closed and read-only.");
    MarketAttempt a = mapper.attempt(attemptId);
    if (a == null || !id.equals(a.getTaskId()) || !user().equals(a.getStudentId()))
      throw new ServiceException(404, "This attempt is unavailable.");
    filter.check(b.getReflection(), false);
    a.setReflection(b.getReflection().trim());
    a.setVersion(b.getVersion());
    if (mapper.reflect(a) != 1) conflict();
    record("MARKET_REFLECT", attemptId);
  }

  public Long historyAccess(Long id, Long studentId) {
    task(id, false);
    if (student()) {
      if (studentId != null && !user().equals(studentId))
        throw new ServiceException(403, "You can only view your own attempts.");
      return user();
    }
    if (studentId == null) throw new ServiceException(400, "Choose a student.");
    return studentId;
  }

  public List<MarketAttempt> history(Long id, Long studentId) {
    List<MarketAttempt> rows = mapper.attempts(id, studentId);
    rows.forEach(this::safe);
    return rows;
  }

  public void mentorAccess(Long id) {
    mentor();
    task(id, false);
  }

  public List<Map<String, Object>> participants(Long id) {
    return mapper.participants(id);
  }
}
