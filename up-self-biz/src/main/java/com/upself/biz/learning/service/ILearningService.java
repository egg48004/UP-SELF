package com.upself.biz.learning.service;

import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.LearningBodies.*;
import com.upself.common.core.domain.model.LoginUser;

import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface ILearningService {
    List<Course> courses(String keyword);

    Course course(Long id);

    Long createCourse(CourseInput b);

    void updateCourse(Long id, CourseInput b);

    void assignMentor(Long id, Long mentorId);

    void courseState(Long id, String state);

    void enroll(Long id, Long studentId);

    void unenroll(Long id, Long studentId);

    List<Map<String, Object>> members(Long id);

    List<Chapter> chapters(Long courseId);

    Chapter chapter(Long id);

    Long createChapter(Long courseId, ChapterInput b);

    void updateChapter(Long id, ChapterInput b);

    void deleteChapter(Long id);

    List<LearningResource> resources(Long chapterId);

    LearningResource resource(Long id);

    LearningResource readMedia(Long id, LoginUser actor);

    Long upload(Long chapterId, MultipartFile file, String kind, ResourceInput b);

    void updateResource(Long id, ResourceInput b);

    void deleteResource(Long id);

    List<CoursePost> posts(Long courseId);

    CoursePost post(Long id);

    Long createPost(Long courseId, PostInput b);

    void updatePost(Long id, PostInput b);

    void offlinePost(Long id);

    void deletePost(Long id);
}
