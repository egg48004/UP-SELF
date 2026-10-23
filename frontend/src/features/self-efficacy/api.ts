import { data, table } from "../../shared/http/client";
import type { EfficacyCurrent, EfficacyEntry, EfficacyPoint, EfficacyPerson, EfficacyTrend } from "./types";
export const efficacyApi = {
  current: () => data<EfficacyCurrent>("/client/self-efficacy/current"),
  acknowledge: (noticeVersion: string) => data<EfficacyCurrent>("/client/self-efficacy/acknowledgment", { method: "POST", data: { noticeVersion, accepted: true } }),
  record: (assessmentDate: string, instrumentVersion: string, answers: number[]) => data<EfficacyCurrent>("/client/self-efficacy/entries", { method: "POST", data: { assessmentDate, instrumentVersion, answers } }),
  history: (current: number, studentId?: number) => studentId === undefined
    ? table<EfficacyEntry>("/client/self-efficacy/history", { params: { current, pageSize: 10 } })
    : table<EfficacyPoint>("/self-efficacy/students/" + studentId + "/history", { params: { current, pageSize: 10 } }),
  trend: (weeks: number, studentId?: number) => data<EfficacyTrend>(studentId === undefined ? "/client/self-efficacy/trend" : "/self-efficacy/students/" + studentId + "/trend", { params: { weeks } }),
  student: (id: number) => data<EfficacyPerson>("/self-efficacy/students/" + id),
};
