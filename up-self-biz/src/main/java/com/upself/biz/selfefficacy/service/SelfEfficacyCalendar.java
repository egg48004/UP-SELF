package com.upself.biz.selfefficacy.service;

import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class SelfEfficacyCalendar {
    public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private final Clock clock;
    public SelfEfficacyCalendar() { this(Clock.systemUTC()); }
    public SelfEfficacyCalendar(Clock clock) { this.clock = clock; }
    public LocalDate today() { return LocalDate.now(clock.withZone(ZONE)); }
}
