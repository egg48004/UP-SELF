import { request, table } from "../../shared/http/client";
import type { Course } from "./types";

export interface SavedCourse {
  courseId: number;
  savedAt: string;
  available: boolean;
  course: Course | null;
}
const base = "/client/learning/bookmarks";
export const bookmarkApi = {
  list: (current: number) => table<SavedCourse>(base, { params: { current, pageSize: 10 } }),
  save: (courseId: number) => request(`${base}/${courseId}`, { method: "PUT" }),
  remove: (courseId: number) => request(`${base}/${courseId}`, { method: "DELETE" }),
};
