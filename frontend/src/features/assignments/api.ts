import { data, request, table, download } from "../../shared/http/client";
import { session } from "../../shared/auth/session";
import type {
  Assignment,
  AssignmentBody,
  Submission,
  ReviewBody,
} from "./types";
const base = () =>
  session.channel === "student" ? "/client/assignments" : "/assignments";
export const assignmentApi = {
  list: (
    current = 1,
    keyword = "",
    courseId?: number,
    pending = false,
    pageSize = 10,
  ) =>
    table<Assignment>(base(), {
      params: { current, pageSize, keyword, courseId, pending },
    }),
  detail: (id: number) => data<Assignment>(base() + "/" + id),
  create: (body: AssignmentBody) =>
    data<number>(base(), { method: "POST", data: body }),
  update: (id: number, body: AssignmentBody) =>
    request(base() + "/" + id, { method: "PUT", data: body }),
  mine: (id: number) => data<Submission>(base() + "/" + id + "/submission"),
  draft: (id: number, version: number, answerText: string) =>
    data<Submission>(base() + "/" + id + "/submission", {
      method: "PUT",
      data: { version, answerText },
    }),
  submit: (
    id: number,
    version: number,
    answerText: string,
    assignmentVersion: number,
    requestKey: string,
  ) =>
    data<Submission>(base() + "/" + id + "/submit", {
      method: "POST",
      data: { version, answerText, assignmentVersion, requestKey },
    }),
  upload: (
    id: number,
    version: number,
    file: File,
    signal: AbortSignal,
    onProgress: (n: number) => void,
  ) => {
    const form = new FormData();
    form.append("version", String(version));
    form.append("file", file);
    return data<Submission>(base() + "/" + id + "/files", {
      method: "POST",
      data: form,
      signal,
      timeout: 180000,
      onUploadProgress: (e) =>
        onProgress(e.total ? Math.round((e.loaded * 100) / e.total) : 0),
    });
  },
  removeFile: (id: number, fileId: number, version: number) =>
    data<Submission>(base() + "/" + id + "/files/" + fileId, {
      method: "DELETE",
      params: { version },
    }),
  submissions: (id: number, current = 1) =>
    table<Submission>(base() + "/" + id + "/submissions", {
      params: { current, pageSize: 10 },
    }),
  reviewDetail: (id: number, sid: number) =>
    data<Submission>(base() + "/" + id + "/submissions/" + sid),
  review: (id: number, sid: number, body: ReviewBody) =>
    data<Submission>(base() + "/" + id + "/submissions/" + sid + "/review", {
      method: "POST",
      data: body,
    }),
  download: (id: number) => download(base() + "/files/" + id + "/content"),
};
export function malaysiaDate(value: string | null) {
  return value
    ? new Intl.DateTimeFormat("en-GB", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Kuala_Lumpur",
      }).format(new Date(value))
    : "";
}
