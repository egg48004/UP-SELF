package com.upself.web;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.*;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.impl.ChatServiceImpl;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
class ChatAccessTest {
 ChatMapper mapper; LearningMapper learning; ChatServiceImpl service; ChatGroup group; Course course;
 void role(String role,long id){LoginUser u=new LoginUser();u.setRoleKey(role);u.setUserId(id);u.setChannel(role.equals("student")?"student":"admin");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,u.getAuthorities()));}
 @BeforeEach void setup(){
  mapper=mock(ChatMapper.class);learning=mock(LearningMapper.class);service=new ChatServiceImpl();
  ReflectionTestUtils.setField(service,"mapper",mapper);ReflectionTestUtils.setField(service,"learning",learning);
  group=new ChatGroup();group.setId(1L);group.setCourseId(10L);group.setStatus("ACTIVE");group.setVersion(0);
  course=new Course();course.setId(10L);course.setMentorId(20L);course.setStatus("PUBLISHED");
  when(mapper.group(1L)).thenReturn(group);when(mapper.lockGroup(1L)).thenReturn(group);
  when(learning.course(10L)).thenReturn(course);when(learning.lockCourse(10L)).thenReturn(course);
  when(mapper.eligible(10L,2L)).thenReturn(1);when(mapper.isMember(1L,2L)).thenReturn(1);role("student",2L);
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void removedStudentCannotReadHistory(){when(mapper.isMember(1L,2L)).thenReturn(0);assertEquals(403,assertThrows(ServiceException.class,()->service.messages(1L,null,null)).getCode());verify(mapper,never()).messages(anyLong(),any(),any());}
 @Test void enrollmentIsRequiredEvenWithGroupMembership(){when(mapper.eligible(10L,2L)).thenReturn(0);assertThrows(ServiceException.class,()->service.detail(1L));}
 @Test void administratorHasNoBusinessBypass(){role("platform_admin",30L);assertEquals(403,assertThrows(ServiceException.class,()->service.detail(1L)).getCode());}
 @Test void reassignedMentorLosesAccess(){role("mentor",21L);assertThrows(ServiceException.class,()->service.detail(1L));role("mentor",20L);assertEquals(1L,service.detail(1L).getId());}
 @Test void memberCannotManageGroup(){assertThrows(ServiceException.class,()->service.addMember(1L,3L));verify(mapper,never()).addMember(anyLong(),anyLong());}
 @Test void pendingAttachmentIsPrivateEvenToMentor(){ChatFile f=new ChatFile();f.setId(4L);f.setGroupId(1L);f.setOwnerId(2L);when(mapper.file(4L)).thenReturn(f);role("mentor",20L);assertThrows(ServiceException.class,()->service.readFile(4L));f.setMessageId(5L);assertEquals(f,service.readFile(4L));}
 @Test void closedGroupIsReadableButCannotSend(){group.setStatus("CLOSED");assertEquals(group,service.detail(1L));SendInput b=new SendInput();b.setBody("Hello");b.setClientKey("1234567890123456");assertEquals(409,assertThrows(ServiceException.class,()->service.send(1L,b)).getCode());verify(mapper,never()).insertMessage(any());}
 @Test void reusedSendKeyMustMatchOriginalPayload(){SendInput b=new SendInput();b.setBody("Changed");b.setClientKey("1234567890123456");ChatMessage old=new ChatMessage();old.setId(6L);old.setBody("Original");when(mapper.byKey(1L,2L,b.getClientKey())).thenReturn(old);when(mapper.messageFiles(6L)).thenReturn(Collections.emptyList());assertEquals(409,assertThrows(ServiceException.class,()->service.send(1L,b)).getCode());}
 @Test void cannotSendSomeoneElsesDraft(){SendInput b=new SendInput();b.setBody("");b.setClientKey("1234567890123456");b.setFileIds(Arrays.asList(4L));ChatFile f=new ChatFile();f.setId(4L);f.setGroupId(1L);f.setOwnerId(9L);when(mapper.file(4L)).thenReturn(f);assertThrows(ServiceException.class,()->service.send(1L,b));verify(mapper,never()).insertMessage(any());}
}
