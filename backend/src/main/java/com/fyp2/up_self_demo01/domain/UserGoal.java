package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_goal")
public class UserGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private String title;

    private String description;

    private LocalDate targetDate;

    /**
     * 目标状态，例如 active、completed
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 目标完成时间
     */
    private LocalDateTime completedAt;
}