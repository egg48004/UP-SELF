import type { Envelope } from "../../shared/http/contracts";
export const badgeCodes = [
  "FIRST_STEP",
  "FIRST_SUBMISSION",
  "GOAL_REACHED",
] as const;
export type BadgeCode = (typeof badgeCodes)[number];
export interface Badge {
  code: BadgeCode;
  awardedAt: string | null;
}
export interface BadgeActionResult extends Envelope {
  earnedBadgeCode?: BadgeCode | null;
}
