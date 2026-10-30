package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.SendInput;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.chat.service.ChatFileStorage;
import com.upself.biz.chat.service.impl.ChatServiceImpl;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ChatSafetyTest {
  ChatMapper mapper;
  LearningMapper learning;
  ChatServiceImpl service;
  ChatGroup group;
  Course course;
  ChatContentFilter filter = new ChatContentFilter();

  @BeforeEach
  void setup() {
    mapper = mock(ChatMapper.class);
    learning = mock(LearningMapper.class);
    service = new ChatServiceImpl();
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "learning", learning);
    ReflectionTestUtils.setField(service, "filter", filter);
    group = new ChatGroup();
    group.setId(1L);
    group.setCourseId(10L);
    group.setStatus("ACTIVE");
    course = new Course();
    course.setId(10L);
    course.setMentorId(20L);
    course.setStatus("PUBLISHED");
    when(mapper.group(1L)).thenReturn(group);
    when(mapper.lockGroup(1L)).thenReturn(group);
    when(learning.course(10L)).thenReturn(course);
    when(learning.lockCourse(10L)).thenReturn(course);
    when(mapper.eligible(10L, 2L)).thenReturn(1);
    when(mapper.eligible(10L, 3L)).thenReturn(1);
    when(mapper.isMember(1L, 2L)).thenReturn(1);
    when(mapper.isMember(1L, 3L)).thenReturn(1);
    role("student", 2L);
  }

  void role(String key, long id) {
    LoginUser user = new LoginUser();
    user.setRoleKey(key);
    user.setUserId(id);
    user.setChannel("student".equals(key) ? "student" : "admin");
    user.getPermissions().add("*:*:*");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void filterPreservesLearningAndHelpSeekingLanguage() {
    for (String text :
        Arrays.asList(
            "class assignment business",
            "I feel sad. This is difficult. Please help.",
            "I am stupid at this",
            "https://meet.example.com/class",
            "你好，let us learn together 👋")) assertDoesNotThrow(() -> filter.check(text, false));
  }

  @Test
  void explicitPhrasesNormalizeCaseSpacingPunctuationAndCompatibilityCharacters() {
    for (String text :
        Arrays.asList(
            "YOU ARE STUPID!",
            "you...are\tstupid",
            "ｙｏｕ ａｒｅ ｓｔｕｐｉｄ",
            "you are stu\u200bpid",
            "kau bodoh"))
      assertEquals(
          422, assertThrows(ServiceException.class, () -> filter.check(text, false)).getCode());
  }

  @Test
  void phraseMatchingDoesNotMatchInsideLongerWords() {
    assertDoesNotThrow(
        () -> filter.check("I will kill your doubts. You are stupidly talented.", false));
  }

  @Test
  void fileMetadataUsesClearAttachmentError() {
    ServiceException e =
        assertThrows(ServiceException.class, () -> filter.check("you-are-stupid.png", true));
    assertTrue(e.getMessage().startsWith("File not shared."));
  }

  @Test
  void filteredSendDoesNotInsertMessageOrAttachFiles() {
    SendInput b = new SendInput();
    b.setBody("You are stupid");
    b.setClientKey("test-request-key-123");
    assertEquals(422, assertThrows(ServiceException.class, () -> service.send(1L, b)).getCode());
    verify(mapper, never()).insertMessage(any());
    verify(mapper, never()).attach(any(), any());
  }

  @Test
  void studentCanHideAndRestorePeerOnlyForOwnView() {
    service.hidePeer(1L, 3L);
    verify(mapper).hidePeer(1L, 2L, 3L);
    service.showPeer(1L, 3L);
    verify(mapper).showPeer(1L, 2L, 3L);
  }

  @Test
  void selfMentorAndNonMemberTargetsCannotBeHidden() {
    for (long target : new long[] {2L, 20L, 99L})
      assertThrows(ServiceException.class, () -> service.hidePeer(1L, target));
    verify(mapper, never()).hidePeer(any(), any(), any());
  }

  @Test
  void mentorAdminAndMaintenanceCannotUseHideEvenWithWildcard() {
    for (String key : new String[] {"mentor", "platform_admin", "admin"}) {
      role(key, 20L);
      assertEquals(
          403, assertThrows(ServiceException.class, () -> service.hidePeer(1L, 3L)).getCode());
      assertEquals(
          403, assertThrows(ServiceException.class, () -> service.showPeer(1L, 3L)).getCode());
    }
  }

  @Test
  void hiddenAttachmentIsDeniedToViewerButReadableByMentor() {
    ChatFile file = new ChatFile();
    file.setId(4L);
    file.setGroupId(1L);
    file.setOwnerId(3L);
    file.setMessageId(7L);
    when(mapper.file(4L)).thenReturn(file);
    when(mapper.hiddenMessage(7L, 2L)).thenReturn(1);
    assertThrows(ServiceException.class, () -> service.readFile(4L));
    role("mentor", 20L);
    assertSame(file, service.readFile(4L));
  }

  @Test
  void hiddenOnlyTailAdvancesCursorWithoutAttachmentQueries() {
    when(mapper.latestMessageId(1L)).thenReturn(99L);
    when(mapper.messages(1L, 5L, null, 2L, 99L)).thenReturn(new ArrayList<>());
    Map<String, Object> batch = service.messages(1L, 5L, null);
    assertEquals(99L, batch.get("nextCursor"));
    assertEquals(false, batch.get("hasMore"));
    verify(mapper, never()).batchMessageFiles(any());
    verify(mapper, never()).messageFiles(any());
  }

  @Test
  void boundedBatchUsesOneAttachmentQueryAndDoesNotSkipBacklog() {
    when(mapper.latestMessageId(1L)).thenReturn(100L);
    List<ChatMessage> rows = new ArrayList<>();
    for (long id = 1; id <= 31; id++) {
      ChatMessage m = new ChatMessage();
      m.setId(id);
      m.setSenderId(3L);
      m.setSenderRole("student");
      m.setStudentSender(true);
      rows.add(m);
    }
    when(mapper.messages(1L, 0L, null, 2L, 100L)).thenReturn(rows);
    Map<String, Object> batch = service.messages(1L, 0L, null);
    assertEquals(30, ((List<?>) batch.get("rows")).size());
    assertEquals(30L, batch.get("nextCursor"));
    assertEquals(true, batch.get("hasMore"));
    verify(mapper, times(1)).batchMessageFiles(any());
    verify(mapper, never()).messageFiles(any());
  }

  @Test
  void uploadWritesFileBeforeOpeningShortTransaction(@TempDir Path directory) throws Exception {
    ChatFileStorage files = new ChatFileStorage();
    files.setDirectory(directory.toString());
    ReflectionTestUtils.setField(service, "files", files);
    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    ReflectionTestUtils.setField(service, "transactionManager", tx);
    when(tx.getTransaction(any()))
        .thenAnswer(
            invocation -> {
              try (java.util.stream.Stream<Path> paths = Files.list(directory)) {
                assertEquals(1L, paths.count());
              }
              return new SimpleTransactionStatus();
            });
    ChatFile f =
        service.upload(
            1L,
            new MockMultipartFile(
                "file", "notes.txt", "text/plain", "Class notes".getBytes("UTF-8")),
            "");
    assertEquals("notes.txt", f.getOriginalName());
    verify(tx).commit(any());
    verify(mapper).insertFile(any());
  }

  @Test
  void uploadRechecksAccessAndRemovesFileOnFailedCommit(@TempDir Path directory) throws Exception {
    ChatFileStorage files = new ChatFileStorage();
    files.setDirectory(directory.toString());
    ReflectionTestUtils.setField(service, "files", files);
    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    ReflectionTestUtils.setField(service, "transactionManager", tx);
    when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    when(mapper.isMember(1L, 2L)).thenReturn(1, 0);
    assertThrows(
        ServiceException.class,
        () ->
            service.upload(
                1L,
                new MockMultipartFile("file", "notes.txt", "text/plain", "Notes".getBytes("UTF-8")),
                ""));
    try (java.util.stream.Stream<Path> paths = Files.list(directory)) {
      assertEquals(0L, paths.count());
    }
    verify(tx).rollback(any());
    verify(mapper, never()).insertFile(any());
  }

  @Test
  void parallelUploadsAreLimitedPerAccountAndGateIsReleased(@TempDir Path directory)
      throws Exception {
    ChatFileStorage files = spy(new ChatFileStorage());
    files.setDirectory(directory.toString());
    ReflectionTestUtils.setField(service, "files", files);
    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    ReflectionTestUtils.setField(service, "transactionManager", tx);
    when(tx.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1),
        release = new java.util.concurrent.CountDownLatch(1);
    doAnswer(
            invocation -> {
              entered.countDown();
              assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
              return invocation.callRealMethod();
            })
        .when(files)
        .save(any());
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      java.util.concurrent.Future<?> first =
          pool.submit(
              () -> {
                role("student", 2L);
                try {
                  return service.upload(
                      1L,
                      new MockMultipartFile("file", "first.txt", "text/plain", new byte[] {65}),
                      "");
                } finally {
                  SecurityContextHolder.clearContext();
                }
              });
      assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(
          429,
          assertThrows(
                  ServiceException.class,
                  () ->
                      service.upload(
                          1L,
                          new MockMultipartFile(
                              "file", "second.txt", "text/plain", new byte[] {66}),
                          ""))
              .getCode());
      release.countDown();
      first.get(5, java.util.concurrent.TimeUnit.SECONDS);
      assertDoesNotThrow(
          () ->
              service.upload(
                  1L,
                  new MockMultipartFile("file", "retry.txt", "text/plain", new byte[] {67}),
                  ""));
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }
}
