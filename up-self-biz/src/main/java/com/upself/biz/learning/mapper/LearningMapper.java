package com.upself.biz.learning.mapper;

import com.upself.biz.learning.domain.*;

import org.apache.ibatis.annotations.Param;

import java.util.*;

public interface LearningMapper {
    List<Course> courses(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("keyword") String keyword);

    Course course(@Param("id") Long id);

    Course lockCourse(@Param("id") Long id);

    int insertCourse(Course c);

    int updateCourse(Course c);

    int assignMentor(@Param("id") Long id, @Param("mentorId") Long mentorId);

    int courseStatus(@Param("id") Long id, @Param("status") String status);

    int enrolled(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    int enroll(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    int unenroll(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    List<Map<String, Object>> members(@Param("courseId") Long courseId);

    List<Chapter> chapters(@Param("courseId") Long courseId, @Param("student") boolean student);

    Chapter chapter(@Param("id") Long id);

    int insertChapter(Chapter c);

    int updateChapter(Chapter c);

    int deleteChapter(@Param("id") Long id);

    List<LearningResource> resources(
            @Param("chapterId") Long chapterId, @Param("student") boolean student);

    LearningResource resource(@Param("id") Long id);

    int insertResource(LearningResource r);

    int updateResource(LearningResource r);

    int deleteResource(@Param("id") Long id);

    int captionReferences(@Param("id") Long id);

    List<CoursePost> posts(@Param("courseId") Long courseId, @Param("student") boolean student);

    CoursePost post(@Param("id") Long id);

    int insertPost(CoursePost p);

    int updatePost(CoursePost p);

    int offlinePost(@Param("id") Long id);

    int deletePost(@Param("id") Long id);
}
