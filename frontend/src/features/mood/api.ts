import { data, table, download } from "../../shared/http/client";
import type {
  MoodCurrent,
  MoodEntry,
  MoodPerson,
  MoodTrend,
  MentorMoodEntry,
} from "./types";
export const moodApi = {
  audio: (id: number, mentor = false) => download((mentor ? "/moods" : "/client/moods") + "/recordings/" + id + "/content"),
  recordWithAudio: (weekStart: string, moodLevel: number, privateNote: string, file: File) => {
    const form = new FormData(); form.append("weekStart", weekStart); form.append("moodLevel", String(moodLevel)); form.append("privateNote", privateNote); form.append("file", file);
    return data<MoodCurrent>("/client/moods/entries/recording", { method: "POST", data: form, timeout: 180000 });
  },
  current: () => data<MoodCurrent>("/client/moods/current"),
  acknowledge: (noticeVersion: string, accepted: boolean) =>
    data<MoodCurrent>("/client/moods/acknowledgment", {
      method: "POST",
      data: { noticeVersion, accepted },
    }),
  record: (weekStart: string, moodLevel: number, privateNote: string) =>
    data<MoodCurrent>("/client/moods/entries", {
      method: "POST",
      data: { weekStart, moodLevel, privateNote },
    }),
  history: (current: number, studentId?: number) =>
    studentId === undefined
      ? table<MoodEntry>("/client/moods/history", {
          params: { current, pageSize: 10 },
        })
      : table<MentorMoodEntry>("/moods/students/" + studentId + "/history", {
          params: { current, pageSize: 10 },
        }),
  trend: (weeks: number, studentId?: number) =>
    data<MoodTrend>(
      studentId === undefined
        ? "/client/moods/trend"
        : "/moods/students/" + studentId + "/trend",
      { params: { weeks } },
    ),
  students: (current: number, keyword: string) =>
    table<MoodPerson>("/moods/students", {
      params: { current, pageSize: 10, keyword },
    }),
  student: (id: number) => data<MoodPerson>("/moods/students/" + id),
};
