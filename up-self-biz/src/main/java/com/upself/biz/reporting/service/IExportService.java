package com.upself.biz.reporting.service;

public interface IExportService {
  byte[] export(String type, String keyword, String role, String status, Long courseId);
}
