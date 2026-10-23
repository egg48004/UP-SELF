package com.upself.biz.learning.service.impl;

import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.dto.SavedCourse;
import com.upself.biz.learning.mapper.BookmarkMapper;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.learning.service.IBookmarkService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkServiceImpl implements IBookmarkService {
    @Autowired private BookmarkMapper mapper;
    @Autowired private LearningMapper learning;

    public void verifyStudent() {
        if (!"student".equals(SecurityUtils.getLoginUser().getRoleKey())) {
            throw new ServiceException(403, "Saved courses are available to students only.");
        }
    }

    private Long studentId() {
        verifyStudent();
        return SecurityUtils.getLoginUser().getUserId();
    }

    public List<SavedCourse> list() { return mapper.list(studentId()); }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(Long courseId) {
        Long owner = studentId();
        // Publishing and enrollment changes use this same course lock.
        Course course = learning.lockCourse(courseId);
        if (course == null) throw new ServiceException(404, "This course is unavailable.");
        if (!"PUBLISHED".equals(course.getStatus()) || learning.enrolled(courseId, owner) == 0) {
            throw new ServiceException(403, "You do not have access to this course.");
        }
        mapper.save(owner, courseId);
    }

    @Transactional
    public void remove(Long courseId) {
        // Removing one's bookmark is allowed even after course access is revoked.
        mapper.remove(studentId(), courseId);
    }
}
