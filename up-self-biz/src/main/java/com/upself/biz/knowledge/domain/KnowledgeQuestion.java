package com.upself.biz.knowledge.domain;

import lombok.Data;

@Data
public class KnowledgeQuestion {
    private Long id, courseId, createdBy;
    private String questionText, courseTitle, courseStatus, status, choice;
    private Integer version, answerCount;
    private java.util.Date createTime, updateTime, answeredAt;
}
