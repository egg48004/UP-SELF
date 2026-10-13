package com.fyp2.up_self_demo01.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fyp2.up_self_demo01.domain.vo.CourseListVO;
import com.fyp2.up_self_demo01.service.ICourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final ICourseService courseService;

    public CourseController(ICourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * FR-01：浏览课程
     */
    @GetMapping("/list")
    public List<CourseListVO> listCourses() {
        return courseService.listPublishedCourses();
    }
}