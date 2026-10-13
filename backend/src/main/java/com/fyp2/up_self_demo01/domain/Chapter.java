package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chapter")
public class Chapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String description;
    private Integer sortOrder;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer deleted;
}