package com.upself.biz.followup.service;

import com.upself.biz.followup.domain.FollowUpReminder;
import java.util.*;

public interface IFollowUpService {
  void mentor();

  void refreshCurrent();

  void sync(Long mentorId);

  List<FollowUpReminder> list(String status, Long studentId);

  Map<String, Object> summary();

  void review(Long id, Integer version);
}
