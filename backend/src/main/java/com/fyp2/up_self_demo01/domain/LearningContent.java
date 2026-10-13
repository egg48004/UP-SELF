package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_content")
public class LearningContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;
    private String title;
    private String contentType;
    private String contentText;
    private String resourceUrl;
    private Integer sortOrder;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer deleted;
}