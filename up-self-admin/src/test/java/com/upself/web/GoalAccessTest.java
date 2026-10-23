package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.goal.domain.PersonalGoal;
import com.upself.biz.goal.dto.GoalBodies.*;
import com.upself.biz.goal.mapper.GoalMapper;
import com.upself.biz.goal.service.impl.GoalServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class GoalAccessTest {
    GoalMapper mapper;
    GoalServiceImpl service;

    @BeforeEach void setup() {
        mapper = mock(GoalMapper.class);
        service = new GoalServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        role("student");
    }

    void role(String role) {
        LoginUser user = new LoginUser();
        user.setUserId(2L);
        user.setRoleKey(role);
        user.setChannel("student".equals(role) ? "student" : "admin");
        user.getPermissions().add("*:*:*");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void nonStudentsCannotUseGoalsEvenWithWildcardPermissions() {
        for (String role : new String[]{"mentor", "platform_admin", "admin"}) {
            role(role);
            assertEquals(403, assertThrows(ServiceException.class, () -> service.list(null)).getCode());
            assertThrows(ServiceException.class, () -> service.detail(9L));
            assertThrows(ServiceException.class, () -> service.create(new Create()));
            assertThrows(ServiceException.class, () -> service.edit(9L, new Update()));
            assertThrows(ServiceException.class, () -> service.complete(9L, new Completion()));
            assertThrows(ServiceException.class, () -> service.delete(9L, 0));
        }
        verifyNoInteractions(mapper);
    }

    @Test void listAlwaysUsesCurrentStudentAndRejectsInvalidFilter() {
        when(mapper.list(2L, "ACTIVE")).thenReturn(Collections.emptyList());
        service.list("ACTIVE");
        verify(mapper).list(2L, "ACTIVE");
        assertEquals(400, assertThrows(ServiceException.class, () -> service.list("INVALID")).getCode());
    }

    @Test void missingAndForeignGoalsHaveTheSameResponse() {
        assertEquals(404, assertThrows(ServiceException.class, () -> service.detail(9L)).getCode());
        verify(mapper).own(9L, 2L);
    }

    @Test void creatingUsesAuthenticatedOwnerAndTrimsInput() {
        Create b = new Create(); b.setTitle("  One lesson  "); b.setDetails(" Notes ");
        service.create(b);
        ArgumentCaptor<PersonalGoal> capture = ArgumentCaptor.forClass(PersonalGoal.class);
        verify(mapper).insert(capture.capture());
        assertEquals(2L, capture.getValue().getStudentId());
        assertEquals("One lesson", capture.getValue().getTitle());
        assertEquals("Notes", capture.getValue().getDetails());
    }

    @Test void whitespaceGoalCannotBeSaved() {
        Create b = new Create(); b.setTitle("  \t ");
        assertEquals(400, assertThrows(ServiceException.class, () -> service.create(b)).getCode());
        verifyNoInteractions(mapper);
    }

    @Test void staleEditNeverOverwritesAnotherVersion() {
        Update b = new Update(); b.setTitle("Edited"); b.setVersion(0);
        when(mapper.own(9L, 2L)).thenReturn(new PersonalGoal());
        assertEquals(409, assertThrows(ServiceException.class, () -> service.edit(9L, b)).getCode());
        verify(mapper).edit(9L, 2L, 0, "Edited", "");
    }

    @Test void foreignMutationReturnsNotFound() {
        Completion b = new Completion(); b.setVersion(0); b.setCompleted(true);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.complete(9L, b)).getCode());
        assertEquals(404, assertThrows(ServiceException.class, () -> service.delete(9L, 0)).getCode());
        verify(mapper).complete(9L, 2L, 0, true);
        verify(mapper).delete(9L, 2L, 0);
    }

    @Test void staleDeleteDoesNotSilentlyRemoveChangedGoal() {
        when(mapper.own(9L, 2L)).thenReturn(new PersonalGoal());
        assertEquals(409, assertThrows(ServiceException.class, () -> service.delete(9L, 0)).getCode());
        assertEquals(400, assertThrows(ServiceException.class, () -> service.delete(9L, -1)).getCode());
    }
}
