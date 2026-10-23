package com.upself.biz.selfefficacy.dto;

import com.upself.biz.selfefficacy.domain.SelfEfficacyEntry;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

public class SelfEfficacyViews {
    @Data
    public static class Person {
        private Long id;
        private String displayName;
        private String username;
    }
    @Data
    public static class Instrument {
        private String version;
        private String name;
        private String instruction;
        private List<String> questions;
        private List<String> responses;
        private String sourceUrl;
        private String attribution;
        private String license;
        private String licenseUrl;
    }
    @Data
    public static class Current {
        private String today;
        private String timeZone;
        private int intervalDays;
        private boolean acknowledged;
        private String noticeVersion;
        private boolean available;
        private String nextAvailableDate;
        private SelfEfficacyEntry latest;
        private List<Person> viewers;
        private Instrument instrument;
    }
    // Explicit mentor projection: never expose individual answers or student-only fields.
    @Data
    public static class Point {
        private String assessmentDate;
        private BigDecimal score;
        private String instrumentVersion;
    }
    @Data
    public static class Trend {
        private String startDate;
        private String endDate;
        private String timeZone;
        private List<Point> points;
    }
}
