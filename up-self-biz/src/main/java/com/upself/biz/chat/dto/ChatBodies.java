package com.upself.biz.chat.dto;
import java.util.*;
import javax.validation.constraints.*;
import lombok.Data;
public class ChatBodies {
 @Data public static class GroupInput {
  @NotBlank(message="Enter a group name.") @Size(max=100,message="Use at most 100 characters for the group name.") private String title;
  @NotNull(message="Refresh the group before saving.") @Min(value=0,message="Refresh the group before saving.") private Integer version=0;
  @Pattern(regexp="ACTIVE|CLOSED",message="Choose Active or Closed.") @NotNull(message="Choose a group status.") private String status="ACTIVE";
 }
 @Data public static class MemberInput {
  @NotNull(message="Choose a student.") @Min(value=1,message="Choose a valid student.") private Long studentId;
 }
 @Data public static class SendInput {
  @NotNull(message="Enter a message or add a file.") @Size(max=2000,message="Use at most 2000 characters per message.") private String body="";
  @NotNull(message="Choose up to five attachments.") @Size(max=5,message="Choose up to five attachments.") private List<@NotNull(message="Choose a valid file.") @Min(value=1,message="Choose a valid file.") Long> fileIds=new ArrayList<>();
  @NotBlank(message="Refresh before sending.") @Pattern(regexp="[A-Za-z0-9_-]{16,64}",message="Refresh before sending.") private String clientKey;
 }
}
