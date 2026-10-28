export interface ChatGroup {
  id: number;
  courseId: number;
  title: string;
  status: "ACTIVE" | "CLOSED";
  version: number;
  memberCount?: number;
}
export interface ChatMember {
  studentId: number;
  nickName: string;
  userName: string;
  active: boolean;
}
export interface ChatFile {
  id: number;
  groupId: number;
  ownerId: number;
  messageId: number | null;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  altText: string;
}
export interface ChatMessage {
  id: number;
  groupId: number;
  senderId: number;
  senderName: string;
  body: string;
  clientKey: string;
  createTime: string;
  files: ChatFile[];
}
export interface MessageBatch {
  rows: ChatMessage[];
  hasMore: boolean;
}
