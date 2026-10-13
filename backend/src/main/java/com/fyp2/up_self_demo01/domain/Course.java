package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private String difficulty;
    private Integer estimatedMinutes;
    private String status;
    private Long createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer deleted;
}