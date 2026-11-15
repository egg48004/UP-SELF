export interface MarketOption {
  id?: number;
  label: string;
  outcome?: string | null;
  explanation?: string | null;
}
export interface MarketTask {
  id: number;
  courseId: number;
  courseTitle: string;
  courseStatus: string;
  title: string;
  objective: string;
  situation: string;
  question: string;
  reflectionPrompt: string;
  status: "DRAFT" | "PUBLISHED" | "CLOSED";
  version: number;
  locked: boolean;
  attemptCount: number;
  options: MarketOption[];
}
export interface TaskContent {
  courseId: number;
  title: string;
  objective: string;
  situation: string;
  question: string;
  reflectionPrompt: string;
  version: number;
  options: MarketOption[];
}
export interface MarketAttempt {
  id: number;
  taskId: number;
  optionId: number;
  label: string;
  outcome: string;
  explanation: string;
  reflection: string;
  version: number;
  firstAttempt: boolean;
  createTime: string;
}
export interface Participant {
  studentId: number;
  username: string;
  studentName: string;
  enrolled: number;
  attemptCount: number;
  firstChoice: string | null;
  lastAttemptAt: string | null;
}
