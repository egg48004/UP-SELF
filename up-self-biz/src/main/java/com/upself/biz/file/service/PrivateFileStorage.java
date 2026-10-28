package com.upself.biz.file.service;

import com.upself.common.exception.ServiceException;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

public abstract class PrivateFileStorage {
  @org.springframework.beans.factory.annotation.Autowired private com.upself.biz.recording.service.RecordingStorage recordings;
  private String directory;
  protected void configureDirectory(String path) { this.directory = path; }

  private static final Logger LOG = LoggerFactory.getLogger(PrivateFileStorage.class);

  public static class Stored {
    public final String key, name, mime;
    public final long size;

    Stored(String key, String name, String mime, long size) {
      this.key = key;
      this.name = name;
      this.mime = mime;
      this.size = size;
    }
  }

  private ServiceException invalid() {
    return new ServiceException(
        400, "Choose a valid PDF, DOCX, PNG, JPG or UTF-8 TXT file, up to 20 MB.");
  }

  public Stored save(MultipartFile file) {
    if (file != null && com.upself.biz.recording.service.RecordingStorage.audioName(file.getOriginalFilename())) {
      com.upself.biz.recording.service.RecordingStorage.Stored audio = recordings.save(file);
      return new Stored(audio.key, audio.name, audio.mime, audio.size);
    }
    if (file == null || file.isEmpty() || file.getSize() > 20L * 1024 * 1024) throw invalid();
    String raw = Optional.ofNullable(file.getOriginalFilename()).orElse("file").replace('\\', '/');
    String name = raw.substring(raw.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
    if (name.length() > 150)
      throw new ServiceException(400, "Use a file name of at most 150 characters.");
    String lower = name.toLowerCase(Locale.ROOT);
    String ext;
    String mime;
    try {
      byte[] head = new byte[8];
      int count;
      try (InputStream input = file.getInputStream()) {
        count = input.read(head);
      }
      if (lower.endsWith(".pdf")
          && count >= 5
          && new String(head, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
        ext = "pdf";
        mime = "application/pdf";
      } else if (lower.endsWith(".png")
          && count == 8
          && Arrays.equals(head, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})) {
        ext = "png";
        mime = "image/png";
      } else if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
          && count >= 3
          && head[0] == (byte) 255
          && head[1] == (byte) 216
          && head[2] == (byte) 255) {
        ext = "jpg";
        mime = "image/jpeg";
      } else if (lower.endsWith(".txt")) {
        byte[] bytes = file.getBytes();
        String text =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        if (text.indexOf('\0') >= 0) throw invalid();
        ext = "txt";
        mime = "text/plain;charset=UTF-8";
      } else if (lower.endsWith(".docx")) {
        boolean document = false, types = false;
        int entries = 0;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
          ZipEntry entry;
          byte[] buffer = new byte[8192];
          while ((entry = zip.getNextEntry()) != null) {
            if (++entries > 2000
                || entry.getName().contains("..")
                || entry.getName().toLowerCase(Locale.ROOT).endsWith("vbaproject.bin"))
              throw invalid();
            document |= "word/document.xml".equals(entry.getName());
            types |= "[Content_Types].xml".equals(entry.getName());
            int n;
            while ((n = zip.read(buffer)) != -1) {
              total += n;
              if (total > 100L * 1024 * 1024) throw invalid();
            }
          }
        }
        if (!document || !types) throw invalid();
        ext = "docx";
        mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      } else throw invalid();
      String key = UUID.randomUUID().toString().replace("-", "") + "." + ext;
      Path base = Paths.get(directory).toAbsolutePath().normalize();
      Files.createDirectories(base);
      Path path = base.resolve(key);
      try (InputStream in = file.getInputStream();
          OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
        byte[] buffer = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(buffer)) != -1) {
          total += n;
          if (total > 20L * 1024 * 1024) throw new IOException("Upload limit");
          out.write(buffer, 0, n);
        }
      } catch (IOException failure) {
        discard(key);
        throw failure;
      }
      return new Stored(key, name, mime, Files.size(path));
    } catch (IOException failure) {
      throw invalid();
    }
  }

  private Path resolve(String key) {
    if (key == null || !key.matches("[a-f0-9]{32}\\.(pdf|docx|png|jpg|txt)"))
      throw new ServiceException(404, "This file is unavailable.");
    Path base = Paths.get(directory).toAbsolutePath().normalize();
    return base.resolve(key);
  }

  public FileSystemResource open(String key) {
    if (com.upself.biz.recording.service.RecordingStorage.audioKey(key)) return recordings.open(key);
    Path p = resolve(key);
    if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
      throw new ServiceException(404, "This file is unavailable.");
    return new FileSystemResource(p);
  }

  public void discard(String key) {
    if (com.upself.biz.recording.service.RecordingStorage.audioKey(key)) { recordings.discard(key); return; }
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException failure) {
      LOG.warn("Could not remove an unused private upload", failure);
    }
  }
}
