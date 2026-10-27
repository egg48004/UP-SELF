package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.knowledge.domain.*;
import com.upself.biz.knowledge.dto.KnowledgeBodies.*;
import com.upself.biz.knowledge.mapper.KnowledgeMapper;
import com.upself.biz.knowledge.service.impl.KnowledgeServiceImpl;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class KnowledgeAccessTest {
    KnowledgeMapper mapper;
    LearningMapper learning;
    KnowledgeServiceImpl service;
    Course course;
    KnowledgeQuestion question;
    @BeforeEach void setup() {
        mapper = mock(KnowledgeMapper.class); learning = mock(LearningMapper.class);
        service = new KnowledgeServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "learning", learning);
        ReflectionTestUtils.setField(service, "audit", mock(SysAuditMapper.class));
        course = new Course(); course.setId(7L); course.setMentorId(5L); course.setStatus("PUBLISHED");
        question = new KnowledgeQuestion(); question.setId(9L); question.setCourseId(7L);
        question.setQuestionText("Can you explain this topic?"); question.setStatus("PUBLISHED"); question.setVersion(2);
        when(mapper.question(9L)).thenReturn(question); when(mapper.lockQuestion(9L)).thenReturn(question);
        when(learning.course(7L)).thenReturn(course); when(learning.lockCourse(7L)).thenReturn(course);
        role("mentor", 5L);
    }
    void role(String key, long id) {
        LoginUser user = new LoginUser(); user.setUserId(id); user.setRoleKey(key); user.setChannel("student".equals(key) ? "student" : "admin"); user.getPermissions().add("*:*:*");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    AnswerInput answer() { AnswerInput b = new AnswerInput(); b.setChoice("NOT_SURE"); b.setQuestionVersion(2); return b; }
    QuestionInput input() { QuestionInput b = new QuestionInput(); b.setQuestionText(question.getQuestionText()); b.setStatus("ARCHIVED"); b.setVersion(2); return b; }
    @Test void wildcardNeverOverridesBusinessRoles() {
        for (String key : new String[] {"admin", "platform_admin"}) {
            role(key, 1L);
            assertThrows(ServiceException.class, () -> service.ownQuestions(null));
            assertThrows(ServiceException.class, () -> service.mentorQuestions(null));
            assertThrows(ServiceException.class, () -> service.answer(9L, answer()));
            assertThrows(ServiceException.class, () -> service.verifyAnswers(9L));
        }
    }
    @Test void unassignedMentorCannotEditOrReadAnswers() {
        role("mentor", 6L);
        assertEquals(403, assertThrows(ServiceException.class, () -> service.update(9L, input())).getCode());
        assertThrows(ServiceException.class, () -> service.verifyAnswers(9L));
        verify(mapper, never()).update(any());
    }
    @Test void studentCannotManageQuestionsOrReadPeerAnswers() {
        role("student", 2L);
        assertThrows(ServiceException.class, () -> service.create(7L, input()));
        assertThrows(ServiceException.class, () -> service.verifyAnswers(9L));
    }
    @Test void unenrolledStudentCannotAnswer() {
        role("student", 2L);
        assertEquals(403, assertThrows(ServiceException.class, () -> service.answer(9L, answer())).getCode());
        verify(mapper, never()).saveAnswer(any());
    }
    @Test void staleQuestionAndDraftCannotBeAnswered() {
        role("student", 2L); when(mapper.canAnswer(2L, 7L)).thenReturn(1);
        AnswerInput b = answer(); b.setQuestionVersion(1);
        assertEquals(409, assertThrows(ServiceException.class, () -> service.answer(9L, b)).getCode());
        question.setStatus("DRAFT");
        assertThrows(ServiceException.class, () -> service.answer(9L, answer()));
        verify(mapper, never()).saveAnswer(any());
    }
    @Test void answerOwnerComesFromSessionAndChoiceIsValidated() {
        role("student", 2L); when(mapper.canAnswer(2L, 7L)).thenReturn(1);
        AnswerInput bad = answer(); bad.setChoice("MAYBE");
        assertThrows(ServiceException.class, () -> service.answer(9L, bad));
        service.answer(9L, answer());
        verify(mapper).saveAnswer(argThat(a -> a.getStudentId().equals(2L) && a.getQuestionId().equals(9L) && a.getChoice().equals("NOT_SURE")));
    }
    @Test void answeredWordingAndDeletionAreProtected() {
        when(mapper.answerCount(9L)).thenReturn(1);
        QuestionInput b = input(); b.setQuestionText("A different question?");
        assertEquals(409, assertThrows(ServiceException.class, () -> service.update(9L, b)).getCode());
        assertThrows(ServiceException.class, () -> service.delete(9L, 2));
        verify(mapper, never()).delete(anyLong());
    }
    @Test void answeredQuestionCanBeArchivedWithoutChangingWording() {
        when(mapper.answerCount(9L)).thenReturn(1); when(mapper.update(any())).thenReturn(1);
        service.update(9L, input());
        verify(mapper).update(argThat(q -> "ARCHIVED".equals(q.getStatus())));
    }
    @Test void archivedCoursesAreReadOnlyAndDoNotExposeAnswers() {
        course.setStatus("ARCHIVED");
        assertThrows(ServiceException.class, () -> service.update(9L, input()));
        assertThrows(ServiceException.class, () -> service.verifyAnswers(9L));
    }
}
