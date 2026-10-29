import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";
import { createPinia, setActivePinia, disposePinia } from "pinia";
import type { Pinia } from "pinia";
import { nextTick } from "vue";
const mocks = vi.hoisted(() => ({ read: vi.fn() }));
vi.mock("../../src/features/preferences/api", () => ({
  accessibilityApi: { read: mocks.read },
}));
import { usePreferences } from "../../src/features/preferences/store";
let pinia: Pinia;
let media: {
  matches: boolean;
  addEventListener: ReturnType<typeof vi.fn>;
  removeEventListener: ReturnType<typeof vi.fn>;
};
beforeEach(() => {
  localStorage.clear();
  pinia = createPinia();
  setActivePinia(pinia);
  media = {
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  };
  vi.stubGlobal("matchMedia", () => media);
  mocks.read
    .mockReset()
    .mockResolvedValue({
      size: "large",
      contrast: "highContrast",
      motion: true,
      version: 1,
    });
});
afterEach(() => {
  disposePinia(pinia);
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
describe("platform defaults and personal choices", () => {
  it("applies downloaded defaults without saving them as personal overrides", async () => {
    const p = usePreferences();
    await p.loadDefaults();
    await nextTick();
    expect(p.size).toBe("large");
    expect(p.contrast).toBe("highContrast");
    expect(document.documentElement.dataset.motion).toBe("true");
    expect(p.hasOverrides).toBe(false);
    expect(localStorage.getItem("upself.preferences.v2")).toBeNull();
  });
  it("preserves only personally selected fields when defaults change", async () => {
    const p = usePreferences();
    p.size = "extraLarge";
    await p.loadDefaults();
    expect(p.size).toBe("extraLarge");
    expect(p.contrast).toBe("highContrast");
    expect(JSON.parse(localStorage.getItem("upself.preferences.v2")!)).toEqual({
      size: "extraLarge",
    });
  });
  it("clears personal choices on reset and follows later platform changes", async () => {
    const p = usePreferences();
    p.size = "extraLarge";
    await p.loadDefaults();
    p.reset();
    expect(p.size).toBe("large");
    expect(p.hasOverrides).toBe(false);
    mocks.read.mockResolvedValue({
      size: "standard",
      contrast: "standard",
      motion: false,
      version: 2,
    });
    await p.loadDefaults();
    expect(p.size).toBe("standard");
  });
  it("preserves legacy browser choices and migrates safely on the next edit", async () => {
    localStorage.setItem(
      "upself.preferences.v1",
      JSON.stringify({
        size: "extraLarge",
        contrast: "standard",
        motion: false,
      }),
    );
    const p = usePreferences();
    await p.loadDefaults();
    expect(p.size).toBe("extraLarge");
    expect(p.motion).toBe(false);
    p.contrast = "highContrast";
    expect(localStorage.getItem("upself.preferences.v1")).toBeNull();
    expect(
      JSON.parse(localStorage.getItem("upself.preferences.v2")!).size,
    ).toBe("extraLarge");
  });
  it("honours system reduced motion unless the user explicitly chooses otherwise", async () => {
    media.matches = true;
    mocks.read.mockResolvedValue({
      size: "standard",
      contrast: "standard",
      motion: false,
      version: 1,
    });
    const p = usePreferences();
    await p.loadDefaults();
    expect(p.motion).toBe(true);
    p.motion = false;
    expect(p.motion).toBe(false);
    p.reset();
    expect(p.motion).toBe(true);
  });
  it("does not treat malformed stored values as valid choices", async () => {
    localStorage.setItem(
      "upself.preferences.v2",
      JSON.stringify({ size: "giant", motion: "false", contrast: null }),
    );
    const p = usePreferences();
    await p.loadDefaults();
    expect(p.hasOverrides).toBe(false);
    expect(p.motion).toBe(true);
  });
  it("keeps Aa usable during an outage and allows a retry", async () => {
    mocks.read.mockRejectedValueOnce(new Error("offline"));
    const p = usePreferences();
    await p.loadDefaults();
    expect(p.defaultsError).not.toBe("");
    p.size = "large";
    expect(p.size).toBe("large");
    await p.loadDefaults();
    expect(p.defaultsError).toBe("");
    expect(p.contrast).toBe("highContrast");
  });
  it("rejects invalid downloaded settings without erasing personal choices", async () => {
    mocks.read.mockResolvedValue({
      size: "tiny",
      contrast: "standard",
      motion: false,
      version: 1,
    });
    const p = usePreferences();
    p.size = "large";
    await p.loadDefaults();
    expect(p.defaultsError).not.toBe("");
    expect(p.size).toBe("large");
    expect(p.contrast).toBe("standard");
  });
  it("continues working when local storage is blocked", async () => {
    const p = usePreferences();
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    p.size = "extraLarge";
    await nextTick();
    expect(document.documentElement.dataset.size).toBe("extraLarge");
  });
});
