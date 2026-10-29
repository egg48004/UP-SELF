package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upself.biz.business.domain.*;
import com.upself.biz.business.dto.CostBodies;
import com.upself.biz.business.dto.IdeaBodies;
import com.upself.biz.business.mapper.*;
import com.upself.biz.business.service.impl.*;
import com.upself.biz.mentorship.domain.MentorNote;
import com.upself.biz.mentorship.dto.NoteBodies;
import com.upself.biz.mentorship.mapper.MentorNoteMapper;
import com.upself.biz.mentorship.service.impl.MentorNoteServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.SysAuditMapper;
import java.math.BigDecimal;
import javax.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class BusinessAndNotesTest {
  CostPlanMapper costMapper;
  BusinessIdeaMapper ideas;
  MentorNoteMapper notes;
  CostPlanServiceImpl costs;
  BusinessIdeaServiceImpl business;
  MentorNoteServiceImpl mentoring;

  @BeforeEach
  void setup() {
    costMapper = mock(CostPlanMapper.class);
    ideas = mock(BusinessIdeaMapper.class);
    notes = mock(MentorNoteMapper.class);
    costs = new CostPlanServiceImpl();
    business = new BusinessIdeaServiceImpl();
    mentoring = new MentorNoteServiceImpl();
    ReflectionTestUtils.setField(costs, "mapper", costMapper);
    ReflectionTestUtils.setField(business, "mapper", ideas);
    ReflectionTestUtils.setField(mentoring, "mapper", notes);
    ReflectionTestUtils.setField(mentoring, "audit", mock(SysAuditMapper.class));
    role("student");
  }

  void role(String role) {
    LoginUser u = new LoginUser();
    u.setUserId(2L);
    u.setRoleKey(role);
    u.setChannel("student".equals(role) ? "student" : "admin");
    u.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  CostBodies.Input input(String materials, String other, String quantity, String price) {
    CostBodies.Input b = new CostBodies.Input();
    b.setMaterialsCost(new BigDecimal(materials));
    b.setOtherCosts(new BigDecimal(other));
    b.setQuantity(new BigDecimal(quantity));
    b.setPricePerItem(new BigDecimal(price));
    return b;
  }

  CostBodies.Save save() {
    CostBodies.Save b = new CostBodies.Save();
    b.setMaterialsCost(BigDecimal.ONE);
    b.setOtherCosts(BigDecimal.ZERO);
    b.setPricePerItem(BigDecimal.TEN);
    b.setQuantity(BigDecimal.ONE);
    b.setVersion(0);
    return b;
  }

  @Test
  void decimalArithmeticIsExact() {
    CostPlan p = costs.calculate(input("0.10", "0.20", "3", "0.10"));
    assertEquals(new BigDecimal("0.30"), p.getTotalCost());
    assertEquals(new BigDecimal("0.00"), p.getEstimatedProfit());
  }

  @Test
  void lossesArePreserved() {
    assertEquals(
        new BigDecimal("-5.00"), costs.calculate(input("10", "5", "2", "5")).getEstimatedProfit());
  }

  @Test
  void largeMoneyIsSerializedAsExactDecimalStrings() throws Exception {
    CostPlan p = costs.calculate(input("9999999999.99", "0", "1000000", "9999999999.99"));
    String json = new ObjectMapper().writeValueAsString(p);
    assertTrue(json.contains("\"expectedRevenue\":\"9999999999990000.00\""));
    assertFalse(json.contains("studentId"));
  }

  @Test
  void fractionsAndZeroQuantitiesAreRejected() {
    for (String q : new String[] {"0", "-1", "1.5", "1000001"})
      assertEquals(
          400,
          assertThrows(ServiceException.class, () -> costs.calculate(input("0", "0", q, "1")))
              .getCode());
  }

  @Test
  void invalidMoneyNeverReachesPersistence() {
    for (String amount : new String[] {"-1", "1.001", "10000000000"})
      assertThrows(ServiceException.class, () -> costs.calculate(input(amount, "0", "1", "1")));
    verifyNoInteractions(costMapper);
  }

  @Test
  void beanValidationRejectsFractionalQuantities() {
    try (javax.validation.ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      assertFalse(factory.getValidator().validate(input("1", "1", "1.5", "1")).isEmpty());
    }
  }

  @Test
  void calculateDoesNotSave() {
    costs.calculate(input("1", "2", "4", "5"));
    verifyNoInteractions(costMapper);
  }

  @Test
  void absentCostPlanIsExplicitlyUnsaved() {
    CostPlan p = costs.current();
    assertFalse(p.isSaved());
    assertEquals(0, p.getVersion());
  }

  @Test
  void concurrentFirstSaveDoesNotOverwrite() {
    when(costMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
    assertEquals(409, assertThrows(ServiceException.class, () -> costs.save(save())).getCode());
  }

  @Test
  void staleSavedPlanDoesNotOverwrite() {
    CostBodies.Save b = save();
    b.setVersion(3);
    assertEquals(409, assertThrows(ServiceException.class, () -> costs.save(b)).getCode());
    verify(costMapper).edit(argThat(p -> p.getStudentId() == 2L && p.getVersion() == 3));
  }

  @Test
  void businessDeniedToOtherRolesDespiteWildcard() {
    for (String r : new String[] {"mentor", "platform_admin", "admin"}) {
      role(r);
      assertThrows(ServiceException.class, () -> business.list());
      assertThrows(ServiceException.class, () -> business.detail(3L));
      assertThrows(ServiceException.class, () -> costs.current());
      assertThrows(ServiceException.class, () -> costs.calculate(input("1", "1", "1", "1")));
    }
    verifyNoInteractions(ideas, costMapper);
  }

  @Test
  void ideasAlwaysUseAuthenticatedOwner() {
    business.list();
    verify(ideas).list(2L);
    assertEquals(404, assertThrows(ServiceException.class, () -> business.detail(9L)).getCode());
    verify(ideas).own(9L, 2L);
  }

  @Test
  void emptyIdeasAreRejected() {
    IdeaBodies.Create b = new IdeaBodies.Create();
    b.setTitle("  ");
    assertThrows(ServiceException.class, () -> business.create(b));
    verifyNoInteractions(ideas);
  }

  @Test
  void notesDeniedToOtherRolesDespiteWildcard() {
    for (String r : new String[] {"student", "platform_admin", "admin"}) {
      role(r);
      assertEquals(
          403, assertThrows(ServiceException.class, () -> mentoring.verifyStudent(3L)).getCode());
      assertThrows(ServiceException.class, () -> mentoring.list(3L));
    }
    verifyNoInteractions(notes);
  }

  @Test
  void unrelatedStudentDenied() {
    role("mentor");
    assertEquals(
        403, assertThrows(ServiceException.class, () -> mentoring.verifyStudent(3L)).getCode());
  }

  @Test
  void otherMentorsNotesAreUnavailable() {
    role("mentor");
    when(notes.student(2L, 3L)).thenReturn(new NoteBodies.Student());
    NoteBodies.Update b = new NoteBodies.Update();
    b.setContent("Private");
    b.setVersion(0);
    assertEquals(
        404, assertThrows(ServiceException.class, () -> mentoring.edit(3L, 9L, b)).getCode());
  }

  @Test
  void staleNoteDoesNotOverwrite() {
    role("mentor");
    when(notes.student(2L, 3L)).thenReturn(new NoteBodies.Student());
    when(notes.own(9L, 2L, 3L)).thenReturn(new MentorNote());
    NoteBodies.Update b = new NoteBodies.Update();
    b.setContent("Private");
    b.setVersion(0);
    assertEquals(
        409, assertThrows(ServiceException.class, () -> mentoring.edit(3L, 9L, b)).getCode());
  }
}
