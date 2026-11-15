import { data, request, table } from "../../shared/http/client";
import { session } from "../../shared/auth/session";
import type {
  MarketTask,
  MarketAttempt,
  TaskContent,
  Participant,
} from "./types";
const base = () =>
  session.channel === "student" ? "/client/market-tasks" : "/market-tasks";
export const marketApi = {
  list: (courseId: number | undefined, current = 1) =>
    table<MarketTask>(base(), { params: { courseId, current, pageSize: 10 } }),
  detail: (id: number) => data<MarketTask>(base() + "/" + id),
  create: (body: TaskContent) =>
    data<number>(base(), { method: "POST", data: body }),
  update: (id: number, body: TaskContent) =>
    request(base() + "/" + id, { method: "PUT", data: body }),
  status: (id: number, status: string, version: number) =>
    request(base() + `/${id}/status`, {
      method: "PUT",
      data: { status, version },
    }),
  copy: (id: number) =>
    data<number>(base() + `/${id}/copy`, { method: "POST" }),
  choose: (id: number, optionId: number, version: number, requestKey: string) =>
    data<MarketAttempt>(base() + `/${id}/attempts`, {
      method: "POST",
      data: { optionId, version, requestKey },
    }),
  history: (id: number, current = 1, studentId?: number) =>
    table<MarketAttempt>(base() + `/${id}/attempts`, {
      params: { current, pageSize: 10, studentId },
    }),
  reflect: (
    id: number,
    attemptId: number,
    version: number,
    reflection: string,
  ) =>
    request(base() + `/${id}/attempts/${attemptId}/reflection`, {
      method: "PUT",
      data: { version, reflection },
    }),
  participants: (id: number, current = 1) =>
    table<Participant>(base() + `/${id}/participants`, {
      params: { current, pageSize: 10 },
    }),
};
