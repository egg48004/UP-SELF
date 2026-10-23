import { describe, it, expect, vi } from "vitest";
const mocks = vi.hoisted(() => ({ data: vi.fn() }));
vi.mock("../../src/shared/http/client", () => ({ data: mocks.data, request: vi.fn(), table: vi.fn() }));
import { progressApi, learningDate } from "../../src/features/learning/progress";
describe("learning resume contract", () => {
 it("does not show an invalid continue link for reference AjaxResult's empty object", async () => {
  mocks.data.mockResolvedValue({});
  expect(await progressApi.resume()).toBeNull();
 });
 it("preserves the real accessible course and chapter", async () => {
  const entry={ courseId:4, chapterId:8, chapterTitle:"A small step", courseTitle:"Demo" };
  mocks.data.mockResolvedValue(entry);
  expect(await progressApi.resume()).toEqual(entry);
 });
 it("shows completion times in Malaysia even across a UTC date boundary", () => {
  expect(learningDate("2026-10-22T18:00:00Z")).toContain("23 Oct 2026");
  expect(learningDate(null)).toBe("—");
 });
});
