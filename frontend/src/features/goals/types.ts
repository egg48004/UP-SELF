export interface PersonalGoal {
  id: number;
  title: string;
  details: string;
  status: "ACTIVE" | "COMPLETED";
  completedAt: string | null;
  createTime: string;
  updateTime: string;
  version: number;
}
export interface GoalInput {
  title: string;
  details: string;
}
export type GoalFilter = "" | "ACTIVE" | "COMPLETED";
