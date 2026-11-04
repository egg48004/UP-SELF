package com.upself.biz.learning.mapper;

import com.upself.biz.learning.domain.Questionnaire;
import com.upself.biz.learning.dto.QuestionnaireView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface QuestionnaireMapper {
  List<QuestionnaireView> visible();

  QuestionnaireView visibleById(@Param("id") Long id);

  List<Questionnaire> authored(@Param("owner") Long owner);

  Questionnaire owned(@Param("id") Long id, @Param("owner") Long owner);

  int insert(Questionnaire questionnaire);

  int update(@Param("item") Questionnaire item, @Param("expected") Integer expected);

  int visibility(
      @Param("id") Long id,
      @Param("owner") Long owner,
      @Param("visible") boolean visible,
      @Param("expected") Integer expected);

  int delete(@Param("id") Long id, @Param("owner") Long owner, @Param("expected") Integer expected);
}
