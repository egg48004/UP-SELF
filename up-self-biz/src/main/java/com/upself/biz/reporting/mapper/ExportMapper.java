package com.upself.biz.reporting.mapper;

import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface ExportMapper {
  List<Map<String, Object>> users(
      @Param("keyword") String keyword, @Param("role") String role, @Param("status") String status);

  List<Map<String, Object>> courses(
      @Param("keyword") String keyword, @Param("status") String status);

  List<Map<String, Object>> learning(
      @Param("keyword") String keyword, @Param("courseId") Long courseId);
}
