import type { MoodPoint } from "./types";
export const moodChoices = [
  { level: 1, emoji: "😔" },
  { level: 2, emoji: "🙁" },
  { level: 3, emoji: "😐" },
  { level: 4, emoji: "🙂" },
  { level: 5, emoji: "😊" },
];
export function weekDate(value: string, locale = "en-GB") {
  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(value + "T00:00:00Z"));
}
export function weekEnd(value: string) {
  return new Date(new Date(value + "T00:00:00Z").getTime() + 6 * 86400000)
    .toISOString()
    .slice(0, 10);
}
/** Missing weeks deliberately break a line; no inferred or neutral values. */
export function consecutiveRuns(points: MoodPoint[]) {
  const runs: { index: number; level: number }[][] = [];
  let run: { index: number; level: number }[] = [];
  points.forEach((point, index) => {
    if (point.moodLevel === null) {
      if (run.length) runs.push(run);
      run = [];
    } else run.push({ index, level: point.moodLevel });
  });
  if (run.length) runs.push(run);
  return runs;
}
