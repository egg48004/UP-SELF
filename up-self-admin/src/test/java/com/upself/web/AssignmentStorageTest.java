package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;

import com.upself.biz.assignment.service.AssignmentFileStorage;
import com.upself.common.exception.ServiceException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class AssignmentStorageTest {
  @TempDir Path directory;
  AssignmentFileStorage storage;

  @BeforeEach
  void setup() {
    storage = new AssignmentFileStorage();
    ReflectionTestUtils.setField(storage, "directory", directory.toString());
  }

  MockMultipartFile file(String name, byte[] bytes) {
    return new MockMultipartFile("file", name, "application/octet-stream", bytes);
  }

  @Test
  void unicodeTextUsesPrivateRandomPath() throws Exception {
    byte[] b = "My reflection 🌱".getBytes(StandardCharsets.UTF_8);
    AssignmentFileStorage.Stored s = storage.save(file("../../answer.txt", b));
    assertEquals("answer.txt", s.name);
    assertArrayEquals(b, Files.readAllBytes(storage.open(s.key).getFile().toPath()));
    storage.discard(s.key);
    assertFalse(directory.resolve(s.key).toFile().exists());
  }

  @Test
  void disguisedHtmlAndInvalidUtf8AreRejected() {
    assertThrows(
        ServiceException.class, () -> storage.save(file("fake.pdf", "<html/>".getBytes())));
    assertThrows(
        ServiceException.class, () -> storage.save(file("bad.txt", new byte[] {(byte) 255})));
  }

  @Test
  void emptyAndOversizeRejected() {
    assertThrows(ServiceException.class, () -> storage.save(file("a.txt", new byte[0])));
    assertThrows(
        ServiceException.class, () -> storage.save(file("a.txt", new byte[20 * 1024 * 1024 + 1])));
  }

  @Test
  void traversalCannotReadSecrets() {
    assertThrows(ServiceException.class, () -> storage.open("../config/application-local.yml"));
  }

  @Test
  void docxRequiresDocumentAndRejectsMacro() throws Exception {
    assertNotNull(storage.save(file("a.docx", zip(false))));
    assertThrows(ServiceException.class, () -> storage.save(file("macro.docx", zip(true))));
  }

  byte[] zip(boolean macro) throws Exception {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(b)) {
      for (String n :
          new String[] {
            "[Content_Types].xml",
            "word/document.xml",
            macro ? "word/vbaProject.bin" : "word/styles.xml"
          }) {
        z.putNextEntry(new ZipEntry(n));
        z.write("<xml/>".getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
      }
    }
    return b.toByteArray();
  }
}
