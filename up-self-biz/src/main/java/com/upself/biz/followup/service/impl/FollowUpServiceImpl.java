package com.upself.biz.followup.service.impl;

import com.upself.biz.followup.domain.FollowUpReminder;
import com.upself.biz.followup.mapper.FollowUpMapper;
import com.upself.biz.followup.service.*;
import com.upself.biz.mood.service.IMoodService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpServiceImpl implements IFollowUpService {
  @Autowired private FollowUpMapper mapper;
  @Autowired private FollowUpClock clock;
  @Autowired private SysAuditMapper audit;

  public void mentor() {
    if (!"mentor".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Follow-up reminders are available to current mentors only.");
  }

  @Transactional
  public void refreshCurrent() {
    mentor();
    sync(SecurityUtils.getUserId());
  }

  @Transactional
  public void sync(Long mentorId) {
    if (mapper.lockMentor(mentorId) == null) return;
    LocalDateTime now = clock.now();
    LocalDate week = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<FollowUpReminder> candidates = new ArrayList<>();
    candidates.addAll(
        mapper.moods(
            mentorId, week.toString(), week.minusWeeks(1).toString(), IMoodService.NOTICE_VERSION));
    candidates.addAll(
        mapper.activity(
            mentorId,
            now.minusDays(14)
                .format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))));
    candidates.addAll(
        mapper.overdue(
            mentorId,
            now.minusDays(3)
                .format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))));
    List<Long> ids = new ArrayList<>();
    for (FollowUpReminder candidate : candidates) {
      FollowUpReminder old = mapper.existing(candidate);
      if (old == null) mapper.insert(candidate);
      else {
        candidate.setId(old.getId());
        if (!old.isActive()) mapper.reactivate(candidate);
      }
      ids.add(candidate.getId());
    }
    mapper.endMissing(mentorId, ids);
  }

  public List<FollowUpReminder> list(String status, Long studentId) {
    mentor();
    if (!Arrays.asList("PENDING", "REVIEWED", "ENDED").contains(status))
      throw new ServiceException(400, "Choose a reminder status.");
    return mapper.list(SecurityUtils.getUserId(), status, studentId);
  }

  public Map<String, Object> summary() {
    mentor();
    List<FollowUpReminder> rows = mapper.list(SecurityUtils.getUserId(), "PENDING", null);
    Map<Long, Integer> students = new HashMap<>();
    for (FollowUpReminder r : rows)
      students.put(r.getStudentId(), students.getOrDefault(r.getStudentId(), 0) + 1);
    Map<String, Object> result = new HashMap<>();
    result.put("total", rows.size());
    result.put("byStudent", students);
    return result;
  }

  @Transactional
  public void review(Long id, Integer version) {
    mentor();
    Long actor = SecurityUtils.getUserId();
    mapper.lockMentor(actor);
    FollowUpReminder r = mapper.own(actor, id);
    if (r == null) throw new ServiceException(404, "This reminder is unavailable.");
    if (mapper.review(actor, id, version) != 1) {
      if (r.isActive() && r.isReviewed()) return;
      throw new ServiceException(409, "This reminder has changed. Refresh before reviewing.");
    }
    audit.insert(actor, "FOLLOW_UP_REVIEW", id.toString(), "SUCCESS");
  }
}
