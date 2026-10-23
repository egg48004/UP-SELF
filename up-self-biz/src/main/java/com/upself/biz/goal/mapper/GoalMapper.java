package com.upself.biz.goal.mapper;

import com.upself.biz.goal.domain.PersonalGoal;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface GoalMapper {
    List<PersonalGoal> list(@Param("studentId") Long studentId, @Param("status") String status);
    PersonalGoal own(@Param("id") Long id, @Param("studentId") Long studentId);
    int insert(PersonalGoal goal);
    int edit(@Param("id") Long id, @Param("studentId") Long studentId,
             @Param("version") Integer version, @Param("title") String title,
             @Param("details") String details);
    int complete(@Param("id") Long id, @Param("studentId") Long studentId,
                 @Param("version") Integer version, @Param("completed") boolean completed);
    int delete(@Param("id") Long id, @Param("studentId") Long studentId,
               @Param("version") Integer version);
}
