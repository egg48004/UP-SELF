export interface Recipient {
  id: number;
  name: string;
  username: string;
}
export interface SentMessage {
  id: number;
  body: string;
  recipientCount: number;
  sentAt: string;
}
export interface InboxMessage {
  id: number;
  body: string;
  mentorName: string;
  sentAt: string;
}
export interface SentDetails {
  message: SentMessage;
  recipients: Recipient[];
}
export interface SendRequest {
  body: string;
  studentIds: number[];
  clientKey: string;
}
