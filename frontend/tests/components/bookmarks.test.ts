import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
const mocks = vi.hoisted(() => ({ save: vi.fn(), remove: vi.fn() }));
vi.mock("../../src/features/learning/bookmarks", () => ({ bookmarkApi: mocks }));
vi.mock("../../src/features/auth/store", () => ({ useAuthStore: () => ({ isStudent: true, can: () => true }) }));
import BookmarkButton from "../../src/features/learning/components/BookmarkButton.vue";
import en from "../../src/shared/i18n/en";

function render(saved = false) {
  return mount(BookmarkButton, {
    props: { courseId: 7, title: "One small step", saved },
    global: { plugins: [createI18n({ legacy: false, locale: "en", messages: { en } })], components: { ElButton } },
  });
}
describe("course bookmark feedback", () => {
  beforeEach(() => vi.clearAllMocks());
  it("keeps saved state unchanged when removal fails", async () => {
    mocks.remove.mockRejectedValue(new Error("Connection lost. Please try again."));
    const w = render(true); await w.get("button").trigger("click"); await flushPromises();
    expect(w.emitted("changed")).toBeUndefined();
    expect(w.get("button").attributes("aria-pressed")).toBe("true");
    expect(w.get('[role="alert"]').text()).toContain("Connection lost");
  });
  it("prevents repeated clicks while a save is pending", async () => {
    let finish!: () => void;
    mocks.save.mockReturnValue(new Promise<void>(resolve => { finish = resolve; }));
    const w = render();
    await w.get("button").trigger("click"); await w.get("button").trigger("click");
    expect(mocks.save).toHaveBeenCalledTimes(1);
    expect(w.emitted("changed")).toBeUndefined();
    finish(); await flushPromises();
    expect(w.emitted("changed")).toEqual([[true]]);
  });
});
