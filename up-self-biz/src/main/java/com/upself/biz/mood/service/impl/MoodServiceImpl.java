package com.upself.biz.mood.service.impl;

import com.upself.biz.mood.domain.*;
import com.upself.biz.mood.dto.MoodBodies.*;
import com.upself.biz.mood.dto.MoodViews.*;
import com.upself.biz.mood.mapper.MoodMapper;
import com.upself.biz.mood.service.*;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class MoodServiceImpl implements IMoodService {
  @Autowired private MoodMapper mapper;
  @Autowired private MoodCalendar calendar;
  @Autowired private SysAuditMapper audit;
  @Autowired private com.upself.biz.recording.service.RecordingStorage recordings;

  private LoginUser actor() {
    return SecurityUtils.getLoginUser();
  }

  private void role(String expected) {
    if (!expected.equals(actor().getRoleKey()))
      throw new ServiceException(403, "This action is not available for your role.");
  }

  private boolean acknowledged() {
    return NOTICE_VERSION.equals(mapper.noticeVersion(actor().getUserId()));
  }

  private void lockStudent() {
    if (mapper.lockStudent(actor().getUserId()) == null)
      throw new ServiceException(403, "This action is only available to an active student.");
  }

  public void verifyStudent() {
    role("student");
    if (!acknowledged())
      throw new ServiceException(
          403, "Review and confirm the sharing notice before using mood check-ins.");
  }

  public Current current() {
    role("student");
    LocalDate start = calendar.weekStart();
    Current c = new Current();
    c.setWeekStart(start.toString());
    c.setWeekEnd(start.plusDays(6).toString());
    c.setNextWeekStart(start.plusWeeks(1).toString());
    c.setTimeZone(MoodCalendar.ZONE.getId());
    c.setNoticeVersion(NOTICE_VERSION);
    c.setAcknowledged(acknowledged());
    c.setViewers(mapper.viewers(actor().getUserId()));
    if (c.isAcknowledged()) c.setEntry(mapper.entry(actor().getUserId(), start.toString()));
    return c;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Current acknowledge(Acknowledgment b) {
    role("student");
    lockStudent();
    if (!Boolean.TRUE.equals(b.getAccepted()) || !NOTICE_VERSION.equals(b.getNoticeVersion()))
      throw new ServiceException(400, "Reload and confirm the current sharing notice.");
    if (!acknowledged()) {
      mapper.acceptNotice(actor().getUserId(), NOTICE_VERSION);
      audit.insert(actor().getUserId(), "MOOD_NOTICE_ACCEPT", NOTICE_VERSION, "SUCCESS");
    }
    return current();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Current record(EntryInput b) {
    return persist(b, null);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Current recordWithAudio(EntryInput b, org.springframework.web.multipart.MultipartFile file) {
    return persist(b, file);
  }

  private Current persist(EntryInput b, org.springframework.web.multipart.MultipartFile file) {
    role("student");
    lockStudent();
    verifyStudent();
    String week = calendar.weekStart().toString();
    if (!week.equals(b.getWeekStart()))
      throw new ServiceException(409, "A new week has started. Reload before recording your mood.");
    if (mapper.entry(actor().getUserId(), week) != null)
      throw new ServiceException(409, "You have already recorded your mood for this week.");
    MoodEntry e = new MoodEntry();
    e.setStudentId(actor().getUserId());
    e.setWeekStart(week);
    e.setMoodLevel(b.getMoodLevel());
    e.setPrivateNote(b.getPrivateNote());
    e.setNoteShared(true);
    if (file != null) {
      com.upself.biz.recording.service.RecordingStorage.Stored stored = recordings.save(file);
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
          new org.springframework.transaction.support.TransactionSynchronization() {
            public void afterCompletion(int status) { if (status != STATUS_COMMITTED) recordings.discard(stored.key); }
          });
      e.setRecordingKey(stored.key); e.setRecordingName(stored.name); e.setRecordingMime(stored.mime); e.setRecordingSize(stored.size);
    }
    mapper.insert(e);
    audit.insert(actor().getUserId(), "MOOD_RECORD", e.getId().toString(), "SUCCESS");
    return current();
  }

  public MoodEntry readRecording(Long id) {
    MoodEntry e = mapper.entryById(id);
    if (e == null || e.getRecordingKey() == null) throw new ServiceException(404, "This recording is unavailable.");
    if ("student".equals(actor().getRoleKey())) {
      if (!actor().getUserId().equals(e.getStudentId())) throw new ServiceException(403, "You can only play your own diary recordings.");
    } else verifyMentorAccess(e.getStudentId());
    return e;
  }

  public List<MoodEntry> ownHistory() {
    role("student");
    return mapper.ownHistory(actor().getUserId(), NOTICE_VERSION);
  }

  public List<MoodPerson> students(String keyword) {
    role("mentor");
    if (keyword != null && keyword.length() > 100)
      throw new ServiceException(400, "Use at most 100 characters for your search.");
    return mapper.students(actor().getUserId(), keyword);
  }

  public MoodPerson student(Long studentId) {
    role("mentor");
    MoodPerson person = mapper.student(actor().getUserId(), studentId);
    if (person == null)
      throw new ServiceException(403, "This student is not in your current published courses.");
    return person;
  }

  public void verifyMentorAccess(Long studentId) {
    role("mentor");
    if (mapper.canView(actor().getUserId(), studentId) == 0)
      throw new ServiceException(
          403, "You can only view mood records for students in your current published courses.");
  }

  public List<MentorMoodEntry> mentorHistory(Long studentId) {
    // Keep the paginated query first; its SQL also checks the current relationship.
    role("mentor");
    return mapper.mentorHistory(actor().getUserId(), studentId, NOTICE_VERSION);
  }

  private int weeks(int weeks) {
    if (weeks < 4 || weeks > 52)
      throw new ServiceException(400, "Choose a range from 4 to 52 weeks.");
    return weeks;
  }

  private Trend trend(LocalDate end, int count, List<MoodPoint> recorded) {
    Map<String, Integer> byWeek = new HashMap<>();
    for (MoodPoint point : recorded) byWeek.put(point.getWeekStart(), point.getMoodLevel());
    LocalDate start = end.minusWeeks(count - 1);
    List<MoodPoint> points = new ArrayList<>();
    for (int n = 0; n < count; n++) {
      String week = start.plusWeeks(n).toString();
      points.add(new MoodPoint(week, byWeek.get(week)));
    }
    Trend t = new Trend();
    t.setStartWeek(start.toString());
    t.setEndWeek(end.toString());
    t.setTimeZone(MoodCalendar.ZONE.getId());
    t.setWeeks(count);
    t.setPoints(points);
    return t;
  }

  public Trend ownTrend(int count) {
    verifyStudent();
    weeks(count);
    LocalDate end = calendar.weekStart();
    return trend(
        end,
        count,
        mapper.ownPoints(
            actor().getUserId(),
            NOTICE_VERSION,
            end.minusWeeks(count - 1).toString(),
            end.toString()));
  }

  public Trend mentorTrend(Long studentId, int count) {
    verifyMentorAccess(studentId);
    weeks(count);
    LocalDate end = calendar.weekStart();
    return trend(
        end,
        count,
        mapper.mentorPoints(
            actor().getUserId(),
            studentId,
            NOTICE_VERSION,
            end.minusWeeks(count - 1).toString(),
            end.toString()));
  }
}
