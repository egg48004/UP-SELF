package com.upself.biz.learning.service;

import com.upself.biz.learning.dto.SavedCourse;
import java.util.List;

public interface IBookmarkService {
    void verifyStudent();
    List<SavedCourse> list();
    void save(Long courseId);
    void remove(Long courseId);
}
