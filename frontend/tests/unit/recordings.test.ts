import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import { useRecorder } from "../../src/features/recordings/composables/useRecorder";
vi.mock("../../src/features/recordings/playback", () => ({
  stopOtherMedia: vi.fn(),
}));
class Recorder {
  static current: Recorder;
  static isTypeSupported = () => true;
  state = "inactive";
  mimeType = "audio/webm;codecs=opus";
  ondataavailable: ((event: { data: Blob }) => void) | null = null;
  onstop: (() => void) | null = null;
  onerror: (() => void) | null = null;
  constructor() {
    Recorder.current = this;
  }
  start() {
    this.state = "recording";
  }
  stop() {
    this.state = "inactive";
    this.ondataavailable?.({ data: new Blob(["test audio"]) });
    this.onstop?.();
  }
}
describe("recording lifecycle", () => {
  let microphone: ReturnType<typeof vi.fn>, stopTrack: ReturnType<typeof vi.fn>;
  let views: ReturnType<typeof mount>[];
  function render() {
    let recording!: ReturnType<typeof useRecorder>;
    const view = mount(
      defineComponent({
        setup() {
          recording = useRecorder();
          return () => null;
        },
      }),
    );
    views.push(view);
    return { recording, view };
  }
  beforeEach(() => {
    views = [];
    stopTrack = vi.fn();
    microphone = vi
      .fn()
      .mockResolvedValue({ getTracks: () => [{ stop: stopTrack }] });
    vi.stubGlobal("MediaRecorder", Recorder);
    vi.stubGlobal("navigator", { mediaDevices: { getUserMedia: microphone } });
  });
  afterEach(() => {
    views.forEach((view) => {
      if (view.exists()) view.unmount();
    });
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });
  it("creates a replayable file, releases the microphone and supports deletion", async () => {
    const { recording } = render();
    await recording.start();
    expect(recording.active.value).toBe(true);
    recording.stop();
    expect(recording.file.value?.name).toMatch(/\.webm$/);
    expect(recording.file.value?.size).toBeGreaterThan(0);
    expect(stopTrack).toHaveBeenCalled();
    expect(recording.active.value).toBe(false);
    recording.remove();
    expect(recording.file.value).toBeNull();
  });
  it("keeps a permission failure recoverable without leaving recording active", async () => {
    microphone.mockRejectedValue(new Error("denied"));
    const { recording } = render();
    await recording.start();
    expect(recording.error.value).toBe("recording.denied");
    expect(recording.active.value).toBe(false);
  });
  it("releases a microphone granted after the request was canceled", async () => {
    let grant!: (value: unknown) => void;
    microphone.mockImplementation(
      () =>
        new Promise((resolve) => {
          grant = resolve;
        }),
    );
    const { recording } = render();
    const pending = recording.start();
    recording.stop();
    grant({ getTracks: () => [{ stop: stopTrack }] });
    await pending;
    expect(stopTrack).toHaveBeenCalledOnce();
    expect(recording.file.value).toBeNull();
    expect(recording.active.value).toBe(false);
  });
  it("stops recording and releases tracks when the page unmounts", async () => {
    const { recording, view } = render();
    await recording.start();
    view.unmount();
    await flushPromises();
    expect(stopTrack).toHaveBeenCalled();
    expect(Recorder.current.state).toBe("inactive");
    expect(recording.file.value).toBeNull();
  });
  it("does not let a canceled permission rejection stop a newer recording", async () => {
    let reject!: (error: Error) => void;
    microphone.mockImplementationOnce(
      () =>
        new Promise((_resolve, failed) => {
          reject = failed;
        }),
    );
    const { recording } = render();
    const old = recording.start();
    recording.stop();
    await recording.start();
    reject(new Error("old request denied"));
    await old;
    expect(recording.state.value).toBe("recording");
    expect(stopTrack).not.toHaveBeenCalled();
  });
  it("keeps the recorded audio when the five-minute limit stops capture", async () => {
    vi.useFakeTimers();
    const { recording } = render();
    await recording.start();
    await vi.advanceTimersByTimeAsync(300000);
    expect(recording.active.value).toBe(false);
    expect(recording.file.value?.size).toBeGreaterThan(0);
  });
});
