package com.upself.biz.business.service.impl;

import com.upself.biz.business.domain.BusinessIdea;
import com.upself.biz.business.dto.IdeaBodies.*;
import com.upself.biz.business.mapper.BusinessIdeaMapper;
import com.upself.biz.business.service.IBusinessIdeaService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessIdeaServiceImpl implements IBusinessIdeaService {
  @Autowired private BusinessIdeaMapper mapper;

  public void verifyStudent() {
    if (!"student".equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "Business practice is available to students only.");
  }

  private Long owner() {
    verifyStudent();
    return SecurityUtils.getUserId();
  }

  public List<BusinessIdea> list() {
    return mapper.list(owner());
  }

  public BusinessIdea detail(Long id) {
    BusinessIdea idea = mapper.own(id, owner());
    if (idea == null) throw new ServiceException(404, "This idea is unavailable.");
    return idea;
  }

  private String text(String value, int max) {
    String s = value == null ? "" : value.trim();
    if (s.length() > max) throw new ServiceException(400, "Check the length of your answer.");
    return s;
  }

  private BusinessIdea fields(Create b) {
    BusinessIdea i = new BusinessIdea();
    i.setStudentId(owner());
    i.setTitle(text(b.getTitle(), 100));
    if (i.getTitle().isEmpty()) throw new ServiceException(400, "Enter an idea name.");
    i.setOffer(text(b.getOffer(), 500));
    i.setAudience(text(b.getAudience(), 500));
    i.setResources(text(b.getResources(), 500));
    i.setNextStep(text(b.getNextStep(), 500));
    return i;
  }

  @Transactional
  public Long create(Create b) {
    BusinessIdea i = fields(b);
    mapper.insert(i);
    return i.getId();
  }

  @Transactional
  public void edit(Long id, Update b) {
    BusinessIdea i = fields(b);
    i.setId(id);
    i.setVersion(b.getVersion());
    if (mapper.edit(i) != 1) {
      if (mapper.own(id, i.getStudentId()) == null)
        throw new ServiceException(404, "This idea is unavailable.");
      throw new ServiceException(409, "This idea has changed. Refresh before editing again.");
    }
  }
}
