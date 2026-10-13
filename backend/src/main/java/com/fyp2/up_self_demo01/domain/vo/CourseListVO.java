package com.fyp2.up_self_demo01.domain.vo;

import lombok.Data;

@Data
public class CourseListVO {

    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private Integer estimatedMinutes;
}