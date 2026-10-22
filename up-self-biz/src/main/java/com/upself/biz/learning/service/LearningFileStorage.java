package com.upself.biz.learning.service;

import com.upself.common.exception.ServiceException;
import com.upself.common.utils.Messages;

import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class LearningFileStorage {
    @Value("${learning.storage-path:./.local/uploads}")
    private String directory;

    @Autowired private Messages messages;

    public static final class Stored {
        public final String key, mime, name;
        public final long size;

        Stored(String key, String mime, String name, long size) {
            this.key = key;
            this.mime = mime;
            this.name = name;
            this.size = size;
        }
    }

    public Stored save(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) throw error("file.empty");
        long limit =
                "VIDEO".equals(kind)
                        ? 100L * 1024 * 1024
                        : "IMAGE".equals(kind) ? 10L * 1024 * 1024 : 1024L * 1024;
        if (file.getSize() > limit) throw error("file.large");
        String raw =
                Optional.ofNullable(file.getOriginalFilename()).orElse("file").replace('\\', '/');
        String name = raw.substring(raw.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\p{Cntrl}]", "_");
        if (name.length() > 150) name = name.substring(name.length() - 150);
        String lower = name.toLowerCase(Locale.ROOT);
        byte[] head = new byte[16];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.read(head);
        } catch (IOException e) {
            throw error("file.failed");
        }
        String ext, mime;
        if ("VIDEO".equals(kind)
                && lower.endsWith(".mp4")
                && n >= 12
                && head[4] == 'f'
                && head[5] == 't'
                && head[6] == 'y'
                && head[7] == 'p') {
            ext = "mp4";
            mime = "video/mp4";
        } else if ("IMAGE".equals(kind)
                && lower.endsWith(".png")
                && n >= 8
                && head[0] == (byte) 137
                && head[1] == 80
                && head[2] == 78
                && head[3] == 71
                && head[4] == 13
                && head[5] == 10
                && head[6] == 26
                && head[7] == 10) {
            ext = "png";
            mime = "image/png";
        } else if ("IMAGE".equals(kind)
                && (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
                && n >= 3
                && head[0] == (byte) 255
                && head[1] == (byte) 216
                && head[2] == (byte) 255) {
            ext = "jpg";
            mime = "image/jpeg";
        } else if ("IMAGE".equals(kind)
                && lower.endsWith(".webp")
                && n >= 12
                && new String(head, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(head, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            ext = "webp";
            mime = "image/webp";
        } else if ("CAPTION".equals(kind)
                && lower.endsWith(".vtt")
                && n >= 6
                && new String(head, 0, 6, StandardCharsets.UTF_8).equals("WEBVTT")) {
            ext = "vtt";
            mime = "text/vtt;charset=UTF-8";
        } else throw error("file.type");
        String key = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path base = Paths.get(directory).toAbsolutePath().normalize();
        Path target = base.resolve(key);
        try {
            Files.createDirectories(base);
            try (InputStream in = file.getInputStream();
                    OutputStream out =
                            Files.newOutputStream(
                                    target,
                                    StandardOpenOption.CREATE_NEW,
                                    StandardOpenOption.WRITE)) {
                byte[] b = new byte[8192];
                long count = 0;
                int read;
                while ((read = in.read(b)) != -1) {
                    count += read;
                    if (count > limit) throw new IOException("Upload limit exceeded");
                    out.write(b, 0, read);
                }
            }
            return new Stored(key, mime, name, Files.size(target));
        } catch (IOException e) {
            discard(key);
            throw error("file.failed");
        }
    }

    public FileSystemResource open(String key) {
        Path p = resolve(key);
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
            throw new ServiceException(404, messages.get("resource.unavailable"));
        return new FileSystemResource(p);
    }

    public void discard(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("Could not remove uncommitted upload", e);
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9]{32}\\.(mp4|png|jpg|webp|vtt)"))
            throw new ServiceException(404, messages.get("resource.unavailable"));
        Path base = Paths.get(directory).toAbsolutePath().normalize();
        Path p = base.resolve(key).normalize();
        if (!p.startsWith(base)) throw error("file.failed");
        return p;
    }

    private ServiceException error(String key) {
        return new ServiceException(400, messages.get(key));
    }
}
