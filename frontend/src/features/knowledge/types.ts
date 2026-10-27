export type KnowledgeChoice = "YES" | "NO" | "NOT_SURE";
export interface KnowledgeQuestion {
  id: number;
  courseId: number;
  courseTitle: string;
  courseStatus: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  questionText: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  version: number;
  answerCount?: number;
  choice?: KnowledgeChoice | null;
  answeredAt?: string | null;
}
export interface QuestionInput {
  questionText: string;
  status: KnowledgeQuestion["status"];
  version: number;
}
export interface KnowledgeAnswer {
  id: number;
  questionId: number;
  studentId: number;
  studentName?: string;
  username?: string;
  choice: KnowledgeChoice;
  updateTime: string;
}
