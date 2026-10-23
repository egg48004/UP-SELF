package com.upself.biz.selfefficacy.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class SelfEfficacyEntry {
    private Long id;
    private Long studentId;
    private String assessmentDate;
    private String instrumentVersion;
    private Integer answerOne;
    private Integer answerTwo;
    private Integer answerThree;
    private BigDecimal score;
    private Date createTime;
}
