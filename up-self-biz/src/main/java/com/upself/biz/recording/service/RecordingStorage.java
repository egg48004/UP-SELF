package com.upself.biz.recording.service;

import com.upself.common.exception.ServiceException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.AudioSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Private recording files; business services authorize every read. No transcription dependency. */
@Service
public class RecordingStorage {
    public static final long MAX_BYTES = 20L * 1024 * 1024;
    @Value("${recording.storage-path:${learning.storage-path:./.local/uploads}/recordings}")
    private String directory;

    public static class Stored {
        public final String key, name, mime;
        public final long size;
        Stored(String key, String name, String mime, long size) { this.key=key; this.name=name; this.mime=mime; this.size=size; }
    }
    public static boolean audioName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).matches(".*\\.(webm|mp4|m4a|ogg|wav)");
    }
    public static boolean audioKey(String key) { return key != null && key.matches("[a-f0-9]{32}\\.(webm|m4a|ogg|wav)"); }
    private ServiceException invalid() { return new ServiceException(400, "Choose a valid audio recording up to 20 MB (WebM, M4A, MP4, Ogg or WAV)."); }
    private static String ascii(byte[] bytes, int start, int length) { return new String(bytes,start,length,StandardCharsets.ISO_8859_1); }
    private static long uint(byte[] b, int p) { return ((b[p]&255L)<<24)|((b[p+1]&255L)<<16)|((b[p+2]&255L)<<8)|(b[p+3]&255L); }
    private int mp4Tracks(byte[] bytes, int start, int end, int depth) {
        if (depth > 5) throw invalid();
        int result=0;
        for (int p=start; p+8<=end;) {
            long size=uint(bytes,p); int header=8;
            if (size==1) { if (p+16>end || uint(bytes,p+8)!=0) throw invalid(); size=uint(bytes,p+12); header=16; }
            if (size==0) size=end-p;
            if (size<header || size>end-p) throw invalid();
            String type=ascii(bytes,p+4,4); int next=p+(int)size;
            if ("moov".equals(type)||"trak".equals(type)||"mdia".equals(type)) result |= mp4Tracks(bytes,p+header,next,depth+1);
            if ("hdlr".equals(type) && p+header+12<=next) {
                String handler=ascii(bytes,p+header+8,4);
                if ("soun".equals(handler)) result|=1;
                if ("vide".equals(handler)) result|=2;
            }
            p=next;
        }
        return result;
    }
    public Stored save(MultipartFile file) {
        if (file==null || file.isEmpty() || file.getSize()>MAX_BYTES) throw invalid();
        String raw=Optional.ofNullable(file.getOriginalFilename()).orElse("recording").replace('\\','/');
        String name=raw.substring(raw.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","_");
        if (!audioName(name) || name.length()>150) throw invalid();
        try {
            byte[] bytes=file.getBytes(); if (bytes.length<12 || bytes.length>MAX_BYTES) throw invalid();
            String ext, mime;
            String head=ascii(bytes,0,Math.min(bytes.length,65536));
            if (name.toLowerCase(Locale.ROOT).endsWith(".webm") && (bytes[0]&255)==0x1a && (bytes[1]&255)==0x45 && (bytes[2]&255)==0xdf && (bytes[3]&255)==0xa3
                    && head.contains("webm") && (head.contains("A_OPUS")||head.contains("A_VORBIS")) && !head.contains("V_VP")) { ext="webm"; mime="audio/webm"; }
            else if (name.toLowerCase(Locale.ROOT).endsWith(".ogg") && head.startsWith("OggS") && (head.contains("OpusHead")||head.contains("vorbis")) && !head.contains("theora")) { ext="ogg"; mime="audio/ogg"; }
            else if (name.toLowerCase(Locale.ROOT).endsWith(".wav") && head.startsWith("RIFF") && "WAVE".equals(ascii(bytes,8,4))) {
                try (javax.sound.sampled.AudioInputStream audio=AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) { if (audio.getFormat().getChannels()<1 || audio.getFrameLength()<1) throw invalid(); }
                ext="wav"; mime="audio/wav";
            } else if (name.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|m4a)") && "ftyp".equals(ascii(bytes,4,4)) && mp4Tracks(bytes,0,bytes.length,0)==1) { ext="m4a"; mime="audio/mp4"; }
            else throw invalid();
            String key=UUID.randomUUID().toString().replace("-","")+"."+ext;
            Path base=Paths.get(directory).toAbsolutePath().normalize(); Files.createDirectories(base);
            Path target=base.resolve(key);
            try { Files.write(target,bytes,StandardOpenOption.CREATE_NEW); }
            catch (IOException failure) { Files.deleteIfExists(target); throw failure; }
            return new Stored(key,name,mime,bytes.length);
        } catch (IOException | javax.sound.sampled.UnsupportedAudioFileException e) { throw invalid(); }
    }
    private Path resolve(String key) {
        if (!audioKey(key)) throw new ServiceException(404,"This recording is unavailable.");
        return Paths.get(directory).toAbsolutePath().normalize().resolve(key);
    }
    public FileSystemResource open(String key) {
        Path file=resolve(key);
        if (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) throw new ServiceException(404,"This recording is unavailable.");
        return new FileSystemResource(file);
    }
    public void discard(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(RecordingStorage.class).warn("Could not remove an unused recording",e); }
    }
}
