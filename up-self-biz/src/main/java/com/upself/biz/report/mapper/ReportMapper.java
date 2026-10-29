package com.upself.biz.report.mapper;

import com.upself.biz.report.domain.UserReport;
import com.upself.biz.report.dto.ReportBodies.Own;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReportMapper {
  String name(Long id);

  int insert(UserReport r);

  List<Own> mine(@Param("owner") Long owner);

  List<UserReport> list(@Param("status") String status);

  UserReport detail(Long id);

  int review(
      @Param("id") Long id,
      @Param("actor") Long actor,
      @Param("status") String status,
      @Param("resolution") String resolution,
      @Param("version") Integer version);
}
