package com.upself.biz.chat.domain;
import lombok.Data;
@Data public class ChatGroup {
 private Long id, courseId, createdBy;
 private String title, status;
 private Integer version, memberCount;
 private java.util.Date createTime;
}
