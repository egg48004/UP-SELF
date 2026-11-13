package com.upself.web;
import static org.junit.jupiter.api.Assertions.*;
import com.upself.biz.learning.service.DailyCheckInService;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
class DailyCheckInTest {
 private Map<String,Object> view(String today,String... dates){List<LocalDate> ds=new ArrayList<>();for(String d:dates)ds.add(LocalDate.parse(d));return DailyCheckInService.view(LocalDate.parse(today),ds);}
 @Test void first(){assertEquals(1,view("2026-01-01","2026-01-01").get("streak"));}
 @Test void yesterdayRetained(){assertEquals(2,view("2026-01-02","2026-01-01","2025-12-31").get("streak"));}
 @Test void missedDay(){assertEquals(0,view("2026-01-03","2026-01-01").get("streak"));}
 @Test void restart(){assertEquals(1,view("2026-01-03","2026-01-03","2026-01-01").get("streak"));}
 @Test void leapDay(){assertEquals(3,view("2024-03-01","2024-03-01","2024-02-29","2024-02-28").get("streak"));}
 @Test void historyBounded(){assertEquals(Collections.singletonList("2026-01-31"),view("2026-01-31","2026-01-31","2026-01-01").get("history"));}
}
