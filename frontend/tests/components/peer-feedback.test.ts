import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { createRouter, createMemoryHistory } from "vue-router";
import { ElButton } from "element-plus";
const api = vi.hoisted(() => ({
  state: vi.fn(),
  allocate: vi.fn(),
  send: vi.fn(),
}));
vi.mock("../../src/features/assignments/peer/api", () => ({ peerApi: api }));
import StudentPeerFeedback from "../../src/features/assignments/peer/StudentPeerFeedback.vue";
import en from "../../src/shared/i18n/en";
const outgoing = {
  id: 1,
  version: 0,
  files: [],
  answerText: "A business idea",
  sentAt: null,
  hidden: false,
};
async function render(state: object, submitted = true) {
  api.state.mockResolvedValue(state);
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/", component: { template: "<div />" } }],
  });
  await router.push("/");
  const w = mount(StudentPeerFeedback, {
    props: { assignmentId: 7, submitted },
    global: {
      plugins: [
        router,
        createI18n({ legacy: false, locale: "en", messages: { en } }),
      ],
      components: { ElButton },
      stubs: { WorkFiles: true },
    },
  });
  await flushPromises();
  return w;
}
describe("peer feedback", () => {
  beforeEach(() => vi.clearAllMocks());
  it("disables allocation before own submission", async () => {
    const w = await render(
      { enabled: true, outgoing: null, received: [] },
      false,
    );
    expect(
      w
        .findAll("button")
        .find((x) => x.text() === "Review a classmate")!
        .attributes("disabled"),
    ).toBeDefined();
    w.unmount();
  });
  it("blocks whitespace and retains input after filter rejection", async () => {
    const w = await render({ enabled: true, outgoing, received: [] });
    await w.get("#peer-strength").setValue("  ");
    await w.get("form").trigger("submit");
    expect(api.send).not.toHaveBeenCalled();
    api.send.mockRejectedValue(new Error("Change hurtful words."));
    await w.get("#peer-strength").setValue("My feedback");
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(w.get('[role="alert"]').text()).toBe("Change hurtful words.");
    expect((w.get("#peer-strength").element as HTMLTextAreaElement).value).toBe(
      "My feedback",
    );
    w.unmount();
  });
  it("keeps received feedback readable when closed", async () => {
    const w = await render({
      enabled: false,
      outgoing,
      received: [{ id: 2, strength: "Clear idea", suggestion: "" }],
    });
    expect(w.text()).toContain("Clear idea");
    expect(w.find("form").exists()).toBe(false);
    w.unmount();
  });
  it("does not show hidden feedback text", async () => {
    const w = await render({
      enabled: true,
      outgoing: {
        ...outgoing,
        hidden: true,
        sentAt: "2026-11-13",
        strength: null,
      },
      received: [],
    });
    expect(w.text()).toContain("Feedback hidden by your mentor.");
    expect(w.find("form").exists()).toBe(false);
    w.unmount();
  });
});
