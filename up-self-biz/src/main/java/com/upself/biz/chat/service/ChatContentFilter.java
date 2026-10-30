package com.upself.biz.chat.service;

import com.upself.common.exception.ServiceException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Deliberately small literal phrase filter. It does not inspect file contents or recordings. */
@Component
public class ChatContentFilter {
  private static final Pattern FORMAT = Pattern.compile("\\p{Cf}");
  private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
  private final List<String> phrases = new ArrayList<>();

  public ChatContentFilter() {
    InputStream input = getClass().getResourceAsStream("/chat/content-filter.txt");
    if (input == null) throw new IllegalStateException("Missing discussion filter rules");
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.trim().isEmpty() && !line.trim().startsWith("#"))
          phrases.add(" " + normalize(line) + " ");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read discussion filter rules", e);
    }
  }

  private static String normalize(String text) {
    String normalized =
        Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    return SEPARATORS.matcher(FORMAT.matcher(normalized).replaceAll("")).replaceAll(" ").trim();
  }

  public void check(String text, boolean attachment) {
    String normalized = " " + normalize(text) + " ";
    for (String phrase : phrases)
      if (normalized.contains(phrase))
        throw new ServiceException(
            422,
            attachment
                ? "File not shared. Change hurtful or threatening words in the file name or"
                    + " description."
                : "Message not sent. Change hurtful or threatening words and try again.");
  }
}
