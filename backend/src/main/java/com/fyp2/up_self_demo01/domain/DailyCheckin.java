package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_checkin")
public class DailyCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private LocalDate checkinDate;

    private LocalDateTime createdAt;
}