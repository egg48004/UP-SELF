import { data, request, table } from "../../shared/http/client";
export interface Reminder {
  id: number;
  studentId: number;
  studentName: string;
  username: string;
  kind: string;
  courseId?: number;
  courseTitle?: string;
  assignmentId?: number;
  reason: string;
  eventDate: string;
  active: boolean;
  reviewed: boolean;
  version: number;
}
export const followUpApi = {
  list: (current: number, status: string, studentId?: number) =>
    table<Reminder>("/follow-ups", {
      params: { current, pageSize: 10, status, studentId },
    }),
  summary: () =>
    data<{ total: number; byStudent: Record<string, number> }>(
      "/follow-ups/summary",
    ),
  review: (r: Reminder) =>
    request("/follow-ups/" + r.id + "/review", {
      method: "PUT",
      data: { version: r.version },
    }),
};
