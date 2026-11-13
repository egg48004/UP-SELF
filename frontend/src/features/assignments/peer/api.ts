import { data, request, table } from "../../../shared/http/client";
import { session } from "../../../shared/auth/session";
import type { WorkFile } from "../types";
export interface PeerFeedback {
  id: number;
  attemptNo: number;
  version: number;
  strength: string | null;
  suggestion: string | null;
  hidden: boolean;
  hideReason: string | null;
  sentAt: string | null;
  earlierSubmission: boolean;
  answerText: string | null;
  files: WorkFile[] | null;
  reviewerName: string | null;
  studentName: string | null;
}
export interface PeerState {
  enabled: boolean;
  outgoing: PeerFeedback | null;
  received: PeerFeedback[];
}
const base = (id: number) =>
  (session.channel === "student" ? "/client" : "") +
  `/assignments/${id}/peer-feedback`;
export const peerApi = {
  state: (id: number) => data<PeerState>(base(id)),
  allocate: (id: number) =>
    data<PeerState>(base(id) + "/allocate", { method: "POST" }),
  send: (id: number, version: number, strength: string, suggestion: string) =>
    request(base(id), {
      method: "POST",
      data: { version, strength, suggestion },
    }),
  manage: (id: number, current: number) =>
    table<PeerFeedback>(base(id) + "/manage", {
      params: { current, pageSize: 10 },
    }),
  hide: (id: number, fid: number, version: number, reason: string) =>
    request(base(id) + `/${fid}/hide`, {
      method: "PUT",
      data: { version, reason },
    }),
};
