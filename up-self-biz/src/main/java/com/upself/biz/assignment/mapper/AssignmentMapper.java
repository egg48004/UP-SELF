package com.upself.biz.assignment.mapper;

import com.upself.biz.assignment.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AssignmentMapper {

  List<Assignment> list(
      @Param("userId") Long userId,
      @Param("role") String role,
      @Param("keyword") String keyword,
      @Param("courseId") Long courseId,
      @Param("pending") boolean pending);

  Assignment assignment(@Param("id") Long id);

  Assignment lockAssignment(@Param("id") Long id);

  int insertAssignment(Assignment a);

  int updateAssignment(Assignment a);

  Submission submission(
      @Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

  Submission lockSubmission(
      @Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

  Submission submissionById(@Param("id") Long id);

  int createDraft(Submission s);

  int updateSubmission(Submission s);

  List<Submission> submissions(
      @Param("assignmentId") Long assignmentId, @Param("mentorId") Long mentorId);

  List<Attempt> attempts(@Param("submissionId") Long submissionId);

  Attempt attemptByKey(
      @Param("submissionId") Long submissionId, @Param("requestKey") String requestKey);

  int insertAttempt(Attempt a);

  int reviewAttempt(Attempt a);

  List<SubmissionFile> files(
      @Param("submissionId") Long submissionId, @Param("attemptNo") Integer attemptNo);

  SubmissionFile file(@Param("id") Long id);

  int insertFile(SubmissionFile f);

  int removeFile(@Param("id") Long id);

  int attachFiles(@Param("submissionId") Long submissionId, @Param("attemptNo") Integer attemptNo);
}
