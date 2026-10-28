package com.upself.biz.chat.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
@Data public class ChatFile {
 private Long id, groupId, ownerId, messageId, sizeBytes;
 @JsonIgnore private String storageKey;
 private String originalName, mimeType, altText;
}
