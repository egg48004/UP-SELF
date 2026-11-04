package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.BadgeView;
import com.upself.biz.learning.mapper.*;
import com.upself.biz.learning.service.IBadgeService;
import com.upself.biz.learning.service.impl.*;
import com.upself.biz.goal.dto.GoalBodies.Completion;
import com.upself.biz.goal.mapper.GoalMapper;
import com.upself.biz.goal.service.impl.GoalServiceImpl;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class BadgeTest {
  BadgeMapper mapper;
  BadgeServiceImpl service;
  @BeforeEach void setup() {
    mapper=mock(BadgeMapper.class); service=new BadgeServiceImpl();
    ReflectionTestUtils.setField(service,"mapper",mapper);
    when(mapper.own(2L)).thenReturn(Collections.emptyList()); role("student");
  }
  void role(String key) {
    LoginUser u=new LoginUser(); u.setUserId(2L); u.setRoleKey(key);
    u.setChannel("student".equals(key)?"student":"admin"); u.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,u.getAuthorities()));
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  @Test void catalogIncludesUnearnedMilestonesAndOnlyTheCurrentOwner() {
    List<BadgeView> result=service.mine();
    assertEquals(3,result.size()); assertEquals("FIRST_STEP",result.get(0).getCode());
    assertTrue(result.stream().allMatch(b->b.getAwardedAt()==null)); verify(mapper).own(2L);
  }
  @Test void earnedDatesStayWithTheirCodeAndUnknownRowsDoNotCreateExtraBadges(){
    BadgeView earned=new BadgeView(); earned.setCode("GOAL_REACHED"); earned.setAwardedAt(new Date(123456));
    BadgeView unknown=new BadgeView();unknown.setCode("OTHER");
    when(mapper.own(2L)).thenReturn(Arrays.asList(unknown,earned));
    List<BadgeView> result=service.mine();assertEquals(3,result.size());
    assertEquals(earned.getAwardedAt(),result.get(2).getAwardedAt());assertNull(result.get(0).getAwardedAt());
  }
  @Test void wildcardRolesCannotReadOrAwardStudentBadges(){
    for(String key:new String[]{"mentor","platform_admin","admin"}){
      role(key);assertEquals(403,assertThrows(ServiceException.class,service::mine).getCode());
      assertThrows(ServiceException.class,()->service.award(BadgeCode.FIRST_STEP));
    }
    verifyNoInteractions(mapper);
  }
  @Test void firstAwardUsesAuthenticatedIdentity(){
    assertEquals("FIRST_STEP",service.award(BadgeCode.FIRST_STEP));verify(mapper).insert(2L,BadgeCode.FIRST_STEP);
  }
  @Test void uniqueConstraintRetryIsHarmless(){
    doThrow(new DuplicateKeyException("existing")).when(mapper).insert(2L,BadgeCode.GOAL_REACHED);
    assertNull(service.award(BadgeCode.GOAL_REACHED));
  }
  @Test void databaseFailurePropagatesSoTheBusinessOperationCanRollBack(){
    doThrow(new DataAccessResourceFailureException("offline")).when(mapper).insert(2L,BadgeCode.FIRST_SUBMISSION);
    assertThrows(DataAccessResourceFailureException.class,()->service.award(BadgeCode.FIRST_SUBMISSION));
  }
  ProgressServiceImpl progress(IBadgeService badges,ProgressMapper p){
    ProgressServiceImpl s=new ProgressServiceImpl();LearningMapper l=mock(LearningMapper.class);
    ReflectionTestUtils.setField(s,"learning",l);ReflectionTestUtils.setField(s,"mapper",p);ReflectionTestUtils.setField(s,"badges",badges);
    Chapter chapter=new Chapter();chapter.setId(4L);chapter.setCourseId(1L);chapter.setStatus("PUBLISHED");
    Course course=new Course();course.setId(1L);course.setStatus("PUBLISHED");
    when(l.chapter(4L)).thenReturn(chapter);when(l.lockCourse(1L)).thenReturn(course);when(l.enrolled(1L,2L)).thenReturn(1);return s;
  }
  @Test void completingAnAuthorizedChapterAwardsTheMilestone(){
    IBadgeService b=mock(IBadgeService.class);ProgressMapper p=mock(ProgressMapper.class);ProgressServiceImpl s=progress(b,p);
    when(b.award(BadgeCode.FIRST_STEP)).thenReturn("FIRST_STEP");assertEquals("FIRST_STEP",s.complete(4L,true));
    verify(p).complete(2L,4L,true);verify(b).award(BadgeCode.FIRST_STEP);
  }
  @Test void visitsAndUndoCannotAwardAMilestone(){
    IBadgeService b=mock(IBadgeService.class);ProgressMapper p=mock(ProgressMapper.class);ProgressServiceImpl s=progress(b,p);
    s.visit(4L);assertNull(s.complete(4L,false));verifyNoInteractions(b);
  }
  @Test void missingChapterDoesNotAward(){
    IBadgeService b=mock(IBadgeService.class);ProgressServiceImpl s=progress(b,mock(ProgressMapper.class));
    assertThrows(ServiceException.class,()->s.complete(99L,true));verifyNoInteractions(b);
  }
  @Test void goalAwardsOnlyAfterVersionedUpdateSucceeds(){
    IBadgeService b=mock(IBadgeService.class);GoalMapper m=mock(GoalMapper.class);GoalServiceImpl s=new GoalServiceImpl();
    ReflectionTestUtils.setField(s,"mapper",m);ReflectionTestUtils.setField(s,"badges",b);
    Completion body=new Completion();body.setCompleted(true);body.setVersion(1);
    assertThrows(ServiceException.class,()->s.complete(9L,body));verifyNoInteractions(b);
    when(m.complete(9L,2L,1,true)).thenReturn(1);when(b.award(BadgeCode.GOAL_REACHED)).thenReturn("GOAL_REACHED");
    assertEquals("GOAL_REACHED",s.complete(9L,body));verify(b).award(BadgeCode.GOAL_REACHED);
    clearInvocations(b);body.setCompleted(false);when(m.complete(9L,2L,1,false)).thenReturn(1);
    assertNull(s.complete(9L,body));verifyNoInteractions(b);
  }
}
