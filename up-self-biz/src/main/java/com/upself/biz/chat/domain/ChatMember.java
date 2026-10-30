package com.upself.biz.chat.domain;

import lombok.Data;

@Data
public class ChatMember {
  private Long studentId;
  private String nickName, userName;
  private boolean active, hidden, hideAllowed;
}
