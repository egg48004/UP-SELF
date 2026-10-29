package com.upself.system.service.impl;

import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.domain.AccessibilityDefaults;
import com.upself.system.mapper.AccessibilityMapper;
import com.upself.system.mapper.SysAuditMapper;
import com.upself.system.service.IAccessibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessibilityServiceImpl implements IAccessibilityService {
  @Autowired private AccessibilityMapper mapper;
  @Autowired private SysAuditMapper audit;

  public AccessibilityDefaults read() {
    AccessibilityDefaults result = mapper.read();
    if (result == null)
      throw new ServiceException(503, "Platform reading defaults are unavailable.");
    return result;
  }

  @Transactional
  public AccessibilityDefaults update(AccessibilityDefaults body) {
    if (!"platform_admin".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Only administrators can change platform reading defaults.");
    if (mapper.update(body, SecurityUtils.getLoginUser().getUserId()) != 1)
      throw new ServiceException(409, "These settings changed. Reload them before saving again.");
    audit.insert(
        SecurityUtils.getLoginUser().getUserId(), "ACCESSIBILITY_DEFAULTS_UPDATE", "1", "SUCCESS");
    return read();
  }
}
