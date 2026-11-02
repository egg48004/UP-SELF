package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.mentorship.domain.EncouragementMessage;
import com.upself.biz.mentorship.dto.EncouragementBodies.*;
import com.upself.biz.mentorship.mapper.EncouragementMapper;
import com.upself.biz.mentorship.service.impl.EncouragementServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class EncouragementTest {
  EncouragementMapper mapper;
  LearningMapper learning;
  ChatContentFilter filter;
  SysAuditMapper audit;
  EncouragementServiceImpl service;

  @BeforeEach
  void setup() {
    mapper = mock(EncouragementMapper.class);
    learning = mock(LearningMapper.class);
    filter = mock(ChatContentFilter.class);
    audit = mock(SysAuditMapper.class);
    service = new EncouragementServiceImpl();
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "learning", learning);
    ReflectionTestUtils.setField(service, "filter", filter);
    ReflectionTestUtils.setField(service, "audit", audit);
    role("mentor", 20L);
    when(mapper.activeActor(20L, 20L)).thenReturn(1);
    when(mapper.witnessCourses(anyLong(), anyList()))
        .thenReturn(new ArrayList<>(Arrays.asList(101L, 100L)));
    Actor mentor = new Actor();
    mentor.setId(20L);
    mentor.setRoleId(20L);
    mentor.setStatus("0");
    when(mapper.lockActors(anyList())).thenReturn(Arrays.asList(mentor));
    when(mapper.eligibleStudents(anyLong(), anyList(), anyList()))
        .thenReturn(Arrays.asList(2L, 3L));
    when(mapper.insert(any()))
        .thenAnswer(
            inv -> {
              ((EncouragementMessage) inv.getArgument(0)).setId(50L);
              return 1;
            });
    when(mapper.insertRecipients(eq(50L), anyList())).thenReturn(2);
  }

  void role(String key, long id) {
    LoginUser u = new LoginUser();
    u.setRoleKey(key);
    u.setUserId(id);
    u.setChannel("student".equals(key) ? "student" : "admin");
    u.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Send input() {
    Send b = new Send();
    b.setBody(" One small step at a time. ");
    b.setStudentIds(Arrays.asList(3L, 2L, 3L));
    b.setClientKey("request-key-123456789");
    return b;
  }

  EncouragementMessage previous() {
    EncouragementMessage m = new EncouragementMessage();
    m.setId(50L);
    m.setBody("One small step at a time.");
    return m;
  }

  @Test
  void studentsAdminsAndMaintenanceCannotSendEvenWithWildcard() {
    for (String key : Arrays.asList("student", "platform_admin", "admin")) {
      role(key, 20L);
      assertEquals(
          403, assertThrows(ServiceException.class, () -> service.send(input())).getCode());
    }
    verify(mapper, never()).insert(any());
  }

  @Test
  void disabledMentorIsDenied() {
    when(mapper.activeActor(20L, 20L)).thenReturn(0);
    assertThrows(ServiceException.class, () -> service.send(input()));
    verify(mapper, never()).witnessCourses(any(), any());
  }

  @Test
  void deduplicatesRecipientsFiltersOnceAndInsertsOneBatch() {
    service.send(input());
    verify(filter, times(1)).check("One small step at a time.", false);
    verify(mapper).insertRecipients(50L, Arrays.asList(2L, 3L));
    verify(audit).insert(20L, "ENCOURAGEMENT_SEND", "50", "SUCCESS");
  }

  @Test
  void locksCoursesBeforeAccountsInStableOrder() {
    service.send(input());
    InOrder order = inOrder(learning, mapper);
    order.verify(mapper).witnessCourses(eq(20L), anyList());
    order.verify(learning).lockCourse(100L);
    order.verify(learning).lockCourse(101L);
    order.verify(mapper).lockActors(Arrays.asList(2L, 3L, 20L));
  }

  @Test
  void mixedIneligibleBatchDoesNotWriteAnyMessage() {
    when(mapper.eligibleStudents(any(), any(), any())).thenReturn(Arrays.asList(2L));
    assertEquals(403, assertThrows(ServiceException.class, () -> service.send(input())).getCode());
    verify(mapper, never()).insert(any());
    verify(mapper, never()).insertRecipients(any(), any());
  }

  @Test
  void relationshipLostBeforeLockRecheckIsDenied() {
    when(mapper.witnessCourses(any(), any())).thenReturn(new ArrayList<>());
    assertThrows(ServiceException.class, () -> service.send(input()));
    verify(mapper, never()).eligibleStudents(any(), any(), any());
    verify(mapper, never()).insert(any());
  }

  @Test
  void aChangedMentorAccountCannotSendAfterWaiting() {
    when(mapper.lockActors(any())).thenReturn(Collections.emptyList());
    assertThrows(ServiceException.class, () -> service.send(input()));
    verify(mapper, never()).insert(any());
  }

  @Test
  void filterRejectionLeavesDatabaseUntouched() {
    doThrow(new ServiceException(422, "Message not sent."))
        .when(filter)
        .check(anyString(), eq(false));
    assertEquals(422, assertThrows(ServiceException.class, () -> service.send(input())).getCode());
    verify(mapper, never()).insert(any());
    verifyNoInteractions(audit);
  }

  @Test
  void exactRetryReturnsExistingDeliveryWithoutResendingOrFiltering() {
    when(mapper.byKey(20L, input().getClientKey())).thenReturn(previous());
    when(mapper.recipientIds(50L)).thenReturn(Arrays.asList(2L, 3L));
    service.send(input());
    verify(mapper, never()).insert(any());
    verifyNoInteractions(filter);
    verify(mapper, never()).witnessCourses(any(), any());
  }

  @Test
  void retryCannotChangeTextOrRecipients() {
    when(mapper.byKey(20L, input().getClientKey())).thenReturn(previous());
    when(mapper.recipientIds(50L)).thenReturn(Arrays.asList(2L, 3L));
    Send changed = input();
    changed.setBody("Different");
    assertEquals(409, assertThrows(ServiceException.class, () -> service.send(changed)).getCode());
    Send second = input();
    second.setStudentIds(Arrays.asList(2L));
    assertEquals(409, assertThrows(ServiceException.class, () -> service.send(second)).getCode());
  }

  @Test
  void retryAfterWaitingDoesNotInsertAnotherCopy() {
    when(mapper.byKey(20L, input().getClientKey())).thenReturn(null, previous());
    when(mapper.recipientIds(50L)).thenReturn(Arrays.asList(2L, 3L));
    service.send(input());
    verify(mapper, never()).insert(any());
  }

  @Test
  void inputBoundsRejectBlankEmptyOrInvalidSelection() {
    Send blank = input();
    blank.setBody(" ");
    assertThrows(ServiceException.class, () -> service.send(blank));
    Send invalid = input();
    invalid.setStudentIds(Arrays.asList(0L));
    assertThrows(ServiceException.class, () -> service.send(invalid));
    Send empty = input();
    empty.setStudentIds(Collections.emptyList());
    assertThrows(ServiceException.class, () -> service.send(empty));
  }

  @Test
  void senderHistoryCannotExposeAnotherMentorsMessage() {
    when(mapper.own(20L, 8L)).thenReturn(null);
    assertEquals(404, assertThrows(ServiceException.class, () -> service.sent(8L)).getCode());
    verify(mapper, never()).recipients(any(), any());
  }

  @Test
  void studentInboxUsesAuthenticatedOwnerAndHasNoRecipientProjection() {
    role("student", 2L);
    service.inbox();
    verify(mapper).inbox(2L);
    assertTrue(
        Arrays.stream(InboxMessage.class.getDeclaredFields())
            .noneMatch(
                f -> f.getName().equals("recipients") || f.getName().equals("recipientCount")));
    assertThrows(ServiceException.class, () -> service.history());
  }

  @Test
  void foreignStudentMessageIsUnavailable() {
    role("student", 2L);
    when(mapper.activeActor(2L, 10L)).thenReturn(1);
    assertEquals(404, assertThrows(ServiceException.class, () -> service.received(50L)).getCode());
    verify(mapper).received(2L, 50L);
  }
}
