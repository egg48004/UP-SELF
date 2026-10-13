package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_bookmark")
public class CourseBookmark {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;

    private LocalDateTime createdAt;
}