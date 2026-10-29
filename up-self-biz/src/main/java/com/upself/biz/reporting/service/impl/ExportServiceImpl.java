package com.upself.biz.reporting.service.impl;

import com.upself.biz.reporting.mapper.ExportMapper;
import com.upself.biz.reporting.service.*;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExportServiceImpl implements IExportService {
  @Autowired private ExportMapper mapper;
  @Autowired private SysAuditMapper audit;

  private long n(Map<String, Object> row, String key) {
    Object v = row.get(key);
    return v == null ? 0 : ((Number) v).longValue();
  }

  private void choice(String value, String... allowed) {
    if (value != null && !value.isEmpty() && !Arrays.asList(allowed).contains(value))
      throw new ServiceException(400, "Choose a valid export filter.");
  }

  public byte[] export(String type, String keyword, String role, String status, Long courseId) {
    if (!"platform_admin".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Only administrators can export system reports.");
    if (keyword != null && keyword.length() > 100)
      throw new ServiceException(400, "Use at most 100 characters for the search.");
    List<Map<String, Object>> records;
    List<String> headers;
    String[] keys;
    switch (type) {
      case "users":
        choice(role, "student", "mentor", "platform_admin");
        choice(status, "0", "1");
        records = mapper.users(keyword, role, status);
        headers =
            Arrays.asList("Username", "Display name", "Role", "Status", "Created (Malaysia time)");
        keys = new String[] {"username", "name", "role", "status", "created"};
        break;
      case "courses":
        choice(status, "DRAFT", "PUBLISHED", "ARCHIVED");
        records = mapper.courses(keyword, status);
        headers =
            Arrays.asList(
                "Course",
                "Status",
                "Mentor username",
                "Mentor name",
                "Active students",
                "Difficulty",
                "Estimated minutes");
        keys =
            new String[] {
              "course", "status", "mentor", "mentorName", "students", "difficulty", "minutes"
            };
        break;
      case "learning":
        if (courseId != null && courseId < 1)
          throw new ServiceException(400, "Choose a valid course.");
        records = mapper.learning(keyword, courseId);
        headers =
            Arrays.asList(
                "Student username",
                "Student name",
                "Course",
                "Mentor username",
                "Completed chapters",
                "Total chapters",
                "Completion percent",
                "Not submitted",
                "Waiting for review",
                "Graded",
                "Returned");
        keys =
            new String[] {
              "username",
              "name",
              "course",
              "mentor",
              "completed",
              "total",
              "percent",
              "remaining",
              "waiting",
              "graded",
              "returned"
            };
        break;
      default:
        throw new ServiceException(400, "Choose Users, Courses or Learning summary.");
    }
    if (records.size() > 10000)
      throw new ServiceException(400, "Narrow the filters to export at most 10000 rows.");
    List<List<Object>> rows = new ArrayList<>();
    for (Map<String, Object> row : records) {
      if ("learning".equals(type)) {
        long total = n(row, "total");
        row.put(
            "percent",
            total == 0
                ? "0.00"
                : BigDecimal.valueOf(n(row, "completed") * 100)
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    .toPlainString());
        row.put(
            "remaining",
            Math.max(
                0,
                n(row, "assignments") - n(row, "waiting") - n(row, "graded") - n(row, "returned")));
      }
      List<Object> values = new ArrayList<>();
      for (String key : keys) values.add(row.get(key));
      rows.add(values);
    }
    byte[] csv = CsvWriter.write(headers, rows);
    audit.insert(
        SecurityUtils.getUserId(),
        "EXPORT_" + type.toUpperCase(Locale.ROOT),
        "rows=" + rows.size(),
        "SUCCESS");
    return csv;
  }
}
