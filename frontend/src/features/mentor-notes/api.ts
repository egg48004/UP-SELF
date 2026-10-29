import { data, request, table } from "../../shared/http/client";
import type { MentorNote, NoteStudent } from "./types";
const base = (student: number) => `/mentorship/students/${student}/notes`;
export const noteApi = {
  student: (student: number) => data<NoteStudent>(base(student) + "/student"),
  list: (student: number, current: number) =>
    table<MentorNote>(base(student), { params: { current, pageSize: 10 } }),
  create: (student: number, content: string) =>
    data<number>(base(student), { method: "POST", data: { content } }),
  edit: (student: number, note: MentorNote, content: string) =>
    request(base(student) + "/" + note.id, {
      method: "PUT",
      data: { content, version: note.version },
    }),
};
