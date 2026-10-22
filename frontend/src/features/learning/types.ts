export type State = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export interface Course {
  id: number;
  title: string;
  description: string;
  languageCode: string;
  mentorId: number | null;
  mentorName?: string;
  status: State;
}
export interface CourseBody {
  title: string;
  description: string;
  languageCode: string;
}
export interface Chapter {
  id: number;
  courseId: number;
  title: string;
  body: string;
  sortOrder: number;
  status: "DRAFT" | "PUBLISHED";
  languageCode: string;
}
export type ChapterBody = Omit<Chapter, "id" | "courseId">;
export interface Resource {
  id: number;
  chapterId: number;
  kind: "IMAGE" | "VIDEO" | "CAPTION";
  title: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  altText: string | null;
  captionResourceId: number | null;
  transcript: string | null;
  languageCode: string;
  status: "DRAFT" | "PUBLISHED";
}
export interface ResourceBody {
  title: string;
  altText: string;
  captionResourceId: number | null;
  transcript: string;
  languageCode: string;
  status: "DRAFT" | "PUBLISHED";
}
export interface Post {
  id: number;
  courseId: number;
  title: string;
  body: string;
  type: "ANNOUNCEMENT" | "MEETING";
  meetingUrl: string | null;
  meetingStartsAt: string | null;
  languageCode: string;
  status: "DRAFT" | "PUBLISHED";
  createdBy: number;
}
export type PostBody = Omit<Post, "id" | "courseId" | "createdBy">;
export interface Member {
  studentId: number;
  userName: string;
  nickName: string;
}
