package com.upself.biz.learning.dto;

import com.upself.biz.learning.domain.Course;
import java.util.Date;
import lombok.Data;

/** An owned bookmark may remain after course access is revoked. */
@Data
public class SavedCourse {
    private Long courseId;
    private Date savedAt;
    private Course course;

    public boolean isAvailable() { return course != null; }
}
