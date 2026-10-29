import { data, request, table } from "../../shared/http/client";
import { session } from "../../shared/auth/session";
export interface Report {
  id: number;
  accusedName: string;
  reporterName?: string;
  reason: string;
  status: string;
  createTime: string;
  reviewedAt?: string;
  version: number;
  courseTitle?: string;
  groupTitle?: string;
  messageBody?: string;
  attachmentSummary?: string;
  details?: string;
  resolution?: string;
}
const own = () =>
  session.channel === "student" ? "/client/reports" : "/reports";
export const reportApi = {
  create: (messageId: number, reason: string, details: string) =>
    data<number>(own(), {
      method: "POST",
      data: { messageId, reason, details },
    }),
  mine: (current: number) =>
    table<Report>(own() + "/mine", { params: { current, pageSize: 10 } }),
  list: (current: number, status: string) =>
    table<Report>("/moderation/reports", {
      params: { current, pageSize: 10, status },
    }),
  detail: (id: number) => data<Report>("/moderation/reports/" + id),
  review: (id: number, status: string, resolution: string, version: number) =>
    request("/moderation/reports/" + id, {
      method: "PUT",
      data: { status, resolution, version },
    }),
};
