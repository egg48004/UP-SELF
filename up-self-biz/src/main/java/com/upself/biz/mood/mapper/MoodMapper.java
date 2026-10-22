package com.upself.biz.mood.mapper;

import com.upself.biz.mood.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MoodMapper {
  Long lockStudent(@Param("studentId") Long studentId);

  String noticeVersion(@Param("studentId") Long studentId);

  int acceptNotice(@Param("studentId") Long studentId, @Param("version") String version);

  MoodEntry entry(@Param("studentId") Long studentId, @Param("weekStart") String weekStart);

  int insert(MoodEntry entry);

  List<MoodPerson> viewers(@Param("studentId") Long studentId);

  List<MoodPerson> students(@Param("mentorId") Long mentorId, @Param("keyword") String keyword);

  MoodPerson student(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  int canView(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  List<MoodEntry> ownHistory(@Param("studentId") Long studentId, @Param("version") String version);

  List<MoodPoint> ownPoints(
      @Param("studentId") Long studentId,
      @Param("version") String version,
      @Param("start") String start,
      @Param("end") String end);

  List<MoodPoint> mentorHistory(
      @Param("mentorId") Long mentorId,
      @Param("studentId") Long studentId,
      @Param("version") String version);

  List<MoodPoint> mentorPoints(
      @Param("mentorId") Long mentorId,
      @Param("studentId") Long studentId,
      @Param("version") String version,
      @Param("start") String start,
      @Param("end") String end);
}
