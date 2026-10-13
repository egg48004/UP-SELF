package com.fyp2.up_self_demo01.service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fyp2.up_self_demo01.domain.Course;
import com.fyp2.up_self_demo01.domain.vo.CourseListVO;
import com.fyp2.up_self_demo01.mapper.CourseMapper;
import com.fyp2.up_self_demo01.service.ICourseService;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements ICourseService {

    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public List<CourseListVO> listPublishedCourses() {

        // 只查询已发布且未删除的课程
        LambdaQueryWrapper<Course> wrapper =
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "published")
                        .eq(Course::getDeleted, 0)
                        .orderByAsc(Course::getId);

        List<Course> courses = courseMapper.selectList(wrapper);

        // 将数据库实体转换为接口返回对象
        return courses.stream()
                .map(course -> {
                    CourseListVO vo = new CourseListVO();
                    BeanUtils.copyProperties(course, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}