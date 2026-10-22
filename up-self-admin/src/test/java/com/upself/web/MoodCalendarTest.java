package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;

import com.upself.biz.mood.service.MoodCalendar;
import java.time.*;
import org.junit.jupiter.api.Test;

class MoodCalendarTest {
  MoodCalendar at(String instant) {
    return new MoodCalendar(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
  }

  @Test
  void sundayBeforeMalaysiaMidnightBelongsToPreviousMonday() {
    assertEquals("2026-10-19", at("2026-10-25T15:59:59Z").weekStart().toString());
  }

  @Test
  void malaysiaMondayStartsEightHoursBeforeUtcMonday() {
    assertEquals("2026-10-26", at("2026-10-25T16:00:00Z").weekStart().toString());
  }

  @Test
  void weekSurvivesCalendarYearChange() {
    assertEquals("2024-12-30", at("2025-01-05T12:00:00Z").weekStart().toString());
  }

  @Test
  void aLeapDayRemainsInItsMondayWeek() {
    assertEquals("2024-02-26", at("2024-02-29T00:00:00Z").weekStart().toString());
  }
}
