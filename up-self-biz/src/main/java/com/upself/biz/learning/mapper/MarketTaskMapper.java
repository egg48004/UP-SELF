package com.upself.biz.learning.mapper;

import com.upself.biz.learning.domain.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface MarketTaskMapper {
  List<MarketTask> list(
      @Param("course") Long course, @Param("user") Long user, @Param("role") String role);

  MarketTask find(@Param("id") Long id);

  MarketTask lock(@Param("id") Long id);

  List<MarketOption> options(@Param("id") Long id);

  int insert(MarketTask task);

  int update(MarketTask task);

  int state(MarketTask task);

  int deleteOptions(@Param("id") Long id);

  int insertOption(MarketOption option);

  int attemptCount(@Param("id") Long id);

  MarketAttempt byKey(@Param("id") Long id, @Param("user") Long user, @Param("key") String key);

  MarketAttempt attempt(@Param("id") Long id);

  int insertAttempt(MarketAttempt attempt);

  int reflect(MarketAttempt attempt);

  List<MarketAttempt> attempts(@Param("id") Long id, @Param("user") Long user);

  List<Map<String, Object>> participants(@Param("id") Long id);
}
