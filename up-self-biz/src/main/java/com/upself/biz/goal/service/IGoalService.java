package com.upself.biz.goal.service;

import com.upself.biz.goal.domain.PersonalGoal;
import com.upself.biz.goal.dto.GoalBodies.*;
import java.util.List;

public interface IGoalService {
    void verifyStudent();
    List<PersonalGoal> list(String status);
    PersonalGoal detail(Long id);
    Long create(Create body);
    void edit(Long id, Update body);
    void complete(Long id, Completion body);
    void delete(Long id, Integer version);
}
