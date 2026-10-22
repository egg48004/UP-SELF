package com.upself.biz.assignment.service;

import com.upself.biz.assignment.domain.*;
import com.upself.biz.assignment.dto.AssignmentBodies.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IAssignmentService {
  List<Assignment> list(String keyword, Long courseId, boolean pending);

  Assignment detail(Long id);

  Long create(AssignmentInput body);

  void update(Long id, AssignmentInput body);

  Submission mine(Long id);

  Submission saveDraft(Long id, DraftInput body);

  Submission submit(Long id, SubmitInput body);

  Submission upload(Long id, int version, MultipartFile file);

  Submission removeFile(Long id, Long fileId, int version);

  List<Submission> submissions(Long id);

  Submission reviewDetail(Long id, Long submissionId);

  Submission review(Long id, Long submissionId, ReviewInput body);

  SubmissionFile readFile(Long fileId);
}
