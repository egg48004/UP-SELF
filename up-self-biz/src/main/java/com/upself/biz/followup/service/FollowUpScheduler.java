package com.upself.biz.followup.service;

import com.upself.biz.followup.mapper.FollowUpMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class FollowUpScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(FollowUpScheduler.class);
  @Autowired private FollowUpMapper mapper;
  @Autowired private IFollowUpService service;

  @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kuala_Lumpur")
  @EventListener(ApplicationReadyEvent.class)
  public void check() {
    for (Long id : mapper.mentors()) {
      try {
        service.sync(id);
      } catch (Exception e) {
        LOG.error("Follow-up refresh failed for mentor {}", id, e);
      }
    }
  }
}
