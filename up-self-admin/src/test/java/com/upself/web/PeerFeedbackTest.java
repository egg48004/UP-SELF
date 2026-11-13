package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.PeerBodies;
import com.upself.biz.assignment.mapper.*;
import com.upself.biz.assignment.service.PeerFeedbackService;
import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class PeerFeedbackTest {
  PeerFeedbackService service;
  AssignmentMapper assignments;
  PeerFeedbackMapper mapper;
  LearningMapper courses;
  Assignment a;
  Course c;

  @BeforeEach
  void setup() {
    service = new PeerFeedbackService();
    assignments = mock(AssignmentMapper.class);
    mapper = mock(PeerFeedbackMapper.class);
    courses = mock(LearningMapper.class);
    ReflectionTestUtils.setField(service, "assignments", assignments);
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "courses", courses);
    ReflectionTestUtils.setField(service, "filter", new ChatContentFilter());
    ReflectionTestUtils.setField(service, "audit", mock(SysAuditMapper.class));
    a = new Assignment();
    a.setId(10L);
    a.setCourseId(1L);
    a.setStatus("PUBLISHED");
    a.setPeerFeedback(true);
    c = new Course();
    c.setId(1L);
    c.setStatus("PUBLISHED");
    c.setMentorId(5L);
    when(assignments.assignment(10L)).thenReturn(a);
    when(assignments.lockAssignment(10L)).thenReturn(a);
    when(courses.course(1L)).thenReturn(c);
    when(courses.lockCourse(1L)).thenReturn(c);
    when(courses.enrolled(1L, 2L)).thenReturn(1);
    role("student", 2L);
  }

  void role(String role, Long id) {
    LoginUser u = new LoginUser();
    u.setUserId(id);
    u.setRoleKey(role);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void cannotEnableLegacyWork() {
    when(mapper.attempts(10L)).thenReturn(1);
    assertThrows(ServiceException.class, () -> service.checkEnable(a, true));
  }

  @Test
  void canReopenPreviouslyDisclosedWork() {
    a.setPeerFeedbackConfigured(true);
    when(mapper.attempts(10L)).thenReturn(3);
    assertDoesNotThrow(() -> service.checkEnable(a, true));
  }

  @Test
  void cannotAllocateBeforeOwnSubmission() {
    assertEquals(409, assertThrows(ServiceException.class, () -> service.allocate(10L)).getCode());
    verify(mapper, never()).insert(any());
  }

  @Test
  void closedCannotAllocate() {
    a.setPeerFeedback(false);
    assertThrows(ServiceException.class, () -> service.allocate(10L));
  }

  @Test
  void unenrolledCannotRead() {
    when(courses.enrolled(1L, 2L)).thenReturn(0);
    assertEquals(403, assertThrows(ServiceException.class, () -> service.state(10L)).getCode());
  }

  @Test
  void adminIsNotMentor() {
    role("platform_admin", 3L);
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.mentorAccess(10L)).getCode());
  }

  @Test
  void previousMentorCannotRead() {
    role("mentor", 9L);
    assertEquals(
        403, assertThrows(ServiceException.class, () -> service.mentorAccess(10L)).getCode());
  }

  @Test
  void draftFileNeverShared() {
    SubmissionFile f = new SubmissionFile();
    Submission s = new Submission();
    assertFalse(service.canReadFile(s, f));
    verifyNoInteractions(mapper);
  }

  @Test
  void cannotHideForeignFeedback() {
    role("mentor", 5L);
    PeerFeedback p = new PeerFeedback();
    p.setAssignmentId(11L);
    when(mapper.find(4L)).thenReturn(p);
    assertEquals(
        404,
        assertThrows(ServiceException.class, () -> service.hide(10L, 4L, new PeerBodies.Hide()))
            .getCode());
  }
}
