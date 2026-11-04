package com.upself.biz.learning.service;

import com.upself.biz.learning.domain.BadgeCode;
import com.upself.biz.learning.dto.BadgeView;
import java.util.List;

public interface IBadgeService {
  List<BadgeView> mine();
  String award(BadgeCode code);
}
