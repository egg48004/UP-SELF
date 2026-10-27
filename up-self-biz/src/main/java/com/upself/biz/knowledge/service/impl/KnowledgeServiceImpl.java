package com.upself.biz.knowledge.service.impl;

import com.upself.biz.knowledge.domain.*;
import com.upself.biz.knowledge.dto.KnowledgeBodies.*;
import com.upself.biz.knowledge.mapper.KnowledgeMapper;
import com.upself.biz.knowledge.service.IKnowledgeService;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class KnowledgeServiceImpl implements IKnowledgeService {
    @Autowired private KnowledgeMapper mapper;
    @Autowired private LearningMapper learning;
    @Autowired private SysAuditMapper audit;

    private Long actor() { return SecurityUtils.getLoginUser().getUserId(); }
    private void role(String expected) {
        if (!expected.equals(SecurityUtils.getLoginUser().getRoleKey()))
            throw new ServiceException(403, "This action is not available for your role.");
    }
    public void verifyStudent() { role("student"); }
    public void verifyMentor() { role("mentor"); }
    public List<KnowledgeQuestion> ownQuestions(Long courseId) {
        verifyStudent(); return mapper.ownQuestions(actor(), courseId);
    }
    public List<KnowledgeQuestion> mentorQuestions(Long courseId) {
        verifyMentor(); return mapper.mentorQuestions(actor(), courseId);
    }
    private Course ownedCourse(Long id, boolean lock) {
        verifyMentor();
        Course course = lock ? learning.lockCourse(id) : learning.course(id);
        if (course == null || !actor().equals(course.getMentorId()))
            throw new ServiceException(403, "You can only manage questions for your assigned courses.");
        return course;
    }
    private void writable(Course course) {
        if ("ARCHIVED".equals(course.getStatus()))
            throw new ServiceException(409, "Archived courses are read-only.");
    }
    private KnowledgeQuestion find(Long id) {
        KnowledgeQuestion q = mapper.question(id);
        if (q == null) throw new ServiceException(404, "This question is unavailable.");
        return q;
    }
    private String text(QuestionInput body) {
        String value = body.getQuestionText() == null ? "" : body.getQuestionText().trim();
        if (value.isEmpty() || value.length() > 500)
            throw new ServiceException(400, "Enter a question using 1 to 500 characters.");
        if (!"DRAFT".equals(body.getStatus()) && !"PUBLISHED".equals(body.getStatus()) && !"ARCHIVED".equals(body.getStatus()))
            throw new ServiceException(400, "Choose Draft, Published or Archived.");
        return value;
    }
    private void version(KnowledgeQuestion question, Integer expected) {
        if (question == null) throw new ServiceException(404, "This question is unavailable.");
        if (expected == null || !expected.equals(question.getVersion()))
            throw new ServiceException(409, "This question has changed. Refresh and try again.");
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long create(Long courseId, QuestionInput body) {
        Course course = ownedCourse(courseId, true); writable(course);
        KnowledgeQuestion q = new KnowledgeQuestion();
        q.setCourseId(courseId); q.setQuestionText(text(body)); q.setStatus(body.getStatus()); q.setCreatedBy(actor());
        mapper.insert(q); audit.insert(actor(), "KNOWLEDGE_CREATE", q.getId().toString(), "SUCCESS");
        return q.getId();
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(Long id, QuestionInput body) {
        verifyMentor(); KnowledgeQuestion previous = find(id);
        Course course = ownedCourse(previous.getCourseId(), true); writable(course);
        KnowledgeQuestion q = mapper.lockQuestion(id); version(q, body.getVersion());
        String value = text(body);
        if (!value.equals(q.getQuestionText()) && mapper.answerCount(id) > 0)
            throw new ServiceException(409, "This question has answers. Archive it and add a new question to change the wording.");
        q.setQuestionText(value); q.setStatus(body.getStatus());
        if (mapper.update(q) != 1) throw new ServiceException(409, "Refresh the question and try again.");
        audit.insert(actor(), "KNOWLEDGE_UPDATE", id.toString(), "SUCCESS");
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id, Integer expected) {
        verifyMentor(); KnowledgeQuestion previous = find(id);
        Course course = ownedCourse(previous.getCourseId(), true); writable(course);
        KnowledgeQuestion q = mapper.lockQuestion(id); version(q, expected);
        if (mapper.answerCount(id) > 0) throw new ServiceException(409, "This question has answers. Archive it to keep the responses.");
        mapper.delete(id); audit.insert(actor(), "KNOWLEDGE_DELETE", id.toString(), "SUCCESS");
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KnowledgeAnswer answer(Long id, AnswerInput body) {
        verifyStudent(); KnowledgeQuestion previous = find(id);
        // All question writes and enrollment/mentor changes take the course lock first.
        Course course = learning.lockCourse(previous.getCourseId());
        if (course == null || mapper.canAnswer(actor(), previous.getCourseId()) == 0)
            throw new ServiceException(403, "This question is not available in your joined, published courses.");
        KnowledgeQuestion q = mapper.lockQuestion(id); version(q, body.getQuestionVersion());
        if (!"PUBLISHED".equals(q.getStatus())) throw new ServiceException(409, "This question is no longer available. Refresh your questions.");
        if (!"YES".equals(body.getChoice()) && !"NO".equals(body.getChoice()) && !"NOT_SURE".equals(body.getChoice()))
            throw new ServiceException(400, "Choose Yes, No or Not sure.");
        KnowledgeAnswer a = new KnowledgeAnswer(); a.setQuestionId(id); a.setStudentId(actor());
        a.setChoice(body.getChoice()); a.setQuestionVersion(q.getVersion()); mapper.saveAnswer(a);
        audit.insert(actor(), "KNOWLEDGE_ANSWER", id.toString(), "SUCCESS");
        return mapper.ownAnswer(id, actor());
    }
    public void verifyAnswers(Long id) {
        verifyMentor(); Course c = ownedCourse(find(id).getCourseId(), false);
        if (!"PUBLISHED".equals(c.getStatus()))
            throw new ServiceException(403, "Student answers are available for your current published courses only.");
    }
    public List<KnowledgeAnswer> answers(Long id) {
        verifyMentor(); return mapper.answers(id, actor());
    }
}
