package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.MarketBodies.*;
import com.upself.biz.learning.mapper.*;
import com.upself.biz.learning.service.impl.MarketTaskServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class MarketTaskTest {
  MarketTaskServiceImpl service;
  MarketTaskMapper mapper;
  LearningMapper courses;
  MarketTask task;
  Course course;

  @BeforeEach
  void setup() {
    service = new MarketTaskServiceImpl();
    mapper = mock(MarketTaskMapper.class);
    courses = mock(LearningMapper.class);
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "courses", courses);
    ReflectionTestUtils.setField(service, "filter", new ChatContentFilter());
    ReflectionTestUtils.setField(service, "audit", mock(SysAuditMapper.class));
    task = new MarketTask();
    task.setId(10L);
    task.setCourseId(1L);
    task.setStatus("PUBLISHED");
    task.setVersion(1);
    course = new Course();
    course.setId(1L);
    course.setMentorId(5L);
    course.setStatus("PUBLISHED");
    when(mapper.find(10L)).thenReturn(task);
    when(mapper.lock(10L)).thenReturn(task);
    when(courses.course(1L)).thenReturn(course);
    when(courses.lockCourse(1L)).thenReturn(course);
    when(courses.enrolled(1L, 2L)).thenReturn(1);
    role("student", 2L);
  }

  void role(String role, Long id) {
    LoginUser u = new LoginUser();
    u.setRoleKey(role);
    u.setUserId(id);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void adminCannotReadTask() {
    role("platform_admin", 3L);
    assertEquals(403, assertThrows(ServiceException.class, () -> service.detail(10L)).getCode());
  }

  @Test
  void formerMentorCannotRead() {
    role("mentor", 6L);
    assertThrows(ServiceException.class, () -> service.detail(10L));
  }

  @Test
  void unjoinedStudentCannotRead() {
    when(courses.enrolled(1L, 2L)).thenReturn(0);
    assertThrows(ServiceException.class, () -> service.detail(10L));
  }

  @Test
  void draftIsHidden() {
    task.setStatus("DRAFT");
    assertEquals(404, assertThrows(ServiceException.class, () -> service.detail(10L)).getCode());
  }

  @Test
  void hidesOutcomesUntilChosen() {
    MarketOption o = new MarketOption();
    o.setOutcome("secret");
    o.setExplanation("secret");
    when(mapper.options(10L)).thenReturn(Arrays.asList(o));
    assertNull(service.detail(10L).getOptions().get(0).getOutcome());
    assertNull(o.getExplanation());
  }

  @Test
  void choicesCannotModifyContentAfterParticipation() {
    role("mentor", 5L);
    when(mapper.attemptCount(10L)).thenReturn(1);
    Content b = new Content();
    b.setCourseId(1L);
    b.setVersion(1);
    assertEquals(409, assertThrows(ServiceException.class, () -> service.update(10L, b)).getCode());
    verify(mapper, never()).deleteOptions(any());
  }

  @Test
  void rejectsCrossStudentHistory() {
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.historyAccess(10L, 3L)).getCode());
  }

  @Test
  void closedTaskCannotTakeNewChoice() {
    task.setStatus("CLOSED");
    Choice b = new Choice();
    b.setRequestKey("request");
    assertEquals(409, assertThrows(ServiceException.class, () -> service.choose(10L, b)).getCode());
    verify(mapper, never()).insertAttempt(any());
  }

  @Test
  void participatedTaskCannotBecomeDraft() {
    role("mentor", 5L);
    when(mapper.attemptCount(10L)).thenReturn(1);
    State b = new State();
    b.setVersion(1);
    b.setStatus("DRAFT");
    assertThrows(ServiceException.class, () -> service.status(10L, b));
  }

  @Test
  void reflectionCannotChangeAnotherAttempt() {
    MarketAttempt a = new MarketAttempt();
    a.setTaskId(10L);
    a.setStudentId(3L);
    when(mapper.attempt(20L)).thenReturn(a);
    assertEquals(
        404,
        assertThrows(ServiceException.class, () -> service.reflection(10L, 20L, new Reflection()))
            .getCode());
  }

  @Test
  void archivedCourseIsReadonly() {
    role("mentor", 5L);
    course.setStatus("ARCHIVED");
    assertEquals(400, assertThrows(ServiceException.class, () -> service.copy(10L)).getCode());
  }
}
