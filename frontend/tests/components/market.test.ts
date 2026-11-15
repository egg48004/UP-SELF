import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
const api = vi.hoisted(() => ({ create: vi.fn(), update: vi.fn() }));
vi.mock("../../src/features/market/api", () => ({ marketApi: api }));
import MarketTaskEditor from "../../src/features/market/components/MarketTaskEditor.vue";
import en from "../../src/shared/i18n/en";
function render(task?: object) {
  return mount(MarketTaskEditor, {
    props: { modelValue: true, courseId: 1, task },
    global: {
      plugins: [createI18n({ legacy: false, locale: "en", messages: { en } })],
      components: { ElButton },
      stubs: { ElDialog: { template: "<div><slot /></div>" } },
    },
  });
}
const task = {
  id: 2,
  courseId: 1,
  title: "Delivery",
  objective: "Consider timing",
  situation: "Your delivery is late.",
  question: "What next?",
  reflectionPrompt: "What would you change?",
  version: 4,
  options: [
    {
      label: "Contact customer",
      outcome: "They can decide",
      explanation: "Time to respond",
    },
    {
      label: "Wait",
      outcome: "They may miss delivery",
      explanation: "Less time",
    },
  ],
};
describe("market task authoring", () => {
  beforeEach(() => vi.clearAllMocks());
  it("blocks incomplete content without sending it", async () => {
    const w = render();
    await w.get("form").trigger("submit");
    expect(api.create).not.toHaveBeenCalled();
    expect(w.get('[role="alert"]').text()).toBe(
      "Complete the required fields.",
    );
    w.unmount();
  });
  it("retains edits after a participation lock rejection", async () => {
    api.update.mockRejectedValue(
      new Error(
        "Students have participated. Copy this task to change its content.",
      ),
    );
    const w = render(task);
    await w.get("#market-title").setValue("My unsaved change");
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(w.get('[role="alert"]').text()).toContain("Copy this task");
    expect((w.get("#market-title").element as HTMLInputElement).value).toBe(
      "My unsaved change",
    );
    expect(w.emitted("saved")).toBeUndefined();
    w.unmount();
  });
  it("sends the expected version and emits a saved ID", async () => {
    api.update.mockResolvedValue({ code: 200 });
    const w = render(task);
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(api.update).toHaveBeenCalledWith(
      2,
      expect.objectContaining({ version: 4, courseId: 1 }),
    );
    expect(w.emitted("saved")).toEqual([[2]]);
    w.unmount();
  });
});
