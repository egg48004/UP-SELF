package com.upself.biz.business.mapper;

import com.upself.biz.business.domain.BusinessIdea;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BusinessIdeaMapper {
  List<BusinessIdea> list(@Param("studentId") Long studentId);

  BusinessIdea own(@Param("id") Long id, @Param("studentId") Long studentId);

  int insert(BusinessIdea idea);

  int edit(BusinessIdea idea);
}
