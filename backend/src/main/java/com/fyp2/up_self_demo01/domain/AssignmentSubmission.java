package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assignment_submission")
public class AssignmentSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;
    private Long studentId;
    private Integer attemptNo;

    private String answerText;
    private String status;
    private LocalDateTime submittedAt;

    private BigDecimal grade;
    private String feedback;
    private Long gradedBy;
    private LocalDateTime gradedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}