package com.upself.biz.knowledge.service;

import com.upself.biz.knowledge.domain.*;
import com.upself.biz.knowledge.dto.KnowledgeBodies.*;
import java.util.List;

public interface IKnowledgeService {
    void verifyStudent();
    void verifyMentor();
    List<KnowledgeQuestion> ownQuestions(Long courseId);
    List<KnowledgeQuestion> mentorQuestions(Long courseId);
    Long create(Long courseId, QuestionInput body);
    void update(Long id, QuestionInput body);
    void delete(Long id, Integer version);
    KnowledgeAnswer answer(Long id, AnswerInput body);
    void verifyAnswers(Long id);
    List<KnowledgeAnswer> answers(Long id);
}
