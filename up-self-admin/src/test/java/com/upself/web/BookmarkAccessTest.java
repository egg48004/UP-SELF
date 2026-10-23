package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.BookmarkMapper;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.learning.service.impl.BookmarkServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class BookmarkAccessTest {
    BookmarkServiceImpl service;
    BookmarkMapper mapper;
    LearningMapper learning;

    @BeforeEach void setup() {
        service = new BookmarkServiceImpl();
        mapper = mock(BookmarkMapper.class);
        learning = mock(LearningMapper.class);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "learning", learning);
        role("student");
    }
    void role(String role) {
        LoginUser user = new LoginUser(); user.setUserId(2L); user.setRoleKey(role);
        user.setChannel("student".equals(role) ? "student" : "admin");
        user.getPermissions().add("*:*:*");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void nonStudentsCannotUseBookmarksEvenWithWildcardPermission() {
        for (String role : new String[]{"mentor", "platform_admin", "admin"}) {
            role(role);
            assertEquals(403, assertThrows(ServiceException.class, () -> service.list()).getCode());
            assertThrows(ServiceException.class, () -> service.save(9L));
            assertThrows(ServiceException.class, () -> service.remove(9L));
        }
        verifyNoInteractions(mapper, learning);
    }
    @Test void listAlwaysUsesAuthenticatedStudent() {
        service.list(); verify(mapper).list(2L);
    }
    @Test void missingCourseCannotBeSaved() {
        assertEquals(404, assertThrows(ServiceException.class, () -> service.save(9L)).getCode());
        verify(mapper, never()).save(anyLong(), anyLong());
    }
    @Test void unpublishedCourseCannotBeSavedEvenWhenEnrolled() {
        Course c = new Course(); c.setStatus("DRAFT");
        when(learning.lockCourse(9L)).thenReturn(c);
        assertEquals(403, assertThrows(ServiceException.class, () -> service.save(9L)).getCode());
        verify(mapper, never()).save(anyLong(), anyLong());
    }
    @Test void publishedCourseRequiresEnrollment() {
        Course c = new Course(); c.setStatus("PUBLISHED");
        when(learning.lockCourse(9L)).thenReturn(c);
        assertEquals(403, assertThrows(ServiceException.class, () -> service.save(9L)).getCode());
        verify(mapper, never()).save(anyLong(), anyLong());
    }
    @Test void saveUsesCurrentOwnerAndChecksCourseUnderSharedLock() {
        Course c = new Course(); c.setStatus("PUBLISHED");
        when(learning.lockCourse(9L)).thenReturn(c); when(learning.enrolled(9L, 2L)).thenReturn(1);
        service.save(9L);
        org.mockito.InOrder order = inOrder(learning, mapper);
        order.verify(learning).lockCourse(9L); order.verify(learning).enrolled(9L, 2L);
        order.verify(mapper).save(2L, 9L);
    }
    @Test void removingOwnBookmarkDoesNotRequireCurrentCourseAccess() {
        service.remove(9L);
        verify(mapper).remove(2L, 9L); verifyNoInteractions(learning);
    }
}
