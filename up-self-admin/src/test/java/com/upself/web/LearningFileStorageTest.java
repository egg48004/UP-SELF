package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.biz.learning.service.LearningFileStorage;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.Messages;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

class LearningFileStorageTest {
    @TempDir Path directory;
    private LearningFileStorage storage;

    @BeforeEach
    void setup() {
        storage = new LearningFileStorage();
        Messages messages = mock(Messages.class);
        when(messages.get(anyString())).thenAnswer(call -> call.getArgument(0));
        ReflectionTestUtils.setField(storage, "directory", directory.toString());
        ReflectionTestUtils.setField(storage, "messages", messages);
    }

    @Test
    void validJpegUsesPrivateRandomNameAndCanBeRead() throws Exception {
        byte[] content = {(byte) 255, (byte) 216, (byte) 255, 1};
        LearningFileStorage.Stored stored =
                storage.save(
                        new MockMultipartFile("file", "photo.jpg", "image/jpeg", content), "IMAGE");
        assertEquals(content.length, storage.open(stored.key).contentLength());
        assertEquals("image/jpeg", stored.mime);
        assertNotEquals("photo.jpg", stored.key);
    }

    @Test
    void extensionAloneDoesNotPermitSvgContent() {
        assertThrows(
                ServiceException.class,
                () ->
                        storage.save(
                                new MockMultipartFile(
                                        "file",
                                        "fake.png",
                                        "image/png",
                                        "<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                "IMAGE"));
    }

    @Test
    void traversalCannotOpenFilesOutsideStorage() {
        assertThrows(ServiceException.class, () -> storage.open("../application-local.yml"));
    }

    @Test
    void webVttCaptionsAreAccepted() throws Exception {
        LearningFileStorage.Stored stored =
                storage.save(
                        new MockMultipartFile(
                                "file",
                                "english.vtt",
                                "text/vtt",
                                "WEBVTT\n\n00:00.000 --> 00:01.000\nHello\n"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        "CAPTION");
        assertTrue(storage.open(stored.key).exists());
    }
}
