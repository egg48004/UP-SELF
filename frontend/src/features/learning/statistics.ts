import { data, table } from "../../shared/http/client";
export interface StatisticsTotals {
  courseCount: number;
  totalChapters: number;
  completedChapters: number;
  percent: number;
  assignmentCount: number;
  notSubmittedCount: number;
  submittedCount: number;
  gradedCount: number;
  returnedCount: number;
}
export interface StatisticsCourse extends Omit<
  StatisticsTotals,
  "courseCount"
> {
  courseId: number;
  courseTitle: string;
}
export const statisticsApi = {
  totals: () => data<StatisticsTotals>("/client/learning/statistics"),
  courses: (current: number) =>
    table<StatisticsCourse>("/client/learning/statistics/courses", {
      params: { current, pageSize: 10 },
    }),
};
