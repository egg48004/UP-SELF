package com.upself.biz.chat.service.impl;

import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.*;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.*;
import com.upself.biz.file.service.PrivateFileStorage;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.recording.service.RecordingStorage;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatServiceImpl implements IChatService {
  @Autowired private ChatMapper mapper;
  @Autowired private LearningMapper learning;
  @Autowired private ChatFileStorage files;
  @Autowired private SysAuditMapper audit;
  @Autowired private ChatContentFilter filter;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
  private final java.util.concurrent.ConcurrentMap<Long, Boolean> uploading =
      new java.util.concurrent.ConcurrentHashMap<>();

  private Long actor() {
    return SecurityUtils.getUserId();
  }

  private boolean student() {
    return "student".equals(SecurityUtils.getLoginUser().getRoleKey());
  }

  private void participant() {
    if (!student() && !"mentor".equals(SecurityUtils.getLoginUser().getRoleKey())) denied();
  }

  private void denied() {
    throw new ServiceException(403, "This discussion is not available to you.");
  }

  private Course course(Long id, boolean lock) {
    participant();
    Course c = lock ? learning.lockCourse(id) : learning.course(id);
    if (c == null) throw new ServiceException(404, "This course is unavailable.");
    if (student()) {
      if (!"PUBLISHED".equals(c.getStatus()) || mapper.eligible(id, actor()) != 1) denied();
    } else if (!actor().equals(c.getMentorId())) denied();
    return c;
  }

  private ChatGroup group(Long id, boolean lock, boolean write, boolean manager) {
    participant();
    ChatGroup initial = mapper.group(id);
    if (initial == null) throw new ServiceException(404, "This discussion is unavailable.");
    // Same course lock as enrollment and mentor reassignment, then the group lock.
    Course c = course(initial.getCourseId(), lock);
    ChatGroup g = lock ? mapper.lockGroup(id) : initial;
    if (manager && student()) denied();
    if (student() && mapper.isMember(id, actor()) != 1) denied();
    if (write && !"PUBLISHED".equals(c.getStatus()))
      throw new ServiceException(409, "Discussions are writable in published courses only.");
    if (write && !"ACTIVE".equals(g.getStatus()))
      throw new ServiceException(
          409, "This discussion is closed. You can still read its messages.");
    return g;
  }

  public void verifyCourse(Long id) {
    course(id, false);
  }

  public List<ChatGroup> groups(Long id) {
    participant();
    return mapper.groups(id, student() ? actor() : null, actor());
  }

  public ChatGroup detail(Long id) {
    return group(id, false, false, false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Long create(Long id, GroupInput b) {
    if (student()) denied();
    Course c = course(id, true);
    if (!"PUBLISHED".equals(c.getStatus()))
      throw new ServiceException(409, "Publish the course before creating a discussion.");
    ChatGroup g = new ChatGroup();
    g.setCourseId(id);
    g.setTitle(b.getTitle().trim());
    g.setStatus("ACTIVE");
    g.setCreatedBy(actor());
    mapper.insertGroup(g);
    audit.insert(actor(), "CHAT_CREATE", g.getId().toString(), "SUCCESS");
    return g.getId();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void update(Long id, GroupInput b) {
    ChatGroup g = group(id, true, false, true);
    Course c = learning.course(g.getCourseId());
    if (!"PUBLISHED".equals(c.getStatus()))
      throw new ServiceException(409, "Archived or draft course discussions are read-only.");
    if (!g.getVersion().equals(b.getVersion()))
      throw new ServiceException(409, "This discussion has changed. Refresh before saving.");
    g.setTitle(b.getTitle().trim());
    g.setStatus(b.getStatus());
    if (mapper.updateGroup(g) != 1)
      throw new ServiceException(409, "This discussion has changed. Refresh before saving.");
    audit.insert(actor(), "CHAT_UPDATE", id.toString(), "SUCCESS");
  }

  public List<ChatMember> members(Long id) {
    ChatGroup g = group(id, false, false, false);
    List<ChatMember> result = mapper.members(id, student() ? actor() : null);
    if (student()) {
      Set<Long> hidden = new HashSet<>(mapper.hiddenStudents(id, actor()));
      Set<Long> eligible = new HashSet<>();
      for (ChatMember m : mapper.candidates(g.getCourseId())) eligible.add(m.getStudentId());
      Long mentorId = learning.course(g.getCourseId()).getMentorId();
      for (ChatMember m : result) {
        m.setHidden(hidden.contains(m.getStudentId()));
        m.setHideAllowed(
            m.isActive()
                && !actor().equals(m.getStudentId())
                && !m.getStudentId().equals(mentorId)
                && eligible.contains(m.getStudentId()));
      }
    }
    return result;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void hidePeer(Long id, Long target) {
    if (!student()) denied();
    ChatGroup g = group(id, true, false, false);
    if (actor().equals(target)
        || target.equals(learning.course(g.getCourseId()).getMentorId())
        || mapper.isMember(id, target) != 1
        || mapper.eligible(g.getCourseId(), target) != 1)
      throw new ServiceException(
          400, "Choose another student in this discussion. Mentor messages stay visible.");
    mapper.hidePeer(id, actor(), target);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void showPeer(Long id, Long target) {
    if (!student()) denied();
    group(id, true, false, false);
    mapper.showPeer(id, actor(), target);
  }

  public List<ChatMember> candidates(Long id) {
    ChatGroup g = group(id, false, false, true);
    return mapper.candidates(g.getCourseId());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void addMember(Long id, Long studentId) {
    ChatGroup g = group(id, true, true, true);
    if (mapper.eligible(g.getCourseId(), studentId) != 1)
      throw new ServiceException(400, "Choose an active student enrolled in this course.");
    mapper.addMember(id, studentId);
    audit.insert(actor(), "CHAT_MEMBER_ADD", id + ":" + studentId, "SUCCESS");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void removeMember(Long id, Long studentId) {
    group(id, true, false, true);
    mapper.removeMember(id, studentId);
    audit.insert(actor(), "CHAT_MEMBER_REMOVE", id + ":" + studentId, "SUCCESS");
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> messages(Long id, Long after, Long before) {
    group(id, false, false, false);
    return batch(id, after, before);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> sync(Long id, Long after) {
    ChatGroup g = group(id, false, false, false);
    Map<String, Object> result = new HashMap<>();
    result.put("group", g);
    result.put("messages", batch(id, after, null));
    result.put(
        "hiddenStudentIds",
        student() ? mapper.hiddenStudents(id, actor()) : Collections.emptyList());
    return result;
  }

  private Map<String, Object> batch(Long id, Long after, Long before) {
    if ((after != null && after < 0)
        || (before != null && before < 1)
        || (after != null && before != null))
      throw new ServiceException(400, "Choose one valid message cursor.");
    // Capture the committed upper bound BEFORE reading rows. Hidden-only tails can advance safely.
    Long ceiling = mapper.latestMessageId(id);
    List<ChatMessage> rows =
        mapper.messages(id, after, before, student() ? actor() : null, ceiling);
    boolean more = rows.size() > 30;
    if (more) rows.remove(30);
    if (after == null) Collections.reverse(rows);
    if (!rows.isEmpty()) {
      List<Long> ids = new ArrayList<>();
      for (ChatMessage m : rows) ids.add(m.getId());
      Map<Long, List<ChatFile>> attached = new HashMap<>();
      for (ChatFile f : mapper.batchMessageFiles(ids))
        attached.computeIfAbsent(f.getMessageId(), key -> new ArrayList<>()).add(f);
      for (ChatMessage m : rows) {
        m.setFiles(attached.getOrDefault(m.getId(), Collections.emptyList()));
        m.setHideAllowed(
            student()
                && m.isStudentSender()
                && "student".equals(m.getSenderRole())
                && !actor().equals(m.getSenderId()));
      }
    }
    Long cursor =
        after != null && more
            ? rows.get(rows.size() - 1).getId()
            : Math.max(ceiling, after == null ? 0 : after);
    Map<String, Object> result = new HashMap<>();
    result.put("rows", rows);
    result.put("hasMore", more);
    result.put("nextCursor", cursor);
    return result;
  }

  private ChatMessage hydrate(Long id) {
    ChatMessage m = mapper.message(id);
    m.setFiles(mapper.messageFiles(id));
    return m;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ChatMessage send(Long id, SendInput b) {
    group(id, true, true, false);
    String text = b.getBody().trim();
    Set<Long> ids = new LinkedHashSet<>(b.getFileIds());
    if (ids.size() != b.getFileIds().size())
      throw new ServiceException(400, "Each attachment can be added once.");
    ChatMessage previous = mapper.byKey(id, actor(), b.getClientKey());
    if (previous != null) {
      List<Long> saved = new ArrayList<>();
      for (ChatFile f : mapper.messageFiles(previous.getId())) saved.add(f.getId());
      if (!text.equals(previous.getBody()) || !new HashSet<>(saved).equals(ids))
        throw new ServiceException(
            409, "This send request was already used. Refresh the discussion.");
      return hydrate(previous.getId());
    }
    filter.check(text, false);
    if (text.isEmpty() && ids.isEmpty())
      throw new ServiceException(400, "Write a message or add a file.");
    for (Long fid : ids) {
      ChatFile f = mapper.file(fid);
      if (f == null
          || !id.equals(f.getGroupId())
          || !actor().equals(f.getOwnerId())
          || f.getMessageId() != null)
        throw new ServiceException(
            403, "You can send only your own unsent attachments from this discussion.");
      filter.check(f.getOriginalName(), true);
      filter.check(f.getAltText(), true);
    }
    ChatMessage m = new ChatMessage();
    m.setGroupId(id);
    m.setSenderId(actor());
    m.setSenderRole(student() ? "student" : "mentor");
    m.setBody(text);
    m.setClientKey(b.getClientKey());
    mapper.insertMessage(m);
    for (Long fid : ids)
      if (mapper.attach(fid, m.getId()) != 1)
        throw new ServiceException(409, "An attachment has changed. Refresh before sending.");
    // Group writes are serialized: a committed cursor cannot skip an earlier uncommitted message.
    return hydrate(m.getId());
  }

  public ChatFile upload(Long id, MultipartFile file, String alt) {
    group(id, false, true, false);
    Long owner = actor();
    if (uploading.putIfAbsent(owner, Boolean.TRUE) != null)
      throw new ServiceException(
          429, "One upload at a time. Wait for your current file to finish.");
    try {
      if (mapper.draftCount(id, owner) >= 5)
        throw new ServiceException(400, "Send or remove your five pending attachments first.");
      String name = file == null ? null : file.getOriginalFilename();
      boolean audio = RecordingStorage.audioName(name);
      if (file == null || file.isEmpty() || file.getSize() > (audio ? 5L : 20L) * 1024 * 1024)
        throw new ServiceException(
            400, audio ? "Use an audio file up to 5 MB." : "Use a file up to 20 MB.");
      String description = alt == null ? "" : alt.trim();
      if (description.length() > 200)
        throw new ServiceException(400, "Use at most 200 characters for the image description.");
      filter.check(name, true);
      filter.check(description, true);
      // Validation and disk I/O do not hold a course/group row lock or a DB transaction.
      PrivateFileStorage.Stored saved = files.save(file);
      try {
        if (saved.mime.startsWith("image/") && description.isEmpty())
          throw new ServiceException(400, "Add a short image description before uploading.");
        filter.check(saved.name, true);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setIsolationLevel(
            org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(
            status -> {
              // Membership, course publication, mentor assignment, group status and draft cap may
              // have changed.
              group(id, true, true, false);
              if (mapper.draftCount(id, owner) >= 5)
                throw new ServiceException(
                    400, "Send or remove your five pending attachments first.");
              ChatFile f = new ChatFile();
              f.setGroupId(id);
              f.setOwnerId(owner);
              f.setStorageKey(saved.key);
              f.setOriginalName(saved.name);
              f.setMimeType(saved.mime);
              f.setSizeBytes(saved.size);
              f.setAltText(description);
              mapper.insertFile(f);
              return f;
            });
      } catch (RuntimeException | Error failure) {
        files.discard(saved.key);
        throw failure;
      }
    } finally {
      uploading.remove(owner);
    }
  }

  public List<ChatFile> drafts(Long id) {
    group(id, false, false, false);
    return mapper.drafts(id, actor());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void deleteFile(Long id) {
    ChatFile initial = mapper.file(id);
    if (initial == null) throw new ServiceException(404, "This attachment is unavailable.");
    group(initial.getGroupId(), true, false, false);
    ChatFile f = mapper.file(id);
    if (f == null || !actor().equals(f.getOwnerId()) || f.getMessageId() != null)
      throw new ServiceException(403, "Only your own unsent attachments can be removed.");
    mapper.deleteFile(id);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            files.discard(f.getStorageKey());
          }
        });
  }

  public ChatFile readFile(Long id) {
    ChatFile f = mapper.file(id);
    if (f == null) throw new ServiceException(404, "This attachment is unavailable.");
    group(f.getGroupId(), false, false, false);
    if (f.getMessageId() == null && !actor().equals(f.getOwnerId())) denied();
    if (student()
        && f.getMessageId() != null
        && mapper.hiddenMessage(f.getMessageId(), actor()) != 0) denied();
    return f;
  }
}
