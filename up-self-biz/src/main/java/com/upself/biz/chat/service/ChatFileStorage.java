package com.upself.biz.chat.service;
import com.upself.biz.file.service.PrivateFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class ChatFileStorage extends PrivateFileStorage {
    @Value("${chat.storage-path:${learning.storage-path:./.local/uploads}/chat}")
    public void setDirectory(String path) { configureDirectory(path); }
}
