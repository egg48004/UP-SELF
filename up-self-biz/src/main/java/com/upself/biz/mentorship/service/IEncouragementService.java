package com.upself.biz.mentorship.service;

import com.upself.biz.mentorship.domain.EncouragementMessage;
import com.upself.biz.mentorship.dto.EncouragementBodies.*;
import java.util.List;

public interface IEncouragementService {
  void verifyMentor();

  void verifyStudent();

  List<Recipient> students(String keyword);

  Recipient student(Long id);

  EncouragementMessage send(Send body);

  List<EncouragementMessage> history();

  SentDetails sent(Long id);

  List<InboxMessage> inbox();

  InboxMessage received(Long id);
}
