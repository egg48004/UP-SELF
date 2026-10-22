package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.learning.service.impl.LearningServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.Messages;

import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class LearningAccessTest {
    private LearningServiceImpl service;
    private LearningMapper mapper;
    private LoginUser student;
    private Course course;

    @BeforeEach
    void setup() {
        service = new LearningServiceImpl();
        mapper = mock(LearningMapper.class);
        Messages messages = mock(Messages.class);
        when(messages.get(anyString())).thenAnswer(call -> call.getArgument(0));
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "messages", messages);
        student = new LoginUser();
        student.setUserId(2L);
        student.setRoleKey("student");
        student.setChannel("student");
        student.getPermissions().add("learning:resource:query");
        course = new Course();
        course.setId(1L);
        course.setMentorId(5L);
        course.setStatus("PUBLISHED");
        Chapter chapter = new Chapter();
        chapter.setId(10L);
        chapter.setCourseId(1L);
        chapter.setStatus("PUBLISHED");
        LearningResource resource = new LearningResource();
        resource.setId(20L);
        resource.setChapterId(10L);
        resource.setStatus("PUBLISHED");
        when(mapper.resource(20L)).thenReturn(resource);
        when(mapper.chapter(10L)).thenReturn(chapter);
        when(mapper.course(1L)).thenReturn(course);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unjoinedStudentCannotReadPublishedResource() {
        assertEquals(
                403,
                assertThrows(ServiceException.class, () -> service.readMedia(20L, student))
                        .getCode());
    }

    @Test
    void joinedStudentCanReadPublishedResource() {
        when(mapper.enrolled(1L, 2L)).thenReturn(1);
        assertEquals(20L, service.readMedia(20L, student).getId());
    }

    @Test
    void draftCourseBlocksItsPublishedResources() {
        course.setStatus("DRAFT");
        when(mapper.enrolled(1L, 2L)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.readMedia(20L, student));
    }

    @Test
    void wrongMentorCannotReadCourseResources() {
        student.setRoleKey("mentor");
        student.setChannel("admin");
        assertThrows(ServiceException.class, () -> service.readMedia(20L, student));
    }

    @Test
    void revokedOperationPermissionBlocksMediaEvenWithEnrollment() {
        student.getPermissions().clear();
        when(mapper.enrolled(1L, 2L)).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.readMedia(20L, student));
    }

    @Test
    void newAssignedMentorCannotEditPreviousMentorPost() {
        student.setRoleKey("mentor");
        student.setChannel("admin");
        course.setMentorId(2L);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                student, null, student.getAuthorities()));
        CoursePost post = new CoursePost();
        post.setId(30L);
        post.setCourseId(1L);
        post.setCreatedBy(5L);
        when(mapper.post(30L)).thenReturn(post);
        when(mapper.lockCourse(1L)).thenReturn(course);
        assertThrows(
                ServiceException.class,
                () ->
                        service.updatePost(
                                30L, new com.upself.biz.learning.dto.LearningBodies.PostInput()));
        verify(mapper, never()).updatePost(any());
    }
}
