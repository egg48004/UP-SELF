package com.upself.biz.learning.mapper;

import com.upself.biz.learning.dto.LearningStatistics;
import com.upself.biz.learning.dto.ProgressViews.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProgressMapper {
  List<Item> chapters(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

  int visit(@Param("studentId") Long studentId, @Param("chapterId") Long chapterId);

  int complete(
      @Param("studentId") Long studentId,
      @Param("chapterId") Long chapterId,
      @Param("completed") boolean completed);

  Item resume(@Param("studentId") Long studentId);

  List<Item> history(@Param("studentId") Long studentId);

  List<Overview> overview(
      @Param("mentorId") Long mentorId,
      @Param("keyword") String keyword,
      @Param("courseId") Long courseId,
      @Param("noticeVersion") String noticeVersion);

  LearningStatistics.Totals statistics(@Param("studentId") Long studentId);

  List<LearningStatistics.CourseRow> statisticsCourses(@Param("studentId") Long studentId);
}
