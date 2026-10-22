import { data, request, table, mediaUrl } from "../../shared/http/client";
import { session } from "../../shared/auth/session";
import type { PageQuery } from "../../shared/http/contracts";
import type {
  Course,
  CourseBody,
  Chapter,
  ChapterBody,
  Resource,
  ResourceBody,
  Post,
  PostBody,
  Member,
  State,
} from "./types";
const base = () =>
  session.channel === "student" ? "/client/learning" : "/learning";
const put = (url: string, body: unknown) =>
  request(url, { method: "PUT", data: body });
export const learningApi = {
  courses: (q: PageQuery) => table<Course>(base() + "/courses", { params: q }),
  course: (id: number) => data<Course>(base() + "/courses/" + id),
  createCourse: (b: CourseBody) =>
    data<number>(base() + "/courses", { method: "POST", data: b }),
  updateCourse: (id: number, b: CourseBody) =>
    put(base() + "/courses/" + id, b),
  assign: (id: number, mentorId: number) =>
    put(base() + "/courses/" + id + "/mentor", { mentorId }),
  state: (id: number, status: State) =>
    put(base() + "/courses/" + id + "/status", { status }),
  members: (id: number) =>
    data<Member[]>(base() + "/courses/" + id + "/students"),
  enroll: (id: number, studentId: number) =>
    request(base() + "/courses/" + id + "/students", {
      method: "POST",
      data: { studentId },
    }),
  unenroll: (id: number, sid: number) =>
    request(base() + "/courses/" + id + "/students/" + sid, {
      method: "DELETE",
    }),
  chapters: (id: number) =>
    data<Chapter[]>(base() + "/courses/" + id + "/chapters"),
  chapter: (id: number) => data<Chapter>(base() + "/chapters/" + id),
  createChapter: (id: number, b: ChapterBody) =>
    data<number>(base() + "/courses/" + id + "/chapters", {
      method: "POST",
      data: b,
    }),
  updateChapter: (id: number, b: ChapterBody) =>
    put(base() + "/chapters/" + id, b),
  deleteChapter: (id: number) =>
    request(base() + "/chapters/" + id, { method: "DELETE" }),
  resources: (id: number) =>
    data<Resource[]>(base() + "/chapters/" + id + "/resources"),
  upload: (
    id: number,
    form: FormData,
    progress: (n: number) => void,
    signal: AbortSignal,
  ) =>
    data<number>(base() + "/chapters/" + id + "/resources", {
      method: "POST",
      data: form,
      timeout: 180000,
      signal,
      onUploadProgress: (e) =>
        progress(e.total ? Math.round((e.loaded * 100) / e.total) : 0),
    }),
  updateResource: (id: number, b: ResourceBody) =>
    put(base() + "/resources/" + id, b),
  deleteResource: (id: number) =>
    request(base() + "/resources/" + id, { method: "DELETE" }),
  access: async (id: number) =>
    mediaUrl(
      (
        await data<{ url: string; expiresIn: number }>(
          base() + "/resources/" + id + "/access",
          { method: "POST" },
        )
      ).url,
    ),
  posts: (id: number) => data<Post[]>(base() + "/courses/" + id + "/posts"),
  createPost: (id: number, b: PostBody) =>
    data<number>(base() + "/courses/" + id + "/posts", {
      method: "POST",
      data: b,
    }),
  updatePost: (id: number, b: PostBody) => put(base() + "/posts/" + id, b),
  offline: (id: number) => put(base() + "/posts/" + id + "/offline", {}),
  deletePost: (id: number) =>
    request(base() + "/posts/" + id, { method: "DELETE" }),
};
