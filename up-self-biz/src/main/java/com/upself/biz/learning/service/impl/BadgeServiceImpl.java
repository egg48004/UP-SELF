package com.upself.biz.learning.service.impl;

import com.upself.biz.learning.domain.BadgeCode;
import com.upself.biz.learning.dto.BadgeView;
import com.upself.biz.learning.mapper.BadgeMapper;
import com.upself.biz.learning.service.IBadgeService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BadgeServiceImpl implements IBadgeService {
  @Autowired private BadgeMapper mapper;

  private Long studentId() {
    if (!"student".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Badges are available to students only.");
    return SecurityUtils.getLoginUser().getUserId();
  }

  public List<BadgeView> mine() {
    List<BadgeView> saved = mapper.own(studentId());
    Map<String, Date> dates = new HashMap<>();
    for (BadgeView badge : saved) dates.put(badge.getCode(), badge.getAwardedAt());
    List<BadgeView> result = new ArrayList<>();
    for (BadgeCode code : BadgeCode.values()) {
      BadgeView badge = new BadgeView();
      badge.setCode(code.name());
      badge.setAwardedAt(dates.get(code.name()));
      result.add(badge);
    }
    return result;
  }

  // Every caller already owns the successful business transaction. A failed award
  // must roll that operation back; only an existing unique award is harmless.
  @Transactional(propagation = Propagation.MANDATORY)
  public String award(BadgeCode code) {
    Long owner = studentId();
    Objects.requireNonNull(code, "Badge code is required");
    try {
      mapper.insert(owner, code);
      return code.name();
    } catch (DuplicateKeyException alreadyAwarded) {
      return null;
    }
  }
}
