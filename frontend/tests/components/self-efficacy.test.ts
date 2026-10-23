import { beforeEach, describe, expect, it, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
const mocks = vi.hoisted(() => ({ record: vi.fn(), confirm: vi.fn(), push: vi.fn() }));
vi.mock("../../src/features/self-efficacy/api", () => ({ efficacyApi: { record: mocks.record } }));
vi.mock("../../src/shared/ui/confirm", () => ({ confirmAction: mocks.confirm }));
vi.mock("vue-router", async importOriginal => ({ ...await importOriginal<object>(), onBeforeRouteLeave: vi.fn(), useRouter: () => ({ push: mocks.push }) }));
import SelfEfficacyForm from "../../src/features/self-efficacy/components/SelfEfficacyForm.vue";
import SelfEfficacyTrend from "../../src/features/self-efficacy/components/SelfEfficacyTrend.vue";
import en from "../../src/shared/i18n/en";
import type { EfficacyCurrent } from "../../src/features/self-efficacy/types";
const current: EfficacyCurrent = { today: "2026-10-23", timeZone: "Asia/Kuala_Lumpur", intervalDays: 28, acknowledged: true, noticeVersion: "test", available: true, nextAvailableDate: "2026-10-23", latest: null, viewers: [], instrument: { version: "test-instrument", name: "Test", instruction: "Test instructions", questions: ["First statement", "Second statement", "Third statement"], responses: ["Choice 1", "Choice 2", "Choice 3", "Choice 4", "Choice 5"], sourceUrl: "https://example.com", attribution: "Test fixture", license: "test", licenseUrl: "https://example.com" } };
const global = () => ({ plugins: [createI18n({ legacy: false, locale: "en", messages: { en } })], components: { ElButton } });
function render() { return mount(SelfEfficacyForm, { props: { current }, global: global() }); }
function button(wrapper: ReturnType<typeof render>, text: string) { return wrapper.findAll("button").find(b => b.text() === text)!; }
describe("self-efficacy entry", () => {
  beforeEach(() => { vi.clearAllMocks(); mocks.confirm.mockResolvedValue(true); });
  it("does not preselect or advance an unanswered statement", async () => {
    const w = render(); expect(w.findAll("input:checked")).toHaveLength(0);
    expect(button(w, "Next").attributes("disabled")).toBeDefined();
    await w.get("form").trigger("submit"); expect(w.get("legend").text()).toBe("First statement"); expect(mocks.record).not.toHaveBeenCalled(); w.unmount();
  });
  it("keeps ordered answers when moving back, and retains them after an API failure", async () => {
    const w = render(); await w.findAll("input")[0]!.setValue(); await button(w, "Next").trigger("click");
    await w.findAll("input")[1]!.setValue(); await button(w, "Previous").trigger("click");
    expect(w.findAll("input")[0]!.element.checked).toBe(true);
    await button(w, "Next").trigger("click"); expect(w.findAll("input")[1]!.element.checked).toBe(true);
    await button(w, "Next").trigger("click"); await w.findAll("input")[1]!.setValue();
    mocks.record.mockRejectedValue(new Error("Connection lost. Try again."));
    await w.get("form").trigger("submit"); await flushPromises();
    expect(mocks.record).toHaveBeenCalledWith("2026-10-23", "test-instrument", [1, 2, 2]);
    expect(w.get('[role="alert"]').text()).toContain("Connection lost"); expect(w.findAll("input")[1]!.element.checked).toBe(true); expect(w.emitted("saved")).toBeUndefined(); w.unmount();
  });
  it("never creates a score for unrecorded dates", async () => {
    const w = mount(SelfEfficacyTrend, { props: { trend: { startDate: "2026-09-01", endDate: "2026-10-23", timeZone: "Asia/Kuala_Lumpur", points: [{ assessmentDate: "2026-09-25", score: 1.67, instrumentVersion: "test" }, { assessmentDate: "2026-10-23", score: 3, instrumentVersion: "test" }] } }, global: global() });
    expect(w.findAll("circle")).toHaveLength(2);
    await w.findAll("button")[1]!.trigger("click"); expect(w.findAll("tbody tr")).toHaveLength(2); expect(w.text()).toContain("1.67"); expect(w.text()).not.toContain("Low"); w.unmount();
  });
});
