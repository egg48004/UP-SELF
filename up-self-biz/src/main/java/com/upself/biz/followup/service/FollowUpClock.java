package com.upself.biz.followup.service;

import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class FollowUpClock {
  public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

  public LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }
}
