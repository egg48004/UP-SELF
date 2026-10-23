package com.upself.biz.selfefficacy.service.impl;

import com.upself.biz.selfefficacy.domain.SelfEfficacyEntry;
import com.upself.biz.selfefficacy.dto.SelfEfficacyBodies.*;
import com.upself.biz.selfefficacy.dto.SelfEfficacyViews.*;
import com.upself.biz.selfefficacy.mapper.SelfEfficacyMapper;
import com.upself.biz.selfefficacy.service.*;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class SelfEfficacyServiceImpl implements ISelfEfficacyService {
    public static final int INTERVAL_DAYS = 28;
    @Autowired private SelfEfficacyMapper mapper;
    @Autowired private SelfEfficacyCalendar calendar;
    @Autowired private SysAuditMapper audit;

    private LoginUser actor() { return SecurityUtils.getLoginUser(); }
    private void role(String expected) {
        if (!expected.equals(actor().getRoleKey())) throw new ServiceException(403, "This action is not available for your role.");
    }
    private boolean acknowledged() { return NOTICE_VERSION.equals(mapper.noticeVersion(actor().getUserId())); }
    private void lockStudent() {
        if (mapper.lockStudent(actor().getUserId()) == null) throw new ServiceException(403, "This action is only available to an active student.");
    }
    public void verifyStudent() {
        role("student");
        if (!acknowledged()) throw new ServiceException(403, "Review and confirm the self-belief sharing notice first.");
    }
    public Current current() {
        role("student");
        LocalDate today = calendar.today();
        Current c = new Current();
        c.setToday(today.toString());
        c.setTimeZone(SelfEfficacyCalendar.ZONE.getId());
        c.setIntervalDays(INTERVAL_DAYS);
        c.setNoticeVersion(NOTICE_VERSION);
        c.setAcknowledged(acknowledged());
        c.setViewers(mapper.viewers(actor().getUserId()));
        c.setInstrument(SelfEfficacyScale.instrument());
        SelfEfficacyEntry latest = c.isAcknowledged() ? mapper.latest(actor().getUserId()) : null;
        c.setLatest(latest);
        LocalDate next = latest == null ? today : LocalDate.parse(latest.getAssessmentDate()).plusDays(INTERVAL_DAYS);
        c.setNextAvailableDate(next.toString());
        c.setAvailable(c.isAcknowledged() && !today.isBefore(next));
        return c;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Current acknowledge(Acknowledgment b) {
        role("student"); lockStudent();
        if (!Boolean.TRUE.equals(b.getAccepted()) || !NOTICE_VERSION.equals(b.getNoticeVersion())) throw new ServiceException(400, "Reload and confirm the current sharing notice.");
        if (!acknowledged()) {
            mapper.acceptNotice(actor().getUserId(), NOTICE_VERSION);
            audit.insert(actor().getUserId(), "EFFICACY_NOTICE_ACCEPT", NOTICE_VERSION, "SUCCESS");
        }
        return current();
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Current record(EntryInput b) {
        role("student"); lockStudent(); verifyStudent();
        LocalDate today = calendar.today();
        if (!today.toString().equals(b.getAssessmentDate()) || !SelfEfficacyScale.VERSION.equals(b.getInstrumentVersion())) throw new ServiceException(409, "The check-in has changed. Refresh before saving.");
        SelfEfficacyEntry previous = mapper.latest(actor().getUserId());
        if (previous != null && today.isBefore(LocalDate.parse(previous.getAssessmentDate()).plusDays(INTERVAL_DAYS))) throw new ServiceException(409, "Your check-in is already saved. Refresh to see when you can record again.");
        SelfEfficacyEntry entry = new SelfEfficacyEntry();
        entry.setScore(SelfEfficacyScale.score(b.getAnswers()));
        entry.setStudentId(actor().getUserId());
        entry.setAssessmentDate(today.toString());
        entry.setInstrumentVersion(SelfEfficacyScale.VERSION);
        entry.setAnswerOne(b.getAnswers().get(0).intValueExact()); entry.setAnswerTwo(b.getAnswers().get(1).intValueExact()); entry.setAnswerThree(b.getAnswers().get(2).intValueExact());
        mapper.insert(entry);
        audit.insert(actor().getUserId(), "EFFICACY_RECORD", entry.getId().toString(), "SUCCESS");
        return current();
    }
    // Controller verifies consent before startPage; the history SQL repeats consent scope.
    public List<SelfEfficacyEntry> ownHistory() { role("student"); return mapper.ownHistory(actor().getUserId(), NOTICE_VERSION); }
    public Person student(Long studentId) {
        role("mentor");
        Person p = mapper.student(actor().getUserId(), studentId);
        if (p == null) throw new ServiceException(403, "This student is not in your current published courses.");
        return p;
    }
    public void verifyMentorAccess(Long studentId) {
        role("mentor");
        if (mapper.canView(actor().getUserId(), studentId) == 0) throw new ServiceException(403, "You can only view check-ins for students in your current published courses.");
    }
    public List<Point> mentorHistory(Long studentId) {
        // No extra SQL between startPage and the history query; its WHERE rechecks access.
        role("mentor");
        return mapper.mentorHistory(actor().getUserId(), studentId, NOTICE_VERSION);
    }
    private Trend trend(int weeks, Long studentId) {
        if (weeks < 4 || weeks > 52) throw new ServiceException(400, "Choose a range from 4 to 52 weeks.");
        LocalDate end = calendar.today(), start = end.minusWeeks(weeks);
        Trend t = new Trend();
        t.setStartDate(start.toString()); t.setEndDate(end.toString()); t.setTimeZone(SelfEfficacyCalendar.ZONE.getId());
        t.setPoints(studentId == null ? mapper.ownPoints(actor().getUserId(), NOTICE_VERSION, start.toString(), end.toString()) : mapper.mentorPoints(actor().getUserId(), studentId, NOTICE_VERSION, start.toString(), end.toString()));
        return t;
    }
    public Trend ownTrend(int weeks) { verifyStudent(); return trend(weeks, null); }
    public Trend mentorTrend(Long studentId, int weeks) { verifyMentorAccess(studentId); return trend(weeks, studentId); }
}
