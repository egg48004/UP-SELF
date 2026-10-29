package com.upself.biz.followup.mapper;

import com.upself.biz.followup.domain.FollowUpReminder;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface FollowUpMapper {
  List<Long> mentors();

  Long lockMentor(Long id);

  List<FollowUpReminder> moods(
      @Param("mentor") Long mentor,
      @Param("week") String week,
      @Param("previous") String previous,
      @Param("notice") String notice);

  List<FollowUpReminder> activity(@Param("mentor") Long mentor, @Param("cutoff") String cutoff);

  List<FollowUpReminder> overdue(@Param("mentor") Long mentor, @Param("cutoff") String cutoff);

  FollowUpReminder existing(FollowUpReminder r);

  int insert(FollowUpReminder r);

  int reactivate(FollowUpReminder r);

  int endMissing(@Param("mentor") Long mentor, @Param("ids") List<Long> ids);

  List<FollowUpReminder> list(
      @Param("mentor") Long mentor,
      @Param("status") String status,
      @Param("studentId") Long studentId);

  FollowUpReminder own(@Param("mentor") Long mentor, @Param("id") Long id);

  int review(@Param("mentor") Long mentor, @Param("id") Long id, @Param("version") Integer version);
}
