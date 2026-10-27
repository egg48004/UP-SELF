import type { KnowledgeChoice } from "./types";
export const knowledgeChoices: {
  value: KnowledgeChoice;
  key: string;
  icon: string;
}[] = [
  { value: "YES", key: "knowledge.yes", icon: "✓" },
  { value: "NO", key: "knowledge.no", icon: "−" },
  { value: "NOT_SURE", key: "knowledge.notSure", icon: "?" },
];
export function knowledgeDate(value: string) {
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kuala_Lumpur",
  }).format(new Date(value));
}
