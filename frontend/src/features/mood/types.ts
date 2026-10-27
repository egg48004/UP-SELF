export interface MoodPerson {
  id: number;
  displayName: string;
  username: string;
}
export interface MoodPoint {
  weekStart: string;
  moodLevel: number | null;
}
export interface DiaryRecording {
  recordingId?: number | null;
  recordingName?: string | null;
  recordingMime?: string | null;
  recordingSize?: number | null;
}
export interface MoodEntry extends MoodPoint, DiaryRecording {
  id: number;
  studentId: number;
  privateNote: string;
  noteShared: boolean;
  createTime: string;
}
export interface MentorMoodEntry extends MoodPoint, DiaryRecording {
  diaryNote: string | null;
}
export interface MoodCurrent {
  weekStart: string;
  weekEnd: string;
  nextWeekStart: string;
  timeZone: string;
  noticeVersion: string;
  acknowledged: boolean;
  entry: MoodEntry | null;
  viewers: MoodPerson[];
}
export interface MoodTrend {
  startWeek: string;
  endWeek: string;
  timeZone: string;
  weeks: number;
  points: MoodPoint[];
}
