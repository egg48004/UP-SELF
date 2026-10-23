package com.upself.biz.learning.service.impl;

import com.upself.biz.learning.domain.Chapter;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.dto.ProgressViews.*;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.learning.mapper.ProgressMapper;
import com.upself.biz.learning.service.IProgressService;
import com.upself.biz.mood.service.IMoodService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressServiceImpl implements IProgressService {
    @Autowired private LearningMapper learning;
    @Autowired private ProgressMapper mapper;

    private Long userId() {
        return SecurityUtils.getLoginUser().getUserId();
    }

    private void role(String expected) {
        if (!expected.equals(SecurityUtils.getLoginUser().getRoleKey())) {
            throw new ServiceException(403, "This action is not available for your role.");
        }
    }

    public void verifyStudent() {
        role("student");
    }

    public void verifyMentor() {
        role("mentor");
    }

    private void allowed(Course course) {
        verifyStudent();
        if (course == null) throw new ServiceException(404, "This course is unavailable.");
        if (!"PUBLISHED".equals(course.getStatus())
                || learning.enrolled(course.getId(), userId()) == 0) {
            throw new ServiceException(403, "You do not have access to this course.");
        }
    }

    private void writable(Long chapterId) {
        verifyStudent();
        Chapter chapter = learning.chapter(chapterId);
        if (chapter == null) throw new ServiceException(404, "This chapter is unavailable.");
        // Same course lock used by publishing, deleting chapters and changing enrollment.
        allowed(learning.lockCourse(chapter.getCourseId()));
        chapter = learning.chapter(chapterId);
        if (chapter == null || !"PUBLISHED".equals(chapter.getStatus())) {
            throw new ServiceException(403, "This chapter is not published.");
        }
    }

    public Summary summary(Long courseId) {
        verifyStudent();
        allowed(learning.course(courseId));
        List<Item> items = mapper.chapters(userId(), courseId);
        Summary summary = new Summary();
        summary.setChapters(items);
        summary.setTotal(items.size());
        Date latest = null;
        for (Item item : items) {
            if (item.getCompletedAt() != null) summary.setCompleted(summary.getCompleted() + 1);
            if (item.getLastVisitedAt() != null
                    && (latest == null || !item.getLastVisitedAt().before(latest))) {
                latest = item.getLastVisitedAt();
                summary.setLastChapterId(item.getChapterId());
            }
        }
        summary.setPercent(summary.getTotal() == 0 ? 0
                : (int) Math.round(100.0 * summary.getCompleted() / summary.getTotal()));
        return summary;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void visit(Long chapterId) {
        writable(chapterId);
        mapper.visit(userId(), chapterId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void complete(Long chapterId, boolean completed) {
        writable(chapterId);
        mapper.complete(userId(), chapterId, completed);
    }

    public Item resume() {
        verifyStudent();
        return mapper.resume(userId());
    }

    public List<Item> history() {
        verifyStudent();
        return mapper.history(userId());
    }

    public List<Overview> overview(String keyword, Long courseId) {
        verifyMentor();
        if (keyword != null && keyword.length() > 100) {
            throw new ServiceException(400, "Use at most 100 characters for your search.");
        }
        return mapper.overview(userId(), keyword, courseId, IMoodService.NOTICE_VERSION);
    }
}
