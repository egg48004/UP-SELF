import { describe, it, expect } from "vitest";
import {
  consecutiveRuns,
  weekDate,
  weekEnd,
} from "../../src/features/mood/presentation";
describe("weekly mood presentation", () => {
  it("does not connect or invent missing weeks", () => {
    expect(
      consecutiveRuns([
        { weekStart: "2026-09-28", moodLevel: 2 },
        { weekStart: "2026-10-05", moodLevel: 3 },
        { weekStart: "2026-10-12", moodLevel: null },
        { weekStart: "2026-10-19", moodLevel: 4 },
      ]),
    ).toEqual([
      [
        { index: 0, level: 2 },
        { index: 1, level: 3 },
      ],
      [{ index: 3, level: 4 }],
    ]);
  });
  it("has no artificial point when no records exist", () =>
    expect(
      consecutiveRuns([{ weekStart: "2026-10-19", moodLevel: null }]),
    ).toEqual([]));
  it("uses one week across a year boundary", () => {
    expect(weekEnd("2024-12-30")).toBe("2025-01-05");
    expect(weekDate("2024-12-30")).toContain("2024");
  });
});
