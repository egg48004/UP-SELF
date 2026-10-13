package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_preferences")
public class UserPreferences {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String fontSize;
    private Integer highContrast;

    // 对应 MySQL JSON 字段，第一版暂时使用 String
    private String readingPreferences;

    private LocalDateTime updatedAt;
}