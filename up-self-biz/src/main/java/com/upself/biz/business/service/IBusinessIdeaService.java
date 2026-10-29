package com.upself.biz.business.service;

import com.upself.biz.business.domain.BusinessIdea;
import com.upself.biz.business.dto.IdeaBodies.*;
import java.util.List;

public interface IBusinessIdeaService {
  void verifyStudent();

  List<BusinessIdea> list();

  BusinessIdea detail(Long id);

  Long create(Create body);

  void edit(Long id, Update body);
}
