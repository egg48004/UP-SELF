package com.upself.system.mapper;

import com.upself.system.domain.AccessibilityDefaults;
import org.apache.ibatis.annotations.Param;

public interface AccessibilityMapper {
  AccessibilityDefaults read();

  int update(@Param("body") AccessibilityDefaults body, @Param("actor") Long actor);
}
