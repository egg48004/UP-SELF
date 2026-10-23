package com.upself.biz.mood.service;

import com.upself.biz.mood.domain.*;
import com.upself.biz.mood.dto.MoodBodies.*;
import com.upself.biz.mood.dto.MoodViews.*;
import java.util.List;

public interface IMoodService {
  String NOTICE_VERSION = "2026-10-22-v1";
  Current current();

  Current acknowledge(Acknowledgment body);

  Current record(EntryInput body);

  void verifyStudent();

  List<MoodEntry> ownHistory();

  Trend ownTrend(int weeks);

  List<MoodPerson> students(String keyword);

  MoodPerson student(Long studentId);

  void verifyMentorAccess(Long studentId);

  List<MoodPoint> mentorHistory(Long studentId);

  Trend mentorTrend(Long studentId, int weeks);
}
