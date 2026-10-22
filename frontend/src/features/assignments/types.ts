export interface Assignment {
  id: number;
  courseId: number;
  courseTitle: string;
  courseStatus: string;
  title: string;
  instructions: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  dueAt: string | null;
  maxScore: number;
  version: number;
  languageCode: string;
  myStatus: string | null;
  pendingCount: number | null;
}
export type AssignmentBody = Pick<
  Assignment,
  | "courseId"
  | "title"
  | "instructions"
  | "status"
  | "dueAt"
  | "maxScore"
  | "version"
  | "languageCode"
>;
export interface WorkFile {
  id: number;
  originalName: string;
  fileSize: number;
  mimeType: string;
  attemptNo: number | null;
}
export interface Attempt {
  id: number;
  attemptNo: number;
  answerText: string;
  status: string;
  late: boolean;
  score: number | null;
  maxScore: number;
  feedback: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  files: WorkFile[];
}
export interface Submission {
  id: number | null;
  assignmentId: number;
  studentId: number;
  studentName?: string;
  studentUsername?: string;
  answerText: string;
  status: string;
  version: number;
  attemptNo: number;
  files: WorkFile[];
  attempts: Attempt[];
}
export interface ReviewBody {
  version: number;
  decision: "GRADED" | "RETURNED";
  score: number | null;
  feedback: string;
}
