package com.upself.biz.learning.mapper;

import com.upself.biz.learning.dto.SavedCourse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BookmarkMapper {
    List<SavedCourse> list(@Param("studentId") Long studentId);
    boolean saved(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    int save(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    int remove(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
