package com.upself.biz.mentorship.service.impl;

import com.upself.biz.chat.service.ChatContentFilter;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.mentorship.domain.EncouragementMessage;
import com.upself.biz.mentorship.dto.EncouragementBodies.*;
import com.upself.biz.mentorship.mapper.EncouragementMapper;
import com.upself.biz.mentorship.service.IEncouragementService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EncouragementServiceImpl implements IEncouragementService {
  @Autowired private EncouragementMapper mapper;
  @Autowired private LearningMapper learning;
  @Autowired private ChatContentFilter filter;
  @Autowired private SysAuditMapper audit;

  private Long actor(String role) {
    if (!role.equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "These messages are not available to you.");
    return SecurityUtils.getUserId();
  }

  public void verifyMentor() {
    if (mapper.activeActor(actor("mentor"), 20L) != 1)
      throw new ServiceException(403, "These messages are not available to you.");
  }

  public void verifyStudent() {
    if (mapper.activeActor(actor("student"), 10L) != 1)
      throw new ServiceException(403, "These messages are not available to you.");
  }

  // Controllers verify live account scope before startPage, leaving its next query for the list.
  public List<Recipient> students(String keyword) {
    return mapper.students(actor("mentor"), keyword == null ? "" : keyword.trim());
  }

  public Recipient student(Long id) {
    verifyMentor();
    Recipient person = mapper.student(actor("mentor"), id);
    if (person == null) throw unavailableStudent();
    return person;
  }

  private ServiceException unavailableStudent() {
    return new ServiceException(
        403, "Choose only active students currently assigned to you. Refresh the student list.");
  }

  private EncouragementMessage retry(EncouragementMessage previous, String text, List<Long> ids) {
    if (!text.equals(previous.getBody()) || !ids.equals(mapper.recipientIds(previous.getId())))
      throw new ServiceException(
          409,
          "This send request was already used for a different message. Check sent messages before"
              + " trying again.");
    return mapper.own(actor("mentor"), previous.getId());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public EncouragementMessage send(Send body) {
    verifyMentor();
    Long sender = actor("mentor");
    String text = body.getBody() == null ? "" : body.getBody().trim();
    if (text.isEmpty() || text.length() > 2000)
      throw new ServiceException(400, "Write a message using 1 to 2000 characters.");
    if (body.getStudentIds() == null
        || body.getStudentIds().isEmpty()
        || body.getStudentIds().size() > 100
        || body.getStudentIds().stream().anyMatch(id -> id == null || id < 1))
      throw new ServiceException(400, "Choose between 1 and 100 students.");
    List<Long> ids = new ArrayList<>(new TreeSet<>(body.getStudentIds()));
    EncouragementMessage previous = mapper.byKey(sender, body.getClientKey());
    if (previous != null) return retry(previous, text, ids);
    // Lock witness courses first, in the same order as course/member administration.
    List<Long> courseIds = mapper.witnessCourses(sender, ids);
    Collections.sort(courseIds);
    for (Long courseId : courseIds) learning.lockCourse(courseId);
    List<Long> accountIds = new ArrayList<>(ids);
    accountIds.add(sender);
    Collections.sort(accountIds);
    List<Actor> actors = mapper.lockActors(accountIds);
    if (actors.stream()
        .noneMatch(
            u ->
                sender.equals(u.getId())
                    && Long.valueOf(20).equals(u.getRoleId())
                    && "0".equals(u.getStatus()))) throw unavailableStudent();
    // Another request with the same key may have committed while the course/account lock waited.
    previous = mapper.byKey(sender, body.getClientKey());
    if (previous != null) return retry(previous, text, ids);
    if (courseIds.isEmpty() || !ids.equals(mapper.eligibleStudents(sender, ids, courseIds)))
      throw unavailableStudent();
    filter.check(text, false);
    EncouragementMessage message = new EncouragementMessage();
    message.setMentorId(sender);
    message.setClientKey(body.getClientKey());
    message.setBody(text);
    message.setRecipientCount(ids.size());
    mapper.insert(message);
    if (mapper.insertRecipients(message.getId(), ids) != ids.size())
      throw new ServiceException(
          409, "Student availability changed. Refresh before sending again.");
    audit.insert(sender, "ENCOURAGEMENT_SEND", message.getId().toString(), "SUCCESS");
    return mapper.own(sender, message.getId());
  }

  public List<EncouragementMessage> history() {
    return mapper.history(actor("mentor"));
  }

  public SentDetails sent(Long id) {
    verifyMentor();
    Long sender = actor("mentor");
    EncouragementMessage message = mapper.own(sender, id);
    if (message == null) throw new ServiceException(404, "This message is unavailable.");
    SentDetails result = new SentDetails();
    result.setMessage(message);
    result.setRecipients(mapper.recipients(sender, id));
    return result;
  }

  public List<InboxMessage> inbox() {
    return mapper.inbox(actor("student"));
  }

  public InboxMessage received(Long id) {
    verifyStudent();
    InboxMessage message = mapper.received(actor("student"), id);
    if (message == null) throw new ServiceException(404, "This message is unavailable.");
    return message;
  }
}
