package com.upself.biz.mentorship.service;

import com.upself.biz.mentorship.domain.MentorNote;
import com.upself.biz.mentorship.dto.NoteBodies.*;
import java.util.List;

public interface IMentorNoteService {
  Student verifyStudent(Long studentId);

  List<MentorNote> list(Long studentId);

  Long create(Long studentId, Create body);

  void edit(Long studentId, Long id, Update body);
}
