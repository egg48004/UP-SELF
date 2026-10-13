package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mood_record")
public class MoodRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /**
     * 心情状态，例如 happy、calm、tired
     */
    private String moodValue;

    /**
     * 学生填写的心情备注
     */
    private String note;

    /**
     * 心情记录时间
     */
    private LocalDateTime recordedAt;
}