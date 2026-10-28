package com.upself.biz.assignment.service;
import com.upself.biz.file.service.PrivateFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class AssignmentFileStorage extends PrivateFileStorage {
    @Value("${assignment.storage-path:${learning.storage-path:./.local/uploads}/assignments}")
    public void setDirectory(String path) { configureDirectory(path); }
}
