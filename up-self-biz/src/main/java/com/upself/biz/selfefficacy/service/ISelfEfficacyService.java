package com.upself.biz.selfefficacy.service;

import com.upself.biz.selfefficacy.domain.SelfEfficacyEntry;
import com.upself.biz.selfefficacy.dto.SelfEfficacyBodies.*;
import com.upself.biz.selfefficacy.dto.SelfEfficacyViews.*;
import java.util.List;

public interface ISelfEfficacyService {
    String NOTICE_VERSION = "2026-10-23-v1";
    Current current();
    Current acknowledge(Acknowledgment body);
    Current record(EntryInput body);
    void verifyStudent();
    List<SelfEfficacyEntry> ownHistory();
    Trend ownTrend(int weeks);
    Person student(Long studentId);
    void verifyMentorAccess(Long studentId);
    List<Point> mentorHistory(Long studentId);
    Trend mentorTrend(Long studentId, int weeks);
}
