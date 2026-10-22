package com.upself.biz.mood.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Component;

@Component
public class MoodCalendar {
  public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
  private final Clock clock;

  public MoodCalendar() {
    this(Clock.systemUTC());
  }

  public MoodCalendar(Clock clock) {
    this.clock = clock;
  }

  public LocalDate weekStart() {
    return LocalDate.now(clock.withZone(ZONE))
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
}
