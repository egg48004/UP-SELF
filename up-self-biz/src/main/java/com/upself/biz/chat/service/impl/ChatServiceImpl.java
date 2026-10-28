package com.upself.biz.chat.service.impl;
import com.upself.biz.chat.domain.*;
import com.upself.biz.chat.dto.ChatBodies.*;
import com.upself.biz.chat.mapper.ChatMapper;
import com.upself.biz.chat.service.*;
import com.upself.biz.file.service.PrivateFileStorage;
import com.upself.biz.learning.domain.Course;
import com.upself.biz.learning.mapper.LearningMapper;
import com.upself.biz.recording.service.RecordingStorage;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;
import com.upself.system.mapper.SysAuditMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service public class ChatServiceImpl implements IChatService {
 @Autowired private ChatMapper mapper;
 @Autowired private LearningMapper learning;
 @Autowired private ChatFileStorage files;
 @Autowired private SysAuditMapper audit;
 private Long actor() { return SecurityUtils.getUserId(); }
 private boolean student() { return "student".equals(SecurityUtils.getLoginUser().getRoleKey()); }
 private void participant() {
  if (!student() && !"mentor".equals(SecurityUtils.getLoginUser().getRoleKey())) denied();
 }
 private void denied() { throw new ServiceException(403,"This discussion is not available to you."); }
 private Course course(Long id,boolean lock) {
  participant(); Course c=lock?learning.lockCourse(id):learning.course(id);
  if(c==null) throw new ServiceException(404,"This course is unavailable.");
  if(student()) {
   if(!"PUBLISHED".equals(c.getStatus()) || mapper.eligible(id,actor())!=1) denied();
  } else if(!actor().equals(c.getMentorId())) denied();
  return c;
 }
 private ChatGroup group(Long id,boolean lock,boolean write,boolean manager) {
  participant(); ChatGroup initial=mapper.group(id);
  if(initial==null) throw new ServiceException(404,"This discussion is unavailable.");
  // Same course lock as enrollment and mentor reassignment, then the group lock.
  Course c=course(initial.getCourseId(),lock);
  ChatGroup g=lock?mapper.lockGroup(id):initial;
  if(manager && student()) denied();
  if(student() && mapper.isMember(id,actor())!=1) denied();
  if(write && !"PUBLISHED".equals(c.getStatus())) throw new ServiceException(409,"Discussions are writable in published courses only.");
  if(write && !"ACTIVE".equals(g.getStatus())) throw new ServiceException(409,"This discussion is closed. You can still read its messages.");
  return g;
 }
 public void verifyCourse(Long id) { course(id,false); }
 public List<ChatGroup> groups(Long id) { participant(); return mapper.groups(id,student()?actor():null,actor()); }
 public ChatGroup detail(Long id) { return group(id,false,false,false); }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Long create(Long id,GroupInput b) {
  if(student()) denied(); Course c=course(id,true);
  if(!"PUBLISHED".equals(c.getStatus())) throw new ServiceException(409,"Publish the course before creating a discussion.");
  ChatGroup g=new ChatGroup();g.setCourseId(id);g.setTitle(b.getTitle().trim());g.setStatus("ACTIVE");g.setCreatedBy(actor());mapper.insertGroup(g);
  audit.insert(actor(),"CHAT_CREATE",g.getId().toString(),"SUCCESS");return g.getId();
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void update(Long id,GroupInput b) {
  ChatGroup g=group(id,true,false,true); Course c=learning.course(g.getCourseId());
  if(!"PUBLISHED".equals(c.getStatus())) throw new ServiceException(409,"Archived or draft course discussions are read-only.");
  if(!g.getVersion().equals(b.getVersion())) throw new ServiceException(409,"This discussion has changed. Refresh before saving.");
  g.setTitle(b.getTitle().trim());g.setStatus(b.getStatus());
  if(mapper.updateGroup(g)!=1) throw new ServiceException(409,"This discussion has changed. Refresh before saving.");
  audit.insert(actor(),"CHAT_UPDATE",id.toString(),"SUCCESS");
 }
 public List<ChatMember> members(Long id) { group(id,false,false,false);return mapper.members(id); }
 public List<ChatMember> candidates(Long id) { ChatGroup g=group(id,false,false,true);return mapper.candidates(g.getCourseId()); }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void addMember(Long id,Long studentId) {
  ChatGroup g=group(id,true,true,true);
  if(mapper.eligible(g.getCourseId(),studentId)!=1) throw new ServiceException(400,"Choose an active student enrolled in this course.");
  mapper.addMember(id,studentId);audit.insert(actor(),"CHAT_MEMBER_ADD",id+":"+studentId,"SUCCESS");
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void removeMember(Long id,Long studentId) {
  group(id,true,false,true);mapper.removeMember(id,studentId);audit.insert(actor(),"CHAT_MEMBER_REMOVE",id+":"+studentId,"SUCCESS");
 }
 public Map<String,Object> messages(Long id,Long after,Long before) {
  group(id,false,false,false);
  if((after!=null && after<0)||(before!=null && before<1)||(after!=null && before!=null)) throw new ServiceException(400,"Choose one valid message cursor.");
  List<ChatMessage> rows=mapper.messages(id,after,before);boolean more=rows.size()>30;
  if(more) rows.remove(30);
  if(after==null) Collections.reverse(rows);
  for(ChatMessage m:rows)m.setFiles(mapper.messageFiles(m.getId()));
  Map<String,Object> result=new HashMap<>();result.put("rows",rows);result.put("hasMore",more);return result;
 }
 private ChatMessage hydrate(Long id) { ChatMessage m=mapper.message(id);m.setFiles(mapper.messageFiles(id));return m; }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ChatMessage send(Long id,SendInput b) {
  group(id,true,true,false);String text=b.getBody().trim();
  Set<Long> ids=new LinkedHashSet<>(b.getFileIds());
  if(ids.size()!=b.getFileIds().size()) throw new ServiceException(400,"Each attachment can be added once.");
  ChatMessage previous=mapper.byKey(id,actor(),b.getClientKey());
  if(previous!=null) {
   List<Long> saved=new ArrayList<>();for(ChatFile f:mapper.messageFiles(previous.getId()))saved.add(f.getId());
   if(!text.equals(previous.getBody()) || !new HashSet<>(saved).equals(ids)) throw new ServiceException(409,"This send request was already used. Refresh the discussion.");
   return hydrate(previous.getId());
  }
  if(text.isEmpty() && ids.isEmpty()) throw new ServiceException(400,"Write a message or add a file.");
  for(Long fid:ids) {
   ChatFile f=mapper.file(fid);
   if(f==null || !id.equals(f.getGroupId()) || !actor().equals(f.getOwnerId()) || f.getMessageId()!=null) throw new ServiceException(403,"You can send only your own unsent attachments from this discussion.");
  }
  ChatMessage m=new ChatMessage();m.setGroupId(id);m.setSenderId(actor());m.setBody(text);m.setClientKey(b.getClientKey());mapper.insertMessage(m);
  for(Long fid:ids) if(mapper.attach(fid,m.getId())!=1) throw new ServiceException(409,"An attachment has changed. Refresh before sending.");
  // Group writes are serialized: a committed cursor cannot skip an earlier uncommitted message.
  return hydrate(m.getId());
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ChatFile upload(Long id,MultipartFile file,String alt) {
  group(id,true,true,false);
  if(mapper.draftCount(id,actor())>=5) throw new ServiceException(400,"Send or remove your five pending attachments first.");
  String name=file==null?null:file.getOriginalFilename();boolean audio=RecordingStorage.audioName(name);
  if(file==null || file.isEmpty() || file.getSize()>(audio?5L:20L)*1024*1024) throw new ServiceException(400,audio?"Use an audio file up to 5 MB.":"Use a file up to 20 MB.");
  String description=alt==null?"":alt.trim();if(description.length()>200) throw new ServiceException(400,"Use at most 200 characters for the image description.");
  PrivateFileStorage.Stored saved=files.save(file);
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)files.discard(saved.key);}});
  if(saved.mime.startsWith("image/") && description.isEmpty()) throw new ServiceException(400,"Add a short image description before uploading.");
  ChatFile f=new ChatFile();f.setGroupId(id);f.setOwnerId(actor());f.setStorageKey(saved.key);f.setOriginalName(saved.name);f.setMimeType(saved.mime);f.setSizeBytes(saved.size);f.setAltText(description);mapper.insertFile(f);return f;
 }
 public List<ChatFile> drafts(Long id) { group(id,false,false,false);return mapper.drafts(id,actor()); }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void deleteFile(Long id) {
  ChatFile initial=mapper.file(id);if(initial==null)throw new ServiceException(404,"This attachment is unavailable.");
  group(initial.getGroupId(),true,false,false);ChatFile f=mapper.file(id);
  if(f==null || !actor().equals(f.getOwnerId()) || f.getMessageId()!=null) throw new ServiceException(403,"Only your own unsent attachments can be removed.");
  mapper.deleteFile(id);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){files.discard(f.getStorageKey());}});
 }
 public ChatFile readFile(Long id) {
  ChatFile f=mapper.file(id);if(f==null)throw new ServiceException(404,"This attachment is unavailable.");
  group(f.getGroupId(),false,false,false);
  if(f.getMessageId()==null && !actor().equals(f.getOwnerId())) denied();
  return f;
 }
}
