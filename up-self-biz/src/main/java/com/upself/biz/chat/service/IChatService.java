package com.upself.biz.chat.service;
import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.*;
import java.util.*;
import org.springframework.web.multipart.MultipartFile;
public interface IChatService {
 void verifyCourse(Long courseId);
 List<ChatGroup> groups(Long courseId); ChatGroup detail(Long id);
 Long create(Long courseId,GroupInput body); void update(Long id,GroupInput body);
 List<ChatMember> members(Long id); List<ChatMember> candidates(Long id);
 void addMember(Long id,Long studentId); void removeMember(Long id,Long studentId);
 Map<String,Object> messages(Long id,Long afterId,Long beforeId);
 ChatMessage send(Long id,SendInput body);
 ChatFile upload(Long id,MultipartFile file,String altText);
 List<ChatFile> drafts(Long id); void deleteFile(Long id); ChatFile readFile(Long id);
}
