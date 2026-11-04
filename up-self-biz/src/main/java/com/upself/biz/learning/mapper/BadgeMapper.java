package com.upself.biz.learning.mapper;

import com.upself.biz.learning.domain.BadgeCode;
import com.upself.biz.learning.dto.BadgeView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BadgeMapper {
  List<BadgeView> own(@Param("studentId") Long studentId);
  int insert(@Param("studentId") Long studentId, @Param("code") BadgeCode code);
}
