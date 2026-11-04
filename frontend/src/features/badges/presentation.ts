import { badgeCodes } from "./types";
import type { BadgeCode } from "./types";
export const badgeIcons: Record<BadgeCode, string> = {
  FIRST_STEP: "🌱",
  FIRST_SUBMISSION: "📝",
  GOAL_REACHED: "🎯",
};
export function withBadgeNotice(
  normal: string,
  code: unknown,
  t: (key: string, values?: Record<string, string>) => string,
): string {
  if (typeof code !== "string" || !badgeCodes.includes(code as BadgeCode))
    return normal;
  return (
    normal +
    " " +
    t("badge.earnedNotice", {
      name: t("badge.names." + code),
      icon: badgeIcons[code as BadgeCode],
    })
  );
}
export function badgeDate(value: string): string {
  const date = new Date(value);
  return Number.isFinite(date.getTime())
    ? new Intl.DateTimeFormat("en-GB", {
        dateStyle: "medium",
        timeZone: "Asia/Kuala_Lumpur",
      }).format(date)
    : "";
}
