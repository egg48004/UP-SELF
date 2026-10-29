package com.upself.biz.mentorship.mapper;

import com.upself.biz.mentorship.domain.MentorNote;
import com.upself.biz.mentorship.dto.NoteBodies.Student;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MentorNoteMapper {
  Student student(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  List<MentorNote> list(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  MentorNote own(
      @Param("id") Long id, @Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

  int insert(MentorNote note);

  int edit(
      @Param("id") Long id,
      @Param("mentorId") Long mentorId,
      @Param("studentId") Long studentId,
      @Param("version") Integer version,
      @Param("content") String content);
}
