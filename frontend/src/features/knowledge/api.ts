import { data, request, table } from "../../shared/http/client";
import type {
  KnowledgeAnswer,
  KnowledgeChoice,
  KnowledgeQuestion,
  QuestionInput,
} from "./types";
const base = "/knowledge";
export const knowledgeApi = {
  own: (current: number, courseId?: number) =>
    table<KnowledgeQuestion>("/client/knowledge/questions", {
      params: { current, pageSize: 10, courseId },
    }),
  manage: (current: number, courseId?: number) =>
    table<KnowledgeQuestion>(base + "/questions/manage", {
      params: { current, pageSize: 10, courseId },
    }),
  create: (courseId: number, body: QuestionInput) =>
    data<number>(`${base}/courses/${courseId}/questions`, {
      method: "POST",
      data: body,
    }),
  update: (id: number, body: QuestionInput) =>
    request(`${base}/questions/${id}`, { method: "PUT", data: body }),
  remove: (id: number, version: number) =>
    request(`${base}/questions/${id}`, {
      method: "DELETE",
      params: { version },
    }),
  answer: (id: number, choice: KnowledgeChoice, questionVersion: number) =>
    data<KnowledgeAnswer>(`/client/knowledge/questions/${id}/answer`, {
      method: "PUT",
      data: { choice, questionVersion },
    }),
  answers: (id: number, current: number) =>
    table<KnowledgeAnswer>(`${base}/questions/${id}/answers`, {
      params: { current, pageSize: 10 },
    }),
};
