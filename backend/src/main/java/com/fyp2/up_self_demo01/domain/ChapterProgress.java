package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chapter_progress")
public class ChapterProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long chapterId;
    private Integer isCompleted;

    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}