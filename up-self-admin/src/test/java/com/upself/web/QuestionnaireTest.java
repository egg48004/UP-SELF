package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.learning.domain.Questionnaire;
import com.upself.biz.learning.dto.QuestionnaireBodies.*;
import com.upself.biz.learning.mapper.QuestionnaireMapper;
import com.upself.biz.learning.service.impl.QuestionnaireServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionnaireTest {
  QuestionnaireMapper mapper;
  SysAuditMapper audit;
  QuestionnaireServiceImpl service;

  @BeforeEach
  void setup() {
    mapper = mock(QuestionnaireMapper.class);
    audit = mock(SysAuditMapper.class);
    service = new QuestionnaireServiceImpl();
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "audit", audit);
    role("mentor");
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  void role(String key) {
    LoginUser u = new LoginUser();
    u.setUserId(2L);
    u.setRoleKey(key);
    u.setChannel(key.equals("student") ? "student" : "admin");
    u.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  Create body(String url) {
    Create b = new Create();
    b.setTitle("  Research questionnaire  ");
    b.setUrl(url);
    return b;
  }

  @Test
  void studentReadsGlobalVisibleListWithoutACourseOrMentor() {
    role("student");
    when(mapper.visible()).thenReturn(Collections.emptyList());
    assertTrue(service.visible().isEmpty());
    verify(mapper).visible();
    verifyNoInteractions(audit);
  }

  @Test
  void hiddenAndMissingStudentLinksBothReturn404() {
    role("student");
    assertEquals(
        404, assertThrows(ServiceException.class, () -> service.visibleById(8L)).getCode());
  }

  @Test
  void studentCannotManageEvenWithWildcard() {
    role("student");
    assertThrows(ServiceException.class, service::authored);
    assertThrows(ServiceException.class, () -> service.create(body("https://example.org/form")));
    verifyNoInteractions(mapper, audit);
  }

  @Test
  void adminAndMaintenanceCannotActAsQuestionnaireMentors() {
    for (String role : new String[] {"platform_admin", "admin"}) {
      role(role);
      assertThrows(ServiceException.class, service::authored);
      assertThrows(ServiceException.class, service::visible);
      assertThrows(ServiceException.class, () -> service.create(body("https://example.org/form")));
    }
    verifyNoInteractions(mapper, audit);
  }

  @Test
  void mentorListsOnlyTheirOwnLinks() {
    when(mapper.authored(2L)).thenReturn(Collections.emptyList());
    service.authored();
    verify(mapper).authored(2L);
  }

  @Test
  void newLinkDefaultsHiddenAndUsesAuthenticatedAuthor() {
    doAnswer(
            i -> {
              ((Questionnaire) i.getArgument(0)).setId(5L);
              return 1;
            })
        .when(mapper)
        .insert(any());
    assertEquals(5L, service.create(body("  https://forms.example.org/form?x=1#page  ")));
    org.mockito.ArgumentCaptor<Questionnaire> c =
        org.mockito.ArgumentCaptor.forClass(Questionnaire.class);
    verify(mapper).insert(c.capture());
    assertEquals(2L, c.getValue().getCreatedBy());
    assertFalse(c.getValue().getVisible());
    assertEquals("Research questionnaire", c.getValue().getTitle());
    assertEquals("https://forms.example.org/form?x=1#page", c.getValue().getUrl());
  }

  @Test
  void invalidSchemesCredentialsHostsAndPortsNeverReachDatabase() {
    for (String u :
        new String[] {
          "http://example.org/form",
          "javascript:alert(1)",
          "data:text/html,test",
          "//example.org/form",
          "https://user:pass@example.org/form",
          "https://@example.org/form",
          "https:example.org",
          "https://",
          "https://example.org:0/form",
          "https://example.org:70000/form",
          "https://example.org/has space",
          "https://example.org/line\nfeed"
        })
      assertEquals(
          400, assertThrows(ServiceException.class, () -> service.create(body(u))).getCode(), u);
    verifyNoInteractions(mapper, audit);
  }

  @Test
  void emptyOrOversizedTitleRejected() {
    Create b = body("https://example.org");
    b.setTitle(" ");
    assertThrows(ServiceException.class, () -> service.create(b));
    b.setTitle(String.join("", Collections.nCopies(151, "a")));
    assertThrows(ServiceException.class, () -> service.create(b));
    verifyNoInteractions(mapper, audit);
  }

  @Test
  void foreignMutationIs404AndNeverAudited() {
    Visibility b = new Visibility();
    b.setVersion(0);
    b.setVisible(true);
    assertEquals(
        404, assertThrows(ServiceException.class, () -> service.visibility(9L, b)).getCode());
    verify(mapper).visibility(9L, 2L, true, 0);
    verifyNoInteractions(audit);
  }

  @Test
  void staleVisibilityIs409AndNeverAudited() {
    Visibility b = new Visibility();
    b.setVersion(0);
    b.setVisible(false);
    when(mapper.owned(9L, 2L)).thenReturn(new Questionnaire());
    assertEquals(
        409, assertThrows(ServiceException.class, () -> service.visibility(9L, b)).getCode());
    verifyNoInteractions(audit);
  }

  @Test
  void successfulVisibilityChangeAuditsIdentityWithoutTheExternalUrl() {
    Visibility b = new Visibility();
    b.setVersion(3);
    b.setVisible(true);
    when(mapper.visibility(9L, 2L, true, 3)).thenReturn(1);
    service.visibility(9L, b);
    verify(audit).insert(2L, "QUESTIONNAIRE_VISIBILITY", "9", "SUCCESS");
  }

  @Test
  void deleteAlwaysUsesOwnerAndVersion() {
    when(mapper.delete(9L, 2L, 4)).thenReturn(1);
    service.delete(9L, 4);
    verify(mapper).delete(9L, 2L, 4);
    assertThrows(ServiceException.class, () -> service.delete(9L, -1));
    verify(audit).insert(2L, "QUESTIONNAIRE_DELETE", "9", "SUCCESS");
  }

  @Test
  void updatePreservesOwnerScope() {
    Update b = new Update();
    b.setTitle("Edited title");
    b.setUrl("https://example.org/edit");
    b.setVersion(4);
    b.setVisible(true);
    when(mapper.update(any(), eq(4))).thenReturn(1);
    service.update(9L, b);
    org.mockito.ArgumentCaptor<Questionnaire> c =
        org.mockito.ArgumentCaptor.forClass(Questionnaire.class);
    verify(mapper).update(c.capture(), eq(4));
    assertEquals(9L, c.getValue().getId());
    assertEquals(2L, c.getValue().getCreatedBy());
    assertTrue(c.getValue().getVisible());
  }
}
