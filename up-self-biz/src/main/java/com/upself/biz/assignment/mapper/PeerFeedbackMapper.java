package com.upself.biz.assignment.mapper;

import com.upself.biz.assignment.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PeerFeedbackMapper {
  int attempts(@Param("id") Long id);

  PeerFeedback own(@Param("id") Long id, @Param("user") Long user);

  Submission candidate(@Param("id") Long id, @Param("user") Long user);

  int insert(PeerFeedback row);

  PeerFeedback find(@Param("id") Long id);

  List<PeerFeedback> received(@Param("id") Long id, @Param("user") Long user);

  List<PeerFeedback> managed(@Param("id") Long id);

  int send(PeerFeedback row);

  int hide(PeerFeedback row);

  int fileGrant(
      @Param("id") Long id,
      @Param("user") Long user,
      @Param("submission") Long submission,
      @Param("attempt") Integer attempt);
}
