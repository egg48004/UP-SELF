package com.upself.biz.reporting.service;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CsvWriter {
  private CsvWriter() {}

  public static String cell(Object value) {
    String s = value == null ? "" : value.toString();
    String trimmed = s.replaceFirst("^[\\s\\uFEFF]+", "");
    if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0)
        || s.startsWith("\t")
        || s.startsWith("\r")
        || s.startsWith("\n")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  public static byte[] write(List<String> headers, List<List<Object>> rows) {
    StringBuilder b = new StringBuilder("\uFEFF");
    line(b, new ArrayList<Object>(headers));
    for (List<Object> row : rows) line(b, row);
    return b.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void line(StringBuilder b, List<Object> row) {
    for (int i = 0; i < row.size(); i++) {
      if (i > 0) b.append(',');
      b.append(cell(row.get(i)));
    }
    b.append("\r\n");
  }
}
