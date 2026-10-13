package com.fyp2.up_self_demo01.service;

import java.util.List;

import com.fyp2.up_self_demo01.domain.vo.CourseListVO;

public interface ICourseService {

    /**
     * 查询已发布的课程列表
     *
     * @return 课程列表
     */
    List<CourseListVO> listPublishedCourses();
}