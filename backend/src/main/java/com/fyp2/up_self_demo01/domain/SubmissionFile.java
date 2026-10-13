package com.fyp2.up_self_demo01.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_file")
public class SubmissionFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    /**
     * 文件 MIME 类型，例如 application/pdf
     */
    private String mimeType;

    /**
     * 文件上传时间
     */
    private LocalDateTime uploadedAt;
}