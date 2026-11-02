package com.upself.biz.mentorship.mapper;

import com.upself.biz.mentorship.domain.EncouragementMessage;
import com.upself.biz.mentorship.dto.EncouragementBodies.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface EncouragementMapper {
  int activeActor(@Param("id") Long id, @Param("roleId") Long roleId);

  List<Recipient> students(@Param("mentorId") Long mentorId, @Param("keyword") String keyword);

  Recipient student(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  List<Long> witnessCourses(@Param("mentorId") Long mentorId, @Param("ids") List<Long> ids);

  List<Actor> lockActors(@Param("ids") List<Long> ids);

  List<Long> eligibleStudents(
      @Param("mentorId") Long mentorId,
      @Param("ids") List<Long> ids,
      @Param("courseIds") List<Long> courseIds);

  EncouragementMessage byKey(
      @Param("mentorId") Long mentorId, @Param("clientKey") String clientKey);

  List<Long> recipientIds(Long messageId);

  int insert(EncouragementMessage message);

  int insertRecipients(@Param("messageId") Long messageId, @Param("ids") List<Long> ids);

  List<EncouragementMessage> history(Long mentorId);

  EncouragementMessage own(@Param("mentorId") Long mentorId, @Param("id") Long id);

  List<Recipient> recipients(@Param("mentorId") Long mentorId, @Param("id") Long id);

  List<InboxMessage> inbox(Long studentId);

  InboxMessage received(@Param("studentId") Long studentId, @Param("id") Long id);
}
