package com.upself.biz.knowledge.domain;

import lombok.Data;

@Data
public class KnowledgeAnswer {
    private Long id, questionId, studentId;
    private String studentName, username, choice;
    private Integer questionVersion;
    private java.util.Date createTime, updateTime;
}
