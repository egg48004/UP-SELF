package com.upself.biz.chat.mapper;

import com.upself.biz.chat.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ChatMapper {
  List<ChatGroup> groups(
      @Param("courseId") Long courseId,
      @Param("studentId") Long studentId,
      @Param("actorId") Long actorId);

  ChatGroup group(Long id);

  ChatGroup lockGroup(Long id);

  int insertGroup(ChatGroup group);

  int updateGroup(ChatGroup group);

  int isMember(@Param("groupId") Long groupId, @Param("studentId") Long studentId);

  List<ChatMember> members(@Param("groupId") Long groupId, @Param("viewerId") Long viewerId);

  List<ChatMember> candidates(Long courseId);

  int eligible(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

  int addMember(@Param("groupId") Long groupId, @Param("studentId") Long studentId);

  int removeMember(@Param("groupId") Long groupId, @Param("studentId") Long studentId);

  List<ChatMessage> messages(
      @Param("groupId") Long groupId,
      @Param("afterId") Long afterId,
      @Param("beforeId") Long beforeId,
      @Param("viewerId") Long viewerId,
      @Param("ceiling") Long ceiling);

  Long latestMessageId(Long groupId);

  List<ChatFile> batchMessageFiles(@Param("ids") List<Long> ids);

  List<Long> hiddenStudents(@Param("groupId") Long groupId, @Param("viewerId") Long viewerId);

  int hidePeer(
      @Param("groupId") Long groupId,
      @Param("viewerId") Long viewerId,
      @Param("studentId") Long studentId);

  int showPeer(
      @Param("groupId") Long groupId,
      @Param("viewerId") Long viewerId,
      @Param("studentId") Long studentId);

  int hiddenMessage(@Param("id") Long id, @Param("viewerId") Long viewerId);

  ChatMessage message(Long id);

  ChatMessage byKey(
      @Param("groupId") Long groupId,
      @Param("senderId") Long senderId,
      @Param("clientKey") String key);

  int insertMessage(ChatMessage message);

  List<ChatFile> messageFiles(Long messageId);

  ChatFile file(Long id);

  int insertFile(ChatFile file);

  int deleteFile(Long id);

  int draftCount(@Param("groupId") Long groupId, @Param("ownerId") Long ownerId);

  int attach(@Param("id") Long id, @Param("messageId") Long messageId);

  List<ChatFile> drafts(@Param("groupId") Long groupId, @Param("ownerId") Long ownerId);
}
