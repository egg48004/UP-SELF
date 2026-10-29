package com.upself.system.service;

import com.upself.system.domain.AccessibilityDefaults;

public interface IAccessibilityService {
  AccessibilityDefaults read();

  AccessibilityDefaults update(AccessibilityDefaults body);
}
