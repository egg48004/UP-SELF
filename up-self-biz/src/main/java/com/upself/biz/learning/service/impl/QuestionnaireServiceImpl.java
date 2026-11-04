package com.upself.biz.learning.service.impl;

import com.upself.biz.learning.domain.Questionnaire;
import com.upself.biz.learning.dto.QuestionnaireBodies.*;
import com.upself.biz.learning.dto.QuestionnaireView;
import com.upself.biz.learning.mapper.QuestionnaireMapper;
import com.upself.biz.learning.service.IQuestionnaireService;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionnaireServiceImpl implements IQuestionnaireService {
  @Autowired private QuestionnaireMapper mapper;
  @Autowired private SysAuditMapper audit;

  private Long actor() {
    return SecurityUtils.getLoginUser().getUserId();
  }

  private void role(String expected) {
    if (!expected.equals(SecurityUtils.getLoginUser().getRoleKey()))
      throw new ServiceException(403, "This action is not available for your role.");
  }

  public void verifyStudent() {
    role("student");
  }

  public void verifyMentor() {
    role("mentor");
  }

  public List<QuestionnaireView> visible() {
    verifyStudent();
    return mapper.visible();
  }

  public QuestionnaireView visibleById(Long id) {
    verifyStudent();
    QuestionnaireView item = mapper.visibleById(id);
    if (item == null) throw new ServiceException(404, "This questionnaire is unavailable.");
    return item;
  }

  public List<Questionnaire> authored() {
    verifyMentor();
    return mapper.authored(actor());
  }

  private Questionnaire fields(Create body) {
    String title = body.getTitle() == null ? "" : body.getTitle().trim();
    if (title.isEmpty() || title.length() > 150)
      throw new ServiceException(400, "Enter a title using 1 to 150 characters.");
    String url = body.getUrl() == null ? "" : body.getUrl().trim();
    try {
      URI parsed = new URI(url);
      if (url.isEmpty()
          || url.length() > 2000
          || url.chars().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c))
          || !"https".equalsIgnoreCase(parsed.getScheme())
          || parsed.getHost() == null
          || parsed.getRawUserInfo() != null
          || parsed.getPort() == 0
          || parsed.getPort() > 65535) throw new URISyntaxException(url, "HTTPS required");
    } catch (URISyntaxException bad) {
      throw new ServiceException(
          400, "Enter a valid HTTPS questionnaire link without login details.");
    }
    if (body.getVisible() == null)
      throw new ServiceException(400, "Choose whether this questionnaire is visible.");
    Questionnaire item = new Questionnaire();
    item.setTitle(title);
    item.setUrl(url);
    item.setVisible(body.getVisible());
    item.setCreatedBy(actor());
    return item;
  }

  private void version(Integer value) {
    if (value == null || value < 0)
      throw new ServiceException(400, "Refresh the questionnaire and try again.");
  }

  private void changed(int rows, Long id) {
    if (rows == 1) return;
    if (mapper.owned(id, actor()) == null)
      throw new ServiceException(404, "This questionnaire is unavailable.");
    throw new ServiceException(409, "This questionnaire has changed. Refresh and try again.");
  }

  @Transactional
  public Long create(Create body) {
    verifyMentor();
    Questionnaire item = fields(body);
    mapper.insert(item);
    audit.insert(actor(), "QUESTIONNAIRE_CREATE", item.getId().toString(), "SUCCESS");
    return item.getId();
  }

  @Transactional
  public void update(Long id, Update body) {
    verifyMentor();
    version(body.getVersion());
    Questionnaire item = fields(body);
    item.setId(id);
    changed(mapper.update(item, body.getVersion()), id);
    audit.insert(actor(), "QUESTIONNAIRE_UPDATE", id.toString(), "SUCCESS");
  }

  @Transactional
  public void visibility(Long id, Visibility body) {
    verifyMentor();
    version(body.getVersion());
    if (body.getVisible() == null) throw new ServiceException(400, "Choose Visible or Hidden.");
    changed(mapper.visibility(id, actor(), body.getVisible(), body.getVersion()), id);
    audit.insert(actor(), "QUESTIONNAIRE_VISIBILITY", id.toString(), "SUCCESS");
  }

  @Transactional
  public void delete(Long id, Integer expected) {
    verifyMentor();
    version(expected);
    changed(mapper.delete(id, actor(), expected), id);
    audit.insert(actor(), "QUESTIONNAIRE_DELETE", id.toString(), "SUCCESS");
  }
}
