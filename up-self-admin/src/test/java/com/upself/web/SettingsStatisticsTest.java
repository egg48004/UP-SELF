package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.pagehelper.Page;
import com.upself.biz.learning.dto.LearningStatistics;
import com.upself.biz.learning.mapper.ProgressMapper;
import com.upself.biz.learning.service.impl.ProgressServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.domain.AccessibilityDefaults;
import com.upself.system.mapper.AccessibilityMapper;
import com.upself.system.mapper.SysAuditMapper;
import com.upself.system.service.impl.AccessibilityServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SettingsStatisticsTest {
  AccessibilityMapper settings;
  SysAuditMapper audit;
  AccessibilityServiceImpl defaults;
  ProgressMapper progress;
  ProgressServiceImpl learning;

  @BeforeEach
  void setup() {
    settings = mock(AccessibilityMapper.class);
    audit = mock(SysAuditMapper.class);
    defaults = new AccessibilityServiceImpl();
    ReflectionTestUtils.setField(defaults, "mapper", settings);
    ReflectionTestUtils.setField(defaults, "audit", audit);
    progress = mock(ProgressMapper.class);
    learning = new ProgressServiceImpl();
    ReflectionTestUtils.setField(learning, "mapper", progress);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  void role(String key) {
    LoginUser user = new LoginUser();
    user.setUserId(2L);
    user.setRoleKey(key);
    user.setChannel("student".equals(key) ? "student" : "admin");
    user.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }

  AccessibilityDefaults body() {
    AccessibilityDefaults b = new AccessibilityDefaults();
    b.setSize("large");
    b.setContrast("standard");
    b.setMotion(true);
    b.setVersion(3);
    return b;
  }

  @Test
  void maintenanceWildcardDoesNotPermitDefaultChanges() {
    role("admin");
    assertThrows(ServiceException.class, () -> defaults.update(body()));
    verifyNoInteractions(settings, audit);
  }

  @Test
  void mentorAndStudentCannotUpdateDefaults() {
    for (String key : new String[] {"mentor", "student"}) {
      role(key);
      assertThrows(ServiceException.class, () -> defaults.update(body()));
    }
    verifyNoInteractions(settings, audit);
  }

  @Test
  void staleDefaultsDoNotCreateAnAuditSuccess() {
    role("platform_admin");
    assertThrows(ServiceException.class, () -> defaults.update(body()));
    verifyNoInteractions(audit);
    verify(settings, never()).read();
  }

  @Test
  void successfulUpdateReturnsTheSavedVersionAndAuditsOnlyIdentity() {
    role("platform_admin");
    AccessibilityDefaults b = body(), saved = body();
    saved.setVersion(4);
    when(settings.update(b, 2L)).thenReturn(1);
    when(settings.read()).thenReturn(saved);
    assertEquals(4, defaults.update(b).getVersion().intValue());
    verify(audit).insert(2L, "ACCESSIBILITY_DEFAULTS_UPDATE", "1", "SUCCESS");
  }

  @Test
  void missingDatabaseSettingsAreNotReplacedWithInventedValues() {
    assertThrows(ServiceException.class, defaults::read);
  }

  @Test
  void totalsUseChapterWeightAndTheAuthenticatedStudent() {
    role("student");
    LearningStatistics.Totals t = new LearningStatistics.Totals();
    t.setCourseCount(2);
    t.setTotalChapters(4);
    t.setCompletedChapters(1);
    when(progress.statistics(2L)).thenReturn(t);
    assertEquals(25, learning.statistics().getPercent());
    verify(progress).statistics(2L);
  }

  @Test
  void emptyLearningHasZeroPercent() {
    role("student");
    assertEquals(0, learning.statistics().getPercent());
  }

  @Test
  void statisticsRejectOtherRolesEvenWithWildcardPermissions() {
    for (String key : new String[] {"mentor", "platform_admin", "admin"}) {
      role(key);
      assertThrows(ServiceException.class, learning::statistics);
      assertThrows(ServiceException.class, learning::statisticsCourses);
    }
    verifyNoInteractions(progress);
  }

  @Test
  void coursePercentCalculationKeepsPageHelperMetadata() {
    role("student");
    Page<LearningStatistics.CourseRow> page = new Page<>(2, 10);
    page.setTotal(31);
    LearningStatistics.CourseRow row = new LearningStatistics.CourseRow();
    row.setTotalChapters(3);
    row.setCompletedChapters(2);
    page.add(row);
    when(progress.statisticsCourses(2L)).thenReturn(page);
    assertSame(page, learning.statisticsCourses());
    assertEquals(31, page.getTotal());
    assertEquals(67, row.getPercent());
  }
}
