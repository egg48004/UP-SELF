package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upself.biz.selfefficacy.domain.SelfEfficacyEntry;
import com.upself.biz.selfefficacy.dto.SelfEfficacyBodies.*;
import com.upself.biz.selfefficacy.dto.SelfEfficacyViews.*;
import com.upself.biz.selfefficacy.mapper.SelfEfficacyMapper;
import com.upself.biz.selfefficacy.service.*;
import com.upself.biz.selfefficacy.service.impl.SelfEfficacyServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SelfEfficacyAccessTest {
    SelfEfficacyMapper mapper;
    SelfEfficacyServiceImpl service;
    AtomicReference<SelfEfficacyEntry> latest;

    @BeforeEach void setup() {
        mapper = mock(SelfEfficacyMapper.class); service = new SelfEfficacyServiceImpl(); latest = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "audit", mock(SysAuditMapper.class));
        ReflectionTestUtils.setField(service, "calendar", new SelfEfficacyCalendar(Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC)));
        when(mapper.lockStudent(2L)).thenReturn(2L);
        when(mapper.viewers(2L)).thenReturn(Collections.emptyList());
        when(mapper.latest(2L)).thenAnswer(i -> latest.get());
        doAnswer(i -> { SelfEfficacyEntry entry = i.getArgument(0); entry.setId(99L); latest.set(entry); return null; }).when(mapper).insert(any());
        role("student");
    }
    void role(String key) {
        LoginUser user = new LoginUser(); user.setUserId(2L); user.setRoleKey(key); user.setChannel(key.equals("student") ? "student" : "admin"); user.getPermissions().add("*:*:*");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void acknowledge() { when(mapper.noticeVersion(2L)).thenReturn(ISelfEfficacyService.NOTICE_VERSION); }
    EntryInput input() {
        EntryInput b = new EntryInput(); b.setAssessmentDate("2026-10-23"); b.setInstrumentVersion(SelfEfficacyScale.VERSION); b.setAnswers(Arrays.asList(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("2"))); return b;
    }
    void previous(String date) { SelfEfficacyEntry entry = new SelfEfficacyEntry(); entry.setAssessmentDate(date); latest.set(entry); }
    @Test void scoringPreservesUnweightedMeanAndEndpoints() {
        assertEquals(new BigDecimal("1.67"), SelfEfficacyScale.score(Arrays.asList(1, 2, 2)));
        assertEquals(new BigDecimal("1.00"), SelfEfficacyScale.score(Arrays.asList(1, 1, 1)));
        assertEquals(new BigDecimal("5.00"), SelfEfficacyScale.score(Arrays.asList(5, 5, 5)));
    }
    @Test void incompleteOrOutOfRangeAnswersNeverHaveAScore() {
        for (List<Integer> answers : Arrays.asList(Arrays.asList(1, 2), Arrays.asList(1, null, 2), Arrays.asList(1, 0, 2), Arrays.asList(1, 6, 2))) assertThrows(ServiceException.class, () -> SelfEfficacyScale.score(answers));
        assertThrows(ServiceException.class, () -> SelfEfficacyScale.score(null));
        assertThrows(ServiceException.class, () -> SelfEfficacyScale.score(Arrays.asList(1, 2.5, 3)));
        assertThrows(ServiceException.class, () -> SelfEfficacyScale.score(Arrays.asList(BigDecimal.ONE, new BigDecimal("2.00000000000000000001"), BigDecimal.ONE)));
    }
    @Test void calendarUsesMalaysianMidnight() {
        assertEquals("2026-10-24", new SelfEfficacyCalendar(Clock.fixed(Instant.parse("2026-10-23T16:00:00Z"), ZoneOffset.UTC)).today().toString());
        assertEquals("2026-10-23", new SelfEfficacyCalendar(Clock.fixed(Instant.parse("2026-10-23T15:59:59Z"), ZoneOffset.UTC)).today().toString());
    }
    @Test void moodOrOldConsentCannotAuthorizeEfficacy() {
        when(mapper.noticeVersion(2L)).thenReturn("old");
        assertFalse(service.current().isAcknowledged());
        assertNull(service.current().getLatest());
        assertThrows(ServiceException.class, () -> service.record(input()));
        assertThrows(ServiceException.class, () -> service.verifyStudent());
        verify(mapper, never()).insert(any());
    }
    @Test void onlyAffirmativeCurrentConsentIsAccepted() {
        Acknowledgment b = new Acknowledgment(); b.setNoticeVersion(ISelfEfficacyService.NOTICE_VERSION); b.setAccepted(false);
        assertThrows(ServiceException.class, () -> service.acknowledge(b));
        b.setAccepted(true); b.setNoticeVersion("old"); assertThrows(ServiceException.class, () -> service.acknowledge(b));
    }
    @Test void duplicateConsentDoesNotChangeHistory() {
        acknowledge(); Acknowledgment b = new Acknowledgment(); b.setAccepted(true); b.setNoticeVersion(ISelfEfficacyService.NOTICE_VERSION);
        assertTrue(service.acknowledge(b).isAcknowledged()); verify(mapper, never()).acceptNotice(any(), any());
    }
    @Test void exactlyTwentyEightDaysAllowsRecordingWithServerScore() {
        acknowledge(); previous("2026-09-25");
        assertTrue(service.current().isAvailable());
        Current c = service.record(input());
        assertFalse(c.isAvailable()); assertEquals("2026-11-20", c.getNextAvailableDate());
        assertEquals(new BigDecimal("1.67"), c.getLatest().getScore()); assertEquals(2L, c.getLatest().getStudentId());
        assertEquals(409, assertThrows(ServiceException.class, () -> service.record(input())).getCode());
    }
    @Test void twentySevenDaysIsStillTooEarly() {
        acknowledge(); previous("2026-09-26"); assertFalse(service.current().isAvailable());
        assertEquals(409, assertThrows(ServiceException.class, () -> service.record(input())).getCode());
        verify(mapper, never()).insert(any());
    }
    @Test void oldPageCannotBackdateOrUseAnotherInstrument() {
        acknowledge(); EntryInput b = input(); b.setAssessmentDate("2026-10-22"); assertThrows(ServiceException.class, () -> service.record(b));
        b.setAssessmentDate("2026-10-23"); b.setInstrumentVersion("old"); assertThrows(ServiceException.class, () -> service.record(b));
    }
    @Test void concurrentAccountRoleOrStatusChangesCannotWrite() {
        acknowledge(); when(mapper.lockStudent(2L)).thenReturn(null); assertThrows(ServiceException.class, () -> service.record(input()));
    }
    @Test void managementWildcardCannotReadStudentAssessments() {
        for (String key : new String[] {"mentor", "platform_admin", "admin"}) {
            role(key); assertThrows(ServiceException.class, () -> service.current()); assertThrows(ServiceException.class, () -> service.record(input())); assertThrows(ServiceException.class, () -> service.ownHistory());
            if (!key.equals("mentor")) assertThrows(ServiceException.class, () -> service.mentorTrend(2L, 26));
        }
    }
    @Test void formerMentorOrStudentCannotAccessSharedEndpoint() {
        assertThrows(ServiceException.class, () -> service.mentorHistory(2L)); role("mentor");
        assertThrows(ServiceException.class, () -> service.verifyMentorAccess(2L));
        verify(mapper, never()).mentorHistory(any(), any(), any());
    }
    @Test void mentorProjectionExcludesAnswersAndOwnerIdentifiers() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new Point());
        assertFalse(json.contains("answer")); assertFalse(json.contains("studentId"));
    }
    @Test void unsupportedTrendRangesAreRejected() {
        acknowledge(); assertThrows(ServiceException.class, () -> service.ownTrend(3)); assertThrows(ServiceException.class, () -> service.ownTrend(53));
    }
}
