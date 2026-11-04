import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import {
  badgeDate,
  withBadgeNotice,
} from "../../src/features/badges/presentation";
import messages from "../../src/features/badges/messages";
import BadgesPanel from "../../src/features/badges/components/BadgesPanel.vue";
import { badgeApi } from "../../src/features/badges/api";
vi.mock("../../src/features/badges/api", () => ({
  badgeApi: { mine: vi.fn() },
}));
const i18n = () =>
  createI18n({
    legacy: false,
    locale: "en",
    messages: {
      en: { badge: messages, retry: "Try again", loading: "Loading" },
    },
  });
const rows = [
  { code: "FIRST_STEP", awardedAt: "2026-11-03T00:00:00+08:00" },
  { code: "FIRST_SUBMISSION", awardedAt: null },
  { code: "GOAL_REACHED", awardedAt: null },
];
function panel() {
  return mount(BadgesPanel, {
    global: {
      plugins: [i18n()],
      stubs: {
        "el-button": {
          props: ["disabled"],
          emits: ["click"],
          template: `<button :disabled="disabled" @click="$emit('click')"><slot /></button>`,
        },
      },
    },
  });
}
beforeEach(() => {
  vi.mocked(badgeApi.mine).mockReset();
});
describe("badge success feedback", () => {
  it("preserves the normal success text when no new badge was returned", () => {
    const t = i18n().global.t;
    expect(withBadgeNotice("Saved.", null, t)).toBe("Saved.");
    expect(withBadgeNotice("Saved.", undefined, t)).toBe("Saved.");
  });
  it("announces a named milestone without replacing the business result", () => {
    expect(withBadgeNotice("Saved.", "FIRST_STEP", i18n().global.t)).toBe(
      "Saved. Badge earned: First step 🌱",
    );
  });
  it("does not render an unknown server code as a badge", () => {
    expect(withBadgeNotice("Saved.", "<script>", i18n().global.t)).toBe(
      "Saved.",
    );
  });
  it("uses Malaysia calendar dates across a UTC day boundary", () => {
    expect(badgeDate("2026-11-02T18:00:00Z")).toBe("3 Nov 2026");
    expect(badgeDate("invalid")).toBe("");
  });
});
describe("own badge panel", () => {
  it("renders earned and available states with text, dates and instructions", async () => {
    vi.mocked(badgeApi.mine).mockResolvedValue(
      rows as Awaited<ReturnType<typeof badgeApi.mine>>,
    );
    const w = panel();
    await flushPromises();
    expect(w.findAll("li")).toHaveLength(3);
    expect(w.text()).toContain("Earned 3 Nov 2026");
    expect(w.text()).toContain("To try");
    expect(w.text()).toContain("A draft does not count.");
    expect(
      w
        .findAll(".badge-icon")
        .every((n) => n.attributes("aria-hidden") === "true"),
    ).toBe(true);
    w.unmount();
  });
  it("refresh recovers a failed badge request independently", async () => {
    vi.mocked(badgeApi.mine)
      .mockRejectedValueOnce(new Error("Offline"))
      .mockResolvedValue(rows as Awaited<ReturnType<typeof badgeApi.mine>>);
    const w = panel();
    await flushPromises();
    expect(w.text()).toContain("Offline");
    await w.find("button").trigger("click");
    await flushPromises();
    expect(w.findAll("li")).toHaveLength(3);
    expect(w.text()).not.toContain("Offline");
    w.unmount();
  });
});
