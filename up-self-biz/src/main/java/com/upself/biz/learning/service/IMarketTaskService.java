package com.upself.biz.learning.service;

import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.MarketBodies.*;
import java.util.*;

public interface IMarketTaskService {
  void listAccess(Long courseId);

  List<MarketTask> list(Long courseId);

  MarketTask detail(Long id);

  Long create(Content body);

  void update(Long id, Content body);

  void status(Long id, State body);

  Long copy(Long id);

  MarketAttempt choose(Long id, Choice body);

  void reflection(Long id, Long attemptId, Reflection body);

  Long historyAccess(Long id, Long studentId);

  List<MarketAttempt> history(Long id, Long studentId);

  void mentorAccess(Long id);

  List<Map<String, Object>> participants(Long id);
}
