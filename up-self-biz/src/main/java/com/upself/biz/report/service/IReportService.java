package com.upself.biz.report.service;

import com.upself.biz.report.domain.UserReport;
import com.upself.biz.report.dto.ReportBodies.*;
import java.util.List;

public interface IReportService {
  void participant();

  void administrator();

  Long create(Create b);

  List<Own> mine();

  List<UserReport> list(String status);

  UserReport detail(Long id);

  void review(Long id, Review b);
}
