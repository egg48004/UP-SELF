import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
const mocks = vi.hoisted(() => ({ create: vi.fn(), edit: vi.fn() }));
vi.mock("../../src/features/goals/api", () => ({ goalApi: mocks }));
import GoalEditor from "../../src/features/goals/components/GoalEditor.vue";
import en from "../../src/shared/i18n/en";

function render(props = {}) {
  return mount(GoalEditor, {
    props,
    global: {
      plugins: [createI18n({ legacy: false, locale: "en", messages: { en } })],
      components: { ElButton },
    },
  });
}
describe("personal goal editor", () => {
  beforeEach(() => vi.clearAllMocks());
  it("keeps unsaved input when a concurrent edit is rejected", async () => {
    mocks.edit.mockRejectedValue(new Error("This goal has changed. Refresh your goals and try again."));
    const w = render({ goal: { id: 7, title: "Old", details: "", version: 3 } });
    await w.get("#goal-title").setValue("My unsaved change");
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(mocks.edit).toHaveBeenCalledWith(7, { title: "My unsaved change", details: "" }, 3);
    expect(w.get('[role="alert"]').text()).toContain("Refresh your goals");
    expect((w.get("#goal-title").element as HTMLInputElement).value).toBe("My unsaved change");
    expect(w.emitted("saved")).toBeUndefined();
  });
  it("rejects whitespace without sending a request", async () => {
    const w = render();
    await w.get("#goal-title").setValue("   ");
    await w.get("form").trigger("submit");
    expect(mocks.create).not.toHaveBeenCalled();
    expect(w.get('[role="alert"]').text()).toContain("Please enter a goal");
  });
});
