package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upself.biz.mood.domain.*;
import com.upself.biz.mood.dto.MoodBodies.*;
import com.upself.biz.mood.dto.MoodViews.*;
import com.upself.biz.mood.mapper.MoodMapper;
import com.upself.biz.mood.service.MoodCalendar;
import com.upself.biz.mood.service.impl.MoodServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class MoodAccessTest {
  MoodMapper mapper;
  MoodServiceImpl service;
  SysAuditMapper audit;

  @BeforeEach
  void setup() {
    mapper = mock(MoodMapper.class);
    audit = mock(SysAuditMapper.class);
    service = new MoodServiceImpl();
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "audit", audit);
    ReflectionTestUtils.setField(
        service,
        "calendar",
        new MoodCalendar(Clock.fixed(Instant.parse("2026-10-22T08:00:00Z"), ZoneOffset.UTC)));
    when(mapper.lockStudent(2L)).thenReturn(2L);
    when(mapper.viewers(2L)).thenReturn(Collections.emptyList());
    role("student", 2L);
  }

  void role(String role, long id) {
    LoginUser user = new LoginUser();
    user.setUserId(id);
    user.setRoleKey(role);
    user.setChannel(role.equals("student") ? "student" : "admin");
    user.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  EntryInput input() {
    EntryInput b = new EntryInput();
    b.setWeekStart("2026-10-19");
    b.setMoodLevel(3);
    b.setPrivateNote("Private note");
    return b;
  }

  void acknowledged() {
    when(mapper.noticeVersion(2L)).thenReturn(MoodServiceImpl.NOTICE_VERSION);
  }

  @Test
  void beforeConfirmationNoMoodRecordIsLoaded() {
    Current c = service.current();
    assertFalse(c.isAcknowledged());
    assertNull(c.getEntry());
    verify(mapper, never()).entry(anyLong(), anyString());
  }

  @Test
  void directRecordCannotBypassNotice() {
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.record(input())).getCode());
    verify(mapper, never()).insert(any());
  }

  @Test
  void oldNoticeVersionDoesNotAuthorizeRecording() {
    when(mapper.noticeVersion(2L)).thenReturn("old");
    assertThrows(ServiceException.class, () -> service.record(input()));
  }

  @Test
  void newDiaryIsSharedOnlyAfterCurrentNotice() {
    acknowledged();
    when(mapper.insert(any())).thenAnswer(invocation -> { ((MoodEntry) invocation.getArgument(0)).setId(10L); return 1; });
    service.record(input());
    verify(mapper).insert(argThat(entry -> entry.isNoteShared() && "Private note".equals(entry.getPrivateNote())));
  }

  @Test
  void mentorDiaryProjectionDoesNotContainOwnerOnlyField() throws Exception {
    MentorMoodEntry shared = new MentorMoodEntry();
    shared.setDiaryNote("A shared diary");
    String json = new ObjectMapper().writeValueAsString(shared);
    assertTrue(json.contains("diaryNote"));
    assertFalse(json.contains("privateNote"));
    assertFalse(new MoodEntry().isNoteShared());
  }

  @Test
  void diaryAudioUsesOwnerAndCurrentMentorScope() {
    MoodEntry entry = new MoodEntry(); entry.setStudentId(2L); entry.setRecordingKey("recording.wav");
    when(mapper.entryById(10L)).thenReturn(entry);
    assertSame(entry, service.readRecording(10L));
    role("student", 3L); assertThrows(ServiceException.class, () -> service.readRecording(10L));
    role("mentor", 5L); assertThrows(ServiceException.class, () -> service.readRecording(10L));
    when(mapper.canView(5L, 2L)).thenReturn(1); assertSame(entry, service.readRecording(10L));
    for (String key : new String[] {"admin", "platform_admin"}) {
      role(key, 1L); assertThrows(ServiceException.class, () -> service.readRecording(10L));
    }
  }

  @Test
  void wrongWeekCannotBeBackfilled() {
    acknowledged();
    EntryInput b = input();
    b.setWeekStart("2026-10-12");
    assertEquals(409, assertThrows(ServiceException.class, () -> service.record(b)).getCode());
    verify(mapper, never()).insert(any());
  }

  @Test
  void savedWeekCannotBeOverwritten() {
    acknowledged();
    when(mapper.entry(2L, "2026-10-19")).thenReturn(new MoodEntry());
    assertEquals(
        409, assertThrows(ServiceException.class, () -> service.record(input())).getCode());
    verify(mapper, never()).insert(any());
  }

  @Test
  void roleChangedDuringRequestCannotWrite() {
    when(mapper.lockStudent(2L)).thenReturn(null);
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.record(input())).getCode());
  }

  @Test
  void adminAndMaintenanceWildcardDoesNotReadPrivateOrPublicMoods() {
    for (String role : new String[] {"platform_admin", "admin"}) {
      role(role, 1L);
      assertThrows(ServiceException.class, () -> service.current());
      assertThrows(ServiceException.class, () -> service.students(""));
      assertThrows(ServiceException.class, () -> service.mentorTrend(2L, 12));
    }
  }

  @Test
  void mentorCannotCallOwnerApi() {
    role("mentor", 5L);
    assertThrows(ServiceException.class, () -> service.current());
    assertThrows(ServiceException.class, () -> service.record(input()));
  }

  @Test
  void studentCannotListOtherStudents() {
    assertThrows(ServiceException.class, () -> service.students(""));
  }

  @Test
  void formerOrUnrelatedMentorCannotViewHistory() {
    role("mentor", 5L);
    when(mapper.canView(5L, 2L)).thenReturn(0);
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.mentorTrend(2L, 12)).getCode());
    verify(mapper, never()).mentorPoints(any(), any(), any(), any(), any());
  }

  @Test
  void publicProjectionHasNoPrivateNoteProperty() throws Exception {
    String json = new ObjectMapper().writeValueAsString(new MoodPoint("2026-10-19", 3));
    assertFalse(json.contains("privateNote"));
    assertFalse(json.contains("studentId"));
  }

  @Test
  void missingWeeksRemainNull() {
    acknowledged();
    when(mapper.ownPoints(2L, MoodServiceImpl.NOTICE_VERSION, "2026-09-28", "2026-10-19"))
        .thenReturn(Arrays.asList(new MoodPoint("2026-09-28", 2), new MoodPoint("2026-10-19", 4)));
    Trend result = service.ownTrend(4);
    assertEquals(4, result.getPoints().size());
    assertNull(result.getPoints().get(1).getMoodLevel());
    assertNull(result.getPoints().get(2).getMoodLevel());
    assertEquals(4, result.getPoints().get(3).getMoodLevel());
  }

  @Test
  void trendRangeIsBounded() {
    acknowledged();
    assertThrows(ServiceException.class, () -> service.ownTrend(53));
    assertThrows(ServiceException.class, () -> service.ownTrend(3));
  }

  @Test
  void confirmationIsIdempotent() {
    acknowledged();
    Acknowledgment b = new Acknowledgment();
    b.setAccepted(true);
    b.setNoticeVersion(MoodServiceImpl.NOTICE_VERSION);
    assertTrue(service.acknowledge(b).isAcknowledged());
    verify(mapper, never()).acceptNotice(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void confirmationRequiresAffirmativeCurrentNotice() {
    Acknowledgment b = new Acknowledgment();
    b.setAccepted(false);
    b.setNoticeVersion(MoodServiceImpl.NOTICE_VERSION);
    assertThrows(ServiceException.class, () -> service.acknowledge(b));
    b.setAccepted(true);
    b.setNoticeVersion("old");
    assertThrows(ServiceException.class, () -> service.acknowledge(b));
  }
}
