package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private String instructions;
    private LocalDateTime deadline;
    private String status;
    private Long createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer deleted;
}