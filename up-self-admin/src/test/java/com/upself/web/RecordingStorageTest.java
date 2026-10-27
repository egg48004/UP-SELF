package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import com.upself.biz.recording.service.RecordingStorage;
import com.upself.biz.assignment.service.AssignmentFileStorage;
import com.upself.common.exception.ServiceException;
import java.io.*;
import java.nio.file.*;
import javax.sound.sampled.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class RecordingStorageTest {
    @TempDir Path directory;
    RecordingStorage storage;
    @BeforeEach void setup() { storage=new RecordingStorage(); ReflectionTestUtils.setField(storage,"directory",directory.toString()); }
    byte[] wav() throws Exception {
        ByteArrayOutputStream result=new ByteArrayOutputStream();
        AudioFormat format=new AudioFormat(8000,16,1,true,false);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(new byte[16000]),format,8000),AudioFileFormat.Type.WAVE,result);
        return result.toByteArray();
    }
    MockMultipartFile file(String name, byte[] bytes) { return new MockMultipartFile("file",name,"text/html",bytes); }
    @Test void detectsActualAudioAndUsesPrivateRandomPaths() throws Exception {
        byte[] bytes=wav(); RecordingStorage.Stored result=storage.save(file("../../reflection.wav",bytes));
        assertEquals("reflection.wav",result.name); assertEquals("audio/wav",result.mime);
        assertArrayEquals(bytes,Files.readAllBytes(storage.open(result.key).getFile().toPath()));
        storage.discard(result.key); assertThrows(ServiceException.class,()->storage.open(result.key));
    }
    @Test void rejectsDisguisedFilesEmptyAndOversize() {
        assertThrows(ServiceException.class,()->storage.save(file("fake.webm","<html>not audio</html>".getBytes())));
        assertThrows(ServiceException.class,()->storage.save(file("recording.wav",new byte[0])));
        assertThrows(ServiceException.class,()->storage.save(file("recording.wav",new byte[20*1024*1024+1])));
        assertThrows(ServiceException.class,()->storage.open("../config/application-local.yml"));
    }
    @Test void rejectsVideoAndBrokenMp4Containers() {
        byte[] video=new byte[]{0,0,0,12,'f','t','y','p','i','s','o','m',0,0,0,20,'h','d','l','r',0,0,0,0,0,0,0,0,'v','i','d','e'};
        assertThrows(ServiceException.class,()->storage.save(file("video.mp4",video)));
        video[12]=127;
        assertThrows(ServiceException.class,()->storage.save(file("broken.m4a",video)));
    }
    @Test void assignmentFilesReuseAudioStorageWithoutChangingDocumentStorage() throws Exception {
        AssignmentFileStorage assignments=new AssignmentFileStorage();
        ReflectionTestUtils.setField(assignments,"recordings",storage);
        ReflectionTestUtils.setField(assignments,"directory",directory.resolve("documents").toString());
        AssignmentFileStorage.Stored result=assignments.save(file("answer.wav",wav()));
        assertEquals("audio/wav",result.mime); assertTrue(assignments.open(result.key).exists());
        assignments.discard(result.key); assertFalse(directory.resolve(result.key).toFile().exists());
    }
}
