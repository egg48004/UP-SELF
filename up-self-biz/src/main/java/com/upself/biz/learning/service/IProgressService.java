package com.upself.biz.learning.service;
import com.upself.biz.learning.dto.ProgressViews.*;
import java.util.List;
public interface IProgressService {
 void verifyStudent();
 void verifyMentor();
 Summary summary(Long courseId);
 void visit(Long chapterId);
 void complete(Long chapterId,boolean completed);
 Item resume();
 List<Item> history();
 List<Overview> overview(String keyword,Long courseId);
}
