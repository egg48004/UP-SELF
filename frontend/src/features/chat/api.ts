import { data, request, table, download } from "../../shared/http/client";
import type {
  ChatGroup,
  ChatMember,
  ChatMessage,
  ChatFile,
  MessageBatch,
} from "./types";
export function createChatApi(student: boolean) {
  const base = student ? "/client/chat" : "/chat";
  return {
    groups: (courseId: number, current = 1) =>
      table<ChatGroup>(`${base}/courses/${courseId}/groups`, {
        params: { current, pageSize: 10 },
      }),
    create: (courseId: number, title: string) =>
      data<number>(`${base}/courses/${courseId}/groups`, {
        method: "POST",
        data: { title },
      }),
    detail: (id: number, signal?: AbortSignal) =>
      data<ChatGroup>(`${base}/groups/${id}`, { signal }),
    update: (g: ChatGroup) =>
      request(`${base}/groups/${g.id}`, {
        method: "PUT",
        data: { title: g.title, status: g.status, version: g.version },
      }),
    members: (id: number) => data<ChatMember[]>(`${base}/groups/${id}/members`),
    candidates: (id: number) =>
      data<ChatMember[]>(`${base}/groups/${id}/candidates`),
    add: (id: number, studentId: number) =>
      request(`${base}/groups/${id}/members`, {
        method: "POST",
        data: { studentId },
      }),
    remove: (id: number, studentId: number) =>
      request(`${base}/groups/${id}/members/${studentId}`, {
        method: "DELETE",
      }),
    messages: (
      id: number,
      cursor: { afterId?: number; beforeId?: number } = {},
      signal?: AbortSignal,
    ) =>
      data<MessageBatch>(`${base}/groups/${id}/messages`, {
        params: cursor,
        signal,
      }),
    send: (id: number, body: string, fileIds: number[], clientKey: string) =>
      data<ChatMessage>(`${base}/groups/${id}/messages`, {
        method: "POST",
        data: { body, fileIds, clientKey },
      }),
    drafts: (id: number) =>
      data<ChatFile[]>(`${base}/groups/${id}/draft-files`),
    upload: (id: number, file: File, altText: string) => {
      const form = new FormData();
      form.append("file", file);
      form.append("altText", altText);
      return data<ChatFile>(`${base}/groups/${id}/files`, {
        method: "POST",
        data: form,
        timeout: 180000,
      });
    },
    removeFile: (id: number) =>
      request(`${base}/files/${id}`, { method: "DELETE" }),
    file: (id: number) => download(`${base}/files/${id}/content`),
  };
}
export type ChatApi = ReturnType<typeof createChatApi>;
