package com.upself.biz.learning.dto;

import lombok.Data;

/** Student projection: visible platform links only, without author/edit metadata. */
@Data
public class QuestionnaireView {
  private Long id;
  private String title, url;
}
