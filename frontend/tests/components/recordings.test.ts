import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
import AudioPlayer from "../../src/features/recordings/components/AudioPlayer.vue";
import en from "../../src/shared/i18n/en";
vi.mock("../../src/features/recordings/playback", () => ({
  stopOtherMedia: vi.fn(),
}));
describe("private recording player", () => {
  beforeEach(() => {
    vi.stubGlobal("URL", {
      createObjectURL: vi.fn(() => "blob:private-recording"),
      revokeObjectURL: vi.fn(),
    });
    vi.spyOn(HTMLMediaElement.prototype, "play").mockResolvedValue();
    vi.spyOn(HTMLMediaElement.prototype, "pause").mockImplementation(() => {});
  });
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  function render(load: () => Promise<Blob>) {
    return mount(AudioPlayer, {
      props: { sourceKey: "diary-1", label: "My diary recording", load },
      global: {
        components: { ElButton },
        plugins: [
          createI18n({ legacy: false, locale: "en", messages: { en } }),
        ],
      },
    });
  }
  it("loads only on request and stops and revokes the private URL before unmount", async () => {
    const load = vi
      .fn()
      .mockResolvedValue(new Blob(["audio"], { type: "audio/wav" }));
    const w = render(load);
    expect(load).not.toHaveBeenCalled();
    await w.get("button").trigger("click");
    await flushPromises();
    expect(load).toHaveBeenCalledOnce();
    expect(w.get("audio").attributes("controls")).toBeDefined();
    w.unmount();
    expect(HTMLMediaElement.prototype.pause).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:private-recording");
  });
  it("does not expose a player when authenticated loading fails", async () => {
    const w = render(vi.fn().mockRejectedValue(new Error("Please sign in.")));
    await w.get("button").trigger("click");
    await flushPromises();
    expect(w.find("audio").exists()).toBe(false);
    expect(w.get('[role="status"]').text()).toBe("Please sign in.");
    w.unmount();
  });
});
