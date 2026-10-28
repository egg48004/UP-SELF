package com.upself.biz.chat.domain;
import java.util.List;
import lombok.Data;
@Data public class ChatMessage {
 private Long id, groupId, senderId;
 private String body, clientKey, senderName;
 private java.util.Date createTime;
 private List<ChatFile> files;
}
