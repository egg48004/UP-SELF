package com.upself.biz.selfefficacy.mapper;

import com.upself.biz.selfefficacy.domain.SelfEfficacyEntry;
import com.upself.biz.selfefficacy.dto.SelfEfficacyViews.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SelfEfficacyMapper {
    Long lockStudent(@Param("studentId") Long studentId);
    String noticeVersion(@Param("studentId") Long studentId);
    void acceptNotice(@Param("studentId") Long studentId, @Param("version") String version);
    List<Person> viewers(@Param("studentId") Long studentId);
    Person student(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);
    int canView(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);
    SelfEfficacyEntry latest(@Param("studentId") Long studentId);
    void insert(SelfEfficacyEntry entry);
    List<SelfEfficacyEntry> ownHistory(@Param("studentId") Long studentId, @Param("version") String version);
    List<Point> ownPoints(@Param("studentId") Long studentId, @Param("version") String version, @Param("start") String start, @Param("end") String end);
    List<Point> mentorHistory(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId, @Param("version") String version);
    List<Point> mentorPoints(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId, @Param("version") String version, @Param("start") String start, @Param("end") String end);
}
