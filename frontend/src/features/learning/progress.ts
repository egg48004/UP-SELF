import { data, request, table } from "../../shared/http/client";
export interface LearningItem {
  chapterId: number;
  courseId: number;
  chapterTitle: string;
  courseTitle: string;
  completedAt: string | null;
  lastVisitedAt: string | null;
}
export interface LearningProgress {
  total: number;
  completed: number;
  percent: number;
  lastChapterId: number | null;
  chapters: LearningItem[];
}
export interface StudentOverview {
  studentId: number;
  studentName: string;
  username: string;
  courseId: number;
  courseTitle: string;
  totalChapters: number;
  completedChapters: number;
  assignmentCount: number;
  submittedCount: number;
  gradedCount: number;
  returnedCount: number;
  moodLevel: number | null;
  moodWeek: string | null;
}
export const progressApi = {
  summary: (id: number) =>
    data<LearningProgress>(`/client/learning/courses/${id}/progress`),
  visit: (id: number) =>
    request(`/client/learning/chapters/${id}/visit`, { method: "POST" }),
  complete: (id: number, completed: boolean) =>
    request(`/client/learning/chapters/${id}/completion`, {
      method: "PUT",
      data: { completed },
    }),
  resume: async () => {
    // Reference AjaxResult encodes an absent object as {}, not null.
    const item = await data<Partial<LearningItem>>("/client/learning/resume");
    return item.courseId ? (item as LearningItem) : null;
  },
  history: (current: number) =>
    table<LearningItem>("/client/learning/history", {
      params: { current, pageSize: 10 },
    }),
  overview: (current: number, keyword: string, courseId?: number) =>
    table<StudentOverview>("/learning/students/overview", {
      params: { current, pageSize: 10, keyword, courseId },
    }),
};
export function learningDate(value: string | null) {
  return value
    ? new Intl.DateTimeFormat("en-GB", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Kuala_Lumpur",
      }).format(new Date(value))
    : "—";
}
