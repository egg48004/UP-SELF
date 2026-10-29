package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.IChatService;
import com.upself.biz.followup.domain.FollowUpReminder;
import com.upself.biz.followup.mapper.FollowUpMapper;
import com.upself.biz.followup.service.FollowUpClock;
import com.upself.biz.followup.service.impl.FollowUpServiceImpl;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.report.domain.UserReport;
import com.upself.biz.report.dto.ReportBodies;
import com.upself.biz.report.mapper.ReportMapper;
import com.upself.biz.report.service.impl.ReportServiceImpl;
import com.upself.biz.reporting.mapper.ExportMapper;
import com.upself.biz.reporting.service.CsvWriter;
import com.upself.biz.reporting.service.impl.ExportServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SupportFeaturesTest {
  ReportMapper reports;
  ChatMapper chat;
  IChatService access;
  LearningMapper learning;
  ReportServiceImpl reporting;
  FollowUpMapper reminders;
  FollowUpServiceImpl followups;
  ExportMapper exports;
  ExportServiceImpl exporting;
  SysAuditMapper audit;

  @BeforeEach
  void setup() {
    reports = mock(ReportMapper.class);
    chat = mock(ChatMapper.class);
    access = mock(IChatService.class);
    learning = mock(LearningMapper.class);
    audit = mock(SysAuditMapper.class);
    reporting = new ReportServiceImpl();
    field(reporting, "mapper", reports);
    field(reporting, "chat", chat);
    field(reporting, "access", access);
    field(reporting, "learning", learning);
    field(reporting, "audit", audit);
    reminders = mock(FollowUpMapper.class);
    followups = new FollowUpServiceImpl();
    FollowUpClock clock = mock(FollowUpClock.class);
    when(clock.now()).thenReturn(LocalDateTime.of(2026, 10, 26, 0, 0));
    field(followups, "mapper", reminders);
    field(followups, "clock", clock);
    field(followups, "audit", audit);
    when(reminders.lockMentor(2L)).thenReturn(2L);
    exports = mock(ExportMapper.class);
    exporting = new ExportServiceImpl();
    field(exporting, "mapper", exports);
    field(exporting, "audit", audit);
    role("mentor");
  }

  void field(Object target, String name, Object value) {
    ReflectionTestUtils.setField(target, name, value);
  }

  void role(String r) {
    LoginUser u = new LoginUser();
    u.setUserId(2L);
    u.setRoleKey(r);
    u.setChannel("student".equals(r) ? "student" : "admin");
    u.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void csvQuotesCommaQuotesAndNewlines() {
    assertEquals("\"a,\"\"b\"\"\nc\"", CsvWriter.cell("a,\"b\"\nc"));
  }

  @Test
  void csvProtectsFormulaCellsIncludingLeadingWhitespace() {
    for (String text : Arrays.asList("=SUM(1,2)", " +cmd", "\t@cmd", "\r-cmd", "\uFEFF=cmd"))
      assertTrue(CsvWriter.cell(text).startsWith("\"'"), text);
  }

  @Test
  void csvPreservesUnicodeAndUsesUtf8Bom() {
    byte[] bytes =
        CsvWriter.write(Arrays.asList("Name"), Arrays.asList(Arrays.<Object>asList("Aisha 学生")));
    assertEquals((byte) 0xEF, bytes[0]);
    assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("学生"));
  }

  @Test
  void onlyPublicAdministratorCanExport() {
    for (String r : new String[] {"student", "mentor", "admin"}) {
      role(r);
      assertEquals(
          403,
          assertThrows(
                  ServiceException.class, () -> exporting.export("users", null, null, null, null))
              .getCode());
    }
    verifyNoInteractions(exports);
  }

  @Test
  void exportDoesNotSerializeExtraMapFields() {
    role("platform_admin");
    Map<String, Object> row = new HashMap<>();
    row.put("username", "alice");
    row.put("password", "SECRET-PASSWORD");
    row.put("privateNote", "SECRET-NOTE");
    when(exports.users(null, null, null)).thenReturn(Arrays.asList(row));
    String csv =
        new String(exporting.export("users", null, null, null, null), StandardCharsets.UTF_8);
    assertTrue(csv.contains("alice"));
    assertFalse(csv.contains("SECRET"));
    verify(audit).insert(2L, "EXPORT_USERS", "rows=1", "SUCCESS");
  }

  @Test
  void exportRejectsUnsupportedFilters() {
    role("platform_admin");
    assertThrows(
        ServiceException.class, () -> exporting.export("users", null, "admin", null, null));
    assertThrows(
        ServiceException.class, () -> exporting.export("courses", null, null, "SECRET", null));
    assertThrows(ServiceException.class, () -> exporting.export("sql", null, null, null, null));
  }

  @Test
  void learningExportHasZeroSafePercentAndNoNegativeRemaining() {
    role("platform_admin");
    Map<String, Object> row = new HashMap<>();
    row.put("total", 0L);
    row.put("assignments", 1L);
    row.put("graded", 2L);
    when(exports.learning(null, null)).thenReturn(Arrays.asList(row));
    exporting.export("learning", null, null, null, null);
    assertEquals("0.00", row.get("percent"));
    assertEquals(0L, row.get("remaining"));
  }

  @Test
  void nonMentorsCannotAccessFollowUpsDespiteWildcard() {
    for (String r : new String[] {"student", "platform_admin", "admin"}) {
      role(r);
      assertThrows(ServiceException.class, () -> followups.summary());
      assertThrows(ServiceException.class, () -> followups.review(1L, 0));
    }
    verifyNoInteractions(reminders);
  }

  FollowUpReminder candidate() {
    FollowUpReminder r = new FollowUpReminder();
    r.setMentorId(2L);
    r.setStudentId(3L);
    r.setKind("MOOD");
    r.setScopeKey(0L);
    return r;
  }

  @Test
  void mondayCalendarUsesMalaysiaWeekAndExactThresholds() {
    followups.refreshCurrent();
    verify(reminders).moods(2L, "2026-10-26", "2026-10-19", "2026-10-27-audio-v3");
    verify(reminders).activity(2L, "2026-10-12 00:00:00.000000");
    verify(reminders).overdue(2L, "2026-10-23 00:00:00.000000");
  }

  @Test
  void continuingReviewedConditionIsNotRecreated() {
    FollowUpReminder c = candidate(), old = candidate();
    old.setId(8L);
    old.setActive(true);
    old.setReviewed(true);
    when(reminders.moods(anyLong(), anyString(), anyString(), anyString()))
        .thenReturn(Arrays.asList(c));
    when(reminders.existing(c)).thenReturn(old);
    followups.refreshCurrent();
    verify(reminders, never()).insert(any());
    verify(reminders, never()).reactivate(any());
    verify(reminders).endMissing(2L, Arrays.asList(8L));
  }

  @Test
  void newEpisodeReactivatesExistingReminder() {
    FollowUpReminder c = candidate(), old = candidate();
    old.setId(8L);
    old.setActive(false);
    old.setReviewed(true);
    when(reminders.moods(anyLong(), anyString(), anyString(), anyString()))
        .thenReturn(Arrays.asList(c));
    when(reminders.existing(c)).thenReturn(old);
    followups.refreshCurrent();
    verify(reminders).reactivate(c);
    verify(reminders, never()).insert(any());
  }

  @Test
  void clearedConditionsAreEnded() {
    followups.refreshCurrent();
    verify(reminders).endMissing(2L, Collections.emptyList());
  }

  @Test
  void foreignReminderIsUnavailable() {
    assertEquals(
        404, assertThrows(ServiceException.class, () -> followups.review(9L, 0)).getCode());
  }

  @Test
  void staleReminderIsNotReviewed() {
    FollowUpReminder r = candidate();
    r.setActive(true);
    when(reminders.own(2L, 9L)).thenReturn(r);
    assertEquals(
        409, assertThrows(ServiceException.class, () -> followups.review(9L, 0)).getCode());
  }

  @Test
  void reportReviewDeniedToNonAdministrators() {
    for (String r : new String[] {"student", "mentor", "admin"}) {
      role(r);
      assertThrows(ServiceException.class, () -> reporting.detail(1L));
    }
    verifyNoInteractions(reports);
  }

  @Test
  void onlyStudentsAndMentorsCanSubmitReports() {
    for (String r : new String[] {"platform_admin", "admin"}) {
      role(r);
      assertThrows(ServiceException.class, () -> reporting.create(new ReportBodies.Create()));
    }
    verifyNoInteractions(chat);
  }

  void message(Long sender) {
    ChatMessage m = new ChatMessage();
    m.setId(5L);
    m.setSenderId(sender);
    m.setGroupId(6L);
    m.setBody("Server evidence");
    when(chat.message(5L)).thenReturn(m);
    ChatGroup g = new ChatGroup();
    g.setCourseId(7L);
    g.setTitle("Group");
    when(access.detail(6L)).thenReturn(g);
    Course c = new Course();
    c.setTitle("Course");
    when(learning.course(7L)).thenReturn(c);
    when(chat.messageFiles(5L)).thenReturn(Collections.emptyList());
  }

  ReportBodies.Create body() {
    ReportBodies.Create b = new ReportBodies.Create();
    b.setMessageId(5L);
    b.setReason("OTHER");
    return b;
  }

  @Test
  void cannotReportOwnMessage() {
    message(2L);
    assertEquals(
        400, assertThrows(ServiceException.class, () -> reporting.create(body())).getCode());
    verify(reports, never()).insert(any());
  }

  @Test
  void evidenceRequiresCurrentGroupAccess() {
    message(3L);
    when(access.detail(6L)).thenThrow(new ServiceException(403, "No access"));
    assertThrows(ServiceException.class, () -> reporting.create(body()));
    verify(reports, never()).insert(any());
  }

  @Test
  void evidenceComesFromServerMessage() {
    message(3L);
    when(reports.insert(any()))
        .thenAnswer(
            i -> {
              ((UserReport) i.getArgument(0)).setId(9L);
              return 1;
            });
    reporting.create(body());
    verify(reports)
        .insert(
            argThat(
                r ->
                    "Server evidence".equals(r.getMessageBody())
                        && r.getAccusedId() == 3L
                        && r.getReporterId() == 2L));
  }

  @Test
  void duplicatePendingReportIsRejected() {
    message(3L);
    when(reports.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
    assertEquals(
        409, assertThrows(ServiceException.class, () -> reporting.create(body())).getCode());
  }

  @Test
  void alreadyProcessedReportCannotBeOverwritten() {
    role("platform_admin");
    when(reports.detail(8L)).thenReturn(new UserReport());
    ReportBodies.Review b = new ReportBodies.Review();
    b.setStatus("RESOLVED");
    b.setResolution("Reviewed");
    b.setVersion(0);
    assertEquals(
        409, assertThrows(ServiceException.class, () -> reporting.review(8L, b)).getCode());
  }
}
