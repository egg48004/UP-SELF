package com.upself.biz.learning.service.impl;

import com.upself.biz.learning.domain.*;
import com.upself.biz.learning.dto.LearningBodies.*;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.learning.service.*;
import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.*;
import com.upself.system.mapper.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class LearningServiceImpl implements ILearningService {
    @Autowired private LearningMapper mapper;
    @Autowired private SysUserMapper users;
    @Autowired private SysAuditMapper audit;
    @Autowired private LearningFileStorage files;
    @Autowired private Messages messages;

    private LoginUser actor() {
        return SecurityUtils.getLoginUser();
    }

    private boolean student(LoginUser u) {
        return "student".equals(u.getRoleKey());
    }

    private boolean admin(LoginUser u) {
        return "platform_admin".equals(u.getRoleKey()) || "admin".equals(u.getRoleKey());
    }

    private void fail(int code, String key) {
        throw new ServiceException(code, messages.get(key));
    }

    private Course get(Long id, boolean lock) {
        Course c = lock ? mapper.lockCourse(id) : mapper.course(id);
        if (c == null) fail(404, "course.unavailable");
        return c;
    }

    private void readable(Course c, LoginUser u) {
        if (student(u)) {
            if (!"PUBLISHED".equals(c.getStatus())
                    || mapper.enrolled(c.getId(), u.getUserId()) == 0) fail(403, "course.denied");
        } else if (!admin(u)
                && !("mentor".equals(u.getRoleKey()) && u.getUserId().equals(c.getMentorId())))
            fail(403, "course.denied");
    }

    private Course editable(Long id) {
        Course c = get(id, true);
        LoginUser u = actor();
        if (!admin(u)
                && !("mentor".equals(u.getRoleKey()) && u.getUserId().equals(c.getMentorId())))
            fail(403, "course.denied");
        if ("ARCHIVED".equals(c.getStatus())) fail(400, "course.archived");
        return c;
    }

    private void administrator() {
        if (!admin(actor())) fail(403, "permission.denied");
    }

    private void record(String action, Long id) {
        audit.insert(actor().getUserId(), action, id.toString(), "SUCCESS");
    }

    private void validUser(Long id, String role) {
        SysUser u = users.selectById(id);
        if (u == null || !"0".equals(u.getStatus()) || !role.equals(u.getRoleKey()))
            fail(400, "user.role.invalid");
    }

    public List<Course> courses(String keyword) {
        LoginUser u = actor();
        return mapper.courses(u.getUserId(), u.getRoleKey(), keyword);
    }

    public Course course(Long id) {
        Course c = get(id, false);
        readable(c, actor());
        return c;
    }

    @Transactional
    public Long createCourse(CourseInput b) {
        administrator();
        Course c = new Course();
        c.setTitle(b.getTitle());
        c.setDescription(b.getDescription());
        c.setLanguageCode(b.getLanguageCode());
        c.setCreatedBy(actor().getUserId());
        mapper.insertCourse(c);
        record("COURSE_CREATE", c.getId());
        return c.getId();
    }

    @Transactional
    public void updateCourse(Long id, CourseInput b) {
        administrator();
        Course c = get(id, true);
        c.setTitle(b.getTitle());
        c.setDescription(b.getDescription());
        c.setLanguageCode(b.getLanguageCode());
        mapper.updateCourse(c);
        record("COURSE_EDIT", id);
    }

    @Transactional
    public void assignMentor(Long id, Long mentorId) {
        administrator();
        get(id, true);
        validUser(mentorId, "mentor");
        mapper.assignMentor(id, mentorId);
        record("COURSE_ASSIGN_MENTOR", id);
    }

    @Transactional
    public void courseState(Long id, String state) {
        administrator();
        Course c = get(id, true);
        if ("PUBLISHED".equals(state)) {
            if (c.getMentorId() == null) fail(400, "course.mentor.required");
            validUser(c.getMentorId(), "mentor");
        }
        mapper.courseStatus(id, state);
        record("COURSE_STATE", id);
    }

    @Transactional
    public void enroll(Long id, Long studentId) {
        administrator();
        get(id, true);
        validUser(studentId, "student");
        mapper.enroll(id, studentId);
        record("COURSE_ENROLL", id);
    }

    @Transactional
    public void unenroll(Long id, Long studentId) {
        administrator();
        get(id, true);
        mapper.unenroll(id, studentId);
        record("COURSE_UNENROLL", id);
    }

    public List<Map<String, Object>> members(Long id) {
        Course c = get(id, false);
        readable(c, actor());
        if (student(actor())) fail(403, "permission.denied");
        return mapper.members(id);
    }

    public List<Chapter> chapters(Long courseId) {
        course(courseId);
        return mapper.chapters(courseId, student(actor()));
    }

    private Chapter rawChapter(Long id) {
        Chapter c = mapper.chapter(id);
        if (c == null) fail(404, "chapter.unavailable");
        return c;
    }

    public Chapter chapter(Long id) {
        Chapter c = rawChapter(id);
        readable(get(c.getCourseId(), false), actor());
        if (student(actor()) && !"PUBLISHED".equals(c.getStatus())) fail(403, "chapter.denied");
        return c;
    }

    @Transactional
    public Long createChapter(Long courseId, ChapterInput b) {
        editable(courseId);
        Chapter c = new Chapter();
        c.setCourseId(courseId);
        copy(c, b);
        mapper.insertChapter(c);
        record("CHAPTER_CREATE", c.getId());
        return c.getId();
    }

    private void copy(Chapter c, ChapterInput b) {
        c.setTitle(b.getTitle());
        c.setBody(b.getBody());
        c.setSortOrder(b.getSortOrder());
        c.setStatus(b.getStatus());
        c.setLanguageCode(b.getLanguageCode());
    }

    @Transactional
    public void updateChapter(Long id, ChapterInput b) {
        Chapter c = rawChapter(id);
        editable(c.getCourseId());
        c = rawChapter(id);
        copy(c, b);
        mapper.updateChapter(c);
        record("CHAPTER_EDIT", id);
    }

    @Transactional
    public void deleteChapter(Long id) {
        Chapter c = rawChapter(id);
        editable(c.getCourseId());
        rawChapter(id);
        mapper.deleteChapter(id);
        record("CHAPTER_DELETE", id);
    }

    public List<LearningResource> resources(Long chapterId) {
        chapter(chapterId);
        return mapper.resources(chapterId, student(actor()));
    }

    private LearningResource rawResource(Long id) {
        LearningResource r = mapper.resource(id);
        if (r == null) fail(404, "resource.unavailable");
        return r;
    }

    public LearningResource resource(Long id) {
        return readMedia(id, actor());
    }

    public LearningResource readMedia(Long id, LoginUser u) {
        if (u.getPermissions() == null
                || !(u.getPermissions().contains("learning:resource:query")
                        || u.getPermissions().contains("*:*:*"))) fail(403, "permission.denied");
        LearningResource r = rawResource(id);
        Chapter c = rawChapter(r.getChapterId());
        readable(get(c.getCourseId(), false), u);
        if (student(u)
                && (!"PUBLISHED".equals(c.getStatus()) || !"PUBLISHED".equals(r.getStatus())))
            fail(403, "resource.denied");
        return r;
    }

    private void resourceFields(LearningResource r, ResourceInput b) {
        if ("IMAGE".equals(r.getKind())
                && (b.getAltText() == null || b.getAltText().trim().isEmpty()))
            fail(400, "image.alt.required");
        if (!"VIDEO".equals(r.getKind())
                && (b.getCaptionResourceId() != null
                        || (b.getTranscript() != null && !b.getTranscript().isEmpty())))
            fail(400, "video.fields.only");
        if (b.getCaptionResourceId() != null) {
            LearningResource caption = rawResource(b.getCaptionResourceId());
            if (!"CAPTION".equals(caption.getKind())
                    || !r.getChapterId().equals(caption.getChapterId()))
                fail(400, "caption.invalid");
            if ("PUBLISHED".equals(b.getStatus()) && !"PUBLISHED".equals(caption.getStatus()))
                fail(400, "caption.notPublished");
        }
        if ("CAPTION".equals(r.getKind())
                && "DRAFT".equals(b.getStatus())
                && r.getId() != null
                && mapper.captionReferences(r.getId()) > 0) fail(400, "caption.inUse");
        r.setTitle(b.getTitle());
        r.setAltText(b.getAltText());
        r.setCaptionResourceId(b.getCaptionResourceId());
        r.setTranscript(b.getTranscript());
        r.setLanguageCode(b.getLanguageCode());
        r.setStatus(b.getStatus());
    }

    @Transactional
    public Long upload(Long chapterId, MultipartFile file, String kind, ResourceInput b) {
        Chapter c = rawChapter(chapterId);
        editable(c.getCourseId());
        rawChapter(chapterId);
        LearningResource r = new LearningResource();
        r.setChapterId(chapterId);
        r.setKind(kind);
        r.setCreatedBy(actor().getUserId());
        resourceFields(r, b);
        LearningFileStorage.Stored stored = files.save(file, kind);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED)
                            files.discard(stored.key);
                    }
                });
        r.setStorageKey(stored.key);
        r.setMimeType(stored.mime);
        r.setOriginalName(stored.name);
        r.setFileSize(stored.size);
        mapper.insertResource(r);
        record("RESOURCE_UPLOAD", r.getId());
        return r.getId();
    }

    @Transactional
    public void updateResource(Long id, ResourceInput b) {
        LearningResource r = rawResource(id);
        editable(rawChapter(r.getChapterId()).getCourseId());
        r = rawResource(id);
        resourceFields(r, b);
        mapper.updateResource(r);
        record("RESOURCE_EDIT", id);
    }

    @Transactional
    public void deleteResource(Long id) {
        LearningResource r = rawResource(id);
        editable(rawChapter(r.getChapterId()).getCourseId());
        rawResource(id);
        if (mapper.captionReferences(id) > 0) fail(400, "caption.inUse");
        mapper.deleteResource(id);
        record("RESOURCE_DELETE", id);
    }

    public List<CoursePost> posts(Long courseId) {
        course(courseId);
        return mapper.posts(courseId, student(actor()));
    }

    private CoursePost rawPost(Long id) {
        CoursePost p = mapper.post(id);
        if (p == null) fail(404, "post.unavailable");
        return p;
    }

    public CoursePost post(Long id) {
        CoursePost p = rawPost(id);
        readable(get(p.getCourseId(), false), actor());
        if (student(actor()) && !"PUBLISHED".equals(p.getStatus())) fail(403, "post.denied");
        return p;
    }

    private void author(CoursePost p) {
        if (!actor().getUserId().equals(p.getCreatedBy())) fail(403, "post.author.only");
    }

    private void postFields(CoursePost p, PostInput b) {
        String url = b.getMeetingUrl();
        if ("MEETING".equals(b.getType()) && (url == null || url.trim().isEmpty()))
            fail(400, "meeting.link.required");
        if (url != null && !url.isEmpty()) {
            try {
                URI u = new URI(url);
                if (!"https".equalsIgnoreCase(u.getScheme())
                        || u.getHost() == null
                        || u.getUserInfo() != null) fail(400, "meeting.link.invalid");
            } catch (java.net.URISyntaxException e) {
                fail(400, "meeting.link.invalid");
            }
        }
        if (b.getMeetingStartsAt() != null && !b.getMeetingStartsAt().isEmpty()) {
            try {
                OffsetDateTime.parse(b.getMeetingStartsAt());
            } catch (java.time.format.DateTimeParseException e) {
                fail(400, "meeting.time.invalid");
            }
        }
        p.setTitle(b.getTitle());
        p.setBody(b.getBody());
        p.setType(b.getType());
        p.setMeetingUrl(url);
        p.setMeetingStartsAt(b.getMeetingStartsAt());
        p.setStatus(b.getStatus());
        p.setLanguageCode(b.getLanguageCode());
    }

    @Transactional
    public Long createPost(Long courseId, PostInput b) {
        editable(courseId);
        CoursePost p = new CoursePost();
        p.setCourseId(courseId);
        p.setCreatedBy(actor().getUserId());
        postFields(p, b);
        mapper.insertPost(p);
        record("POST_CREATE", p.getId());
        return p.getId();
    }

    @Transactional
    public void updatePost(Long id, PostInput b) {
        CoursePost p = rawPost(id);
        editable(p.getCourseId());
        p = rawPost(id);
        author(p);
        postFields(p, b);
        mapper.updatePost(p);
        record("POST_EDIT", id);
    }

    @Transactional
    public void offlinePost(Long id) {
        CoursePost p = rawPost(id);
        editable(p.getCourseId());
        if (!admin(actor())) author(p);
        mapper.offlinePost(id);
        record("POST_OFFLINE", id);
    }

    @Transactional
    public void deletePost(Long id) {
        CoursePost p = rawPost(id);
        editable(p.getCourseId());
        p = rawPost(id);
        author(p);
        mapper.deletePost(id);
        record("POST_DELETE", id);
    }
}
