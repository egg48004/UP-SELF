package com.upself.biz.knowledge.mapper;

import com.upself.biz.knowledge.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface KnowledgeMapper {
    List<KnowledgeQuestion> ownQuestions(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    List<KnowledgeQuestion> mentorQuestions(@Param("mentorId") Long mentorId, @Param("courseId") Long courseId);
    KnowledgeQuestion question(@Param("id") Long id);
    KnowledgeQuestion lockQuestion(@Param("id") Long id);
    int canAnswer(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    int insert(KnowledgeQuestion question);
    int update(KnowledgeQuestion question);
    int delete(@Param("id") Long id);
    int answerCount(@Param("id") Long id);
    int saveAnswer(KnowledgeAnswer answer);
    KnowledgeAnswer ownAnswer(@Param("questionId") Long questionId, @Param("studentId") Long studentId);
    List<KnowledgeAnswer> answers(@Param("questionId") Long questionId, @Param("mentorId") Long mentorId);
}
