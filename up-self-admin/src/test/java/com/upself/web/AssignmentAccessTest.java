package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.AssignmentBodies.*;
import com.upself.biz.assignment.mapper.AssignmentMapper;
import com.upself.biz.assignment.service.impl.AssignmentServiceImpl;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class AssignmentAccessTest {
  AssignmentServiceImpl service;
  AssignmentMapper mapper;
  LearningMapper courses;
  Course course;
  Assignment a;
  LoginUser actor;
  Submission work;

  @BeforeEach
  void setup() {
    service = new AssignmentServiceImpl();
    mapper = mock(AssignmentMapper.class);
    courses = mock(LearningMapper.class);
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "courses", courses);
    course = new Course();
    course.setId(1L);
    course.setMentorId(5L);
    course.setStatus("PUBLISHED");
    a = new Assignment();
    a.setId(10L);
    a.setCourseId(1L);
    a.setStatus("PUBLISHED");
    a.setVersion(2);
    a.setMaxScore(100);
    when(mapper.assignment(10L)).thenReturn(a);
    when(mapper.lockAssignment(10L)).thenReturn(a);
    when(courses.course(1L)).thenReturn(course);
    when(courses.lockCourse(1L)).thenReturn(course);
    work = new Submission();
    work.setId(20L);
    work.setAssignmentId(10L);
    work.setStudentId(2L);
    work.setAttemptNo(1);
    work.setVersion(3);
    work.setStatus("SUBMITTED");
    work.setAnswerText("private revision");
    when(mapper.submissionById(20L)).thenReturn(work);
    when(mapper.lockSubmission(10L, 2L)).thenReturn(work);
    when(mapper.attempts(20L)).thenReturn(Collections.emptyList());
    role("student", 2L);
  }

  void role(String role, long id) {
    actor = new LoginUser();
    actor.setUserId(id);
    actor.setRoleKey(role);
    actor.setChannel(role.equals("student") ? "student" : "admin");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void unjoinedStudentBlocked() {
    assertEquals(403, assertThrows(ServiceException.class, () -> service.detail(10L)).getCode());
  }

  @Test
  void draftInvisibleToJoinedStudent() {
    when(courses.enrolled(1L, 2L)).thenReturn(1);
    a.setStatus("DRAFT");
    assertThrows(ServiceException.class, () -> service.detail(10L));
  }

  @Test
  void adminCannotReadStudentWork() {
    role("platform_admin", 3L);
    assertThrows(ServiceException.class, () -> service.reviewDetail(10L, 20L));
    role("admin", 1L);
    assertThrows(ServiceException.class, () -> service.detail(10L));
  }

  @Test
  void wrongMentorBlocked() {
    role("mentor", 6L);
    assertThrows(ServiceException.class, () -> service.reviewDetail(10L, 20L));
  }

  @Test
  void mentorCannotReadDraftAttachments() {
    role("mentor", 5L);
    SubmissionFile f = new SubmissionFile();
    f.setSubmissionId(20L);
    when(mapper.file(30L)).thenReturn(f);
    assertThrows(ServiceException.class, () -> service.readFile(30L));
  }

  @Test
  void otherStudentCannotReadAttachments() {
    role("student", 7L);
    when(courses.enrolled(1L, 7L)).thenReturn(1);
    SubmissionFile f = new SubmissionFile();
    f.setSubmissionId(20L);
    f.setAttemptNo(1);
    when(mapper.file(30L)).thenReturn(f);
    assertThrows(ServiceException.class, () -> service.readFile(30L));
  }

  @Test
  void mentorSeesSubmittedSnapshotRatherThanPrivateRevision() {
    role("mentor", 5L);
    Attempt old = new Attempt();
    old.setAttemptNo(1);
    old.setAnswerText("submitted answer");
    when(mapper.attempts(20L)).thenReturn(Collections.singletonList(old));
    Submission s = service.reviewDetail(10L, 20L);
    assertEquals("submitted answer", s.getAnswerText());
    assertTrue(s.getFiles().isEmpty());
  }

  @Test
  void staleReviewCannotOverwriteFeedback() {
    role("mentor", 5L);
    ReviewInput b = new ReviewInput();
    b.setVersion(1);
    assertEquals(
        409, assertThrows(ServiceException.class, () -> service.review(10L, 20L, b)).getCode());
    verify(mapper, never()).reviewAttempt(any());
  }

  @Test
  void scoreUsesAttemptMaximum() {
    role("mentor", 5L);
    Attempt old = new Attempt();
    old.setMaxScore(10);
    when(mapper.attempts(20L)).thenReturn(Collections.singletonList(old));
    ReviewInput b = new ReviewInput();
    b.setVersion(3);
    b.setDecision("GRADED");
    b.setScore(11);
    assertEquals(
        400, assertThrows(ServiceException.class, () -> service.review(10L, 20L, b)).getCode());
    verify(mapper, never()).reviewAttempt(any());
  }

  @Test
  void repeatedRequestCannotCreateSecondAttempt() {
    when(courses.enrolled(1L, 2L)).thenReturn(1);
    SubmitInput b = new SubmitInput();
    b.setRequestKey("same-key");
    when(mapper.attemptByKey(20L, "same-key")).thenReturn(new Attempt());
    assertEquals(20L, service.submit(10L, b).getId());
    verify(mapper, never()).insertAttempt(any());
  }

  @Test
  void archivedCourseCannotAcceptChanges() {
    role("mentor", 5L);
    course.setStatus("ARCHIVED");
    assertThrows(ServiceException.class, () -> service.update(10L, new AssignmentInput()));
    verify(mapper, never()).updateAssignment(any());
  }
}
