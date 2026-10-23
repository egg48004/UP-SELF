package com.upself.biz.goal.service.impl;

import com.upself.biz.goal.domain.PersonalGoal;
import com.upself.biz.goal.dto.GoalBodies.*;
import com.upself.biz.goal.mapper.GoalMapper;
import com.upself.biz.goal.service.IGoalService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalServiceImpl implements IGoalService {
    @Autowired private GoalMapper mapper;

    private Long studentId() {
        verifyStudent();
        return SecurityUtils.getLoginUser().getUserId();
    }

    public void verifyStudent() {
        if (!"student".equals(SecurityUtils.getLoginUser().getRoleKey())) {
            throw new ServiceException(403, "Personal goals are available to students only.");
        }
    }

    public List<PersonalGoal> list(String status) {
        Long owner = studentId();
        if (status != null && !status.isEmpty()
                && !"ACTIVE".equals(status) && !"COMPLETED".equals(status)) {
            throw new ServiceException(400, "Choose All goals, Active or Completed.");
        }
        return mapper.list(owner, status);
    }

    public PersonalGoal detail(Long id) {
        PersonalGoal goal = mapper.own(id, studentId());
        if (goal == null) throw new ServiceException(404, "This goal is unavailable.");
        return goal;
    }

    private String title(Create body) {
        String title = body.getTitle() == null ? "" : body.getTitle().trim();
        if (title.isEmpty() || title.length() > 150) {
            throw new ServiceException(400, "Enter a goal using 1 to 150 characters.");
        }
        if (body.getDetails() != null && body.getDetails().length() > 2000) {
            throw new ServiceException(400, "Use at most 2000 characters for your notes.");
        }
        return title;
    }

    @Transactional
    public Long create(Create body) {
        Long owner = studentId();
        PersonalGoal goal = new PersonalGoal();
        goal.setStudentId(owner);
        goal.setTitle(title(body));
        goal.setDetails(body.getDetails() == null ? "" : body.getDetails().trim());
        mapper.insert(goal);
        return goal.getId();
    }

    // Each mutation checks owner and version in the same SQL statement.
    private void changed(int rows, Long id, Long owner) {
        if (rows > 0) return;
        if (mapper.own(id, owner) == null) {
            throw new ServiceException(404, "This goal is unavailable.");
        }
        throw new ServiceException(409, "This goal has changed. Refresh your goals and try again.");
    }

    @Transactional
    public void edit(Long id, Update body) {
        Long owner = studentId();
        changed(mapper.edit(id, owner, body.getVersion(), title(body),
                body.getDetails() == null ? "" : body.getDetails().trim()), id, owner);
    }

    @Transactional
    public void complete(Long id, Completion body) {
        Long owner = studentId();
        changed(mapper.complete(id, owner, body.getVersion(), body.getCompleted()), id, owner);
    }

    @Transactional
    public void delete(Long id, Integer version) {
        Long owner = studentId();
        if (version == null || version < 0) {
            throw new ServiceException(400, "Refresh your goals and try again.");
        }
        changed(mapper.delete(id, owner, version), id, owner);
    }
}
