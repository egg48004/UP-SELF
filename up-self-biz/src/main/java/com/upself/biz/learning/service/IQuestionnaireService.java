package com.upself.biz.learning.service;

import com.upself.biz.learning.domain.Questionnaire;
import com.upself.biz.learning.dto.QuestionnaireBodies.*;
import com.upself.biz.learning.dto.QuestionnaireView;
import java.util.List;

public interface IQuestionnaireService {
  void verifyStudent();

  void verifyMentor();

  List<QuestionnaireView> visible();

  QuestionnaireView visibleById(Long id);

  List<Questionnaire> authored();

  Long create(Create body);

  void update(Long id, Update body);

  void visibility(Long id, Visibility body);

  void delete(Long id, Integer version);
}
