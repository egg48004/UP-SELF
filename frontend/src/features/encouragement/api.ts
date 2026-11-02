import { data, table } from "../../shared/http/client";
import type {
  Recipient,
  SentMessage,
  SentDetails,
  InboxMessage,
  SendRequest,
} from "./types";
export const mentorEncouragementApi = {
  students: (current = 1, keyword = "") =>
    table<Recipient>("/encouragement/students", {
      params: { current, pageSize: 10, keyword },
    }),
  student: (id: number) => data<Recipient>(`/encouragement/students/${id}`),
  send: (body: SendRequest) =>
    data<SentMessage>("/encouragement/messages", {
      method: "POST",
      data: body,
    }),
  history: (current = 1) =>
    table<SentMessage>("/encouragement/sent", {
      params: { current, pageSize: 10 },
    }),
  sent: (id: number) => data<SentDetails>(`/encouragement/sent/${id}`),
};
export const studentEncouragementApi = {
  inbox: (current = 1) =>
    table<InboxMessage>("/client/encouragement/inbox", {
      params: { current, pageSize: 10 },
    }),
};
