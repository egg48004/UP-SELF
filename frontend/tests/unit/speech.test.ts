import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { mount } from "@vue/test-utils";
import type { VueWrapper } from "@vue/test-utils";
import {
  stopSpeech,
  useSpeech,
} from "../../src/features/preferences/composables/useSpeech";
import {
  matchingVoices,
  splitSpeechText,
  voiceKey,
} from "../../src/features/preferences/speechText";

const english = {
  name: "English local",
  lang: "en-US",
  voiceURI: "english",
  localService: true,
  default: false,
} as SpeechSynthesisVoice;
const chinese = {
  name: "System default",
  lang: "zh-CN",
  voiceURI: "chinese",
  localService: true,
  default: true,
} as SpeechSynthesisVoice;
class Utterance {
  lang = "";
  voice: SpeechSynthesisVoice | null = null;
  rate = 9;
  pitch = 9;
  volume = 0;
  onstart: (() => void) | null = null;
  onend: (() => void) | null = null;
  onerror: ((event: { error: string }) => void) | null = null;
  constructor(public text: string) {}
}

describe("read aloud playback", () => {
  let engine: EventTarget & {
    cancel: ReturnType<typeof vi.fn>;
    speak: ReturnType<typeof vi.fn>;
    pause: ReturnType<typeof vi.fn>;
    resume: ReturnType<typeof vi.fn>;
    getVoices: ReturnType<typeof vi.fn>;
    paused: boolean;
  };
  let speech: ReturnType<typeof useSpeech>;
  let spoken: Utterance[];
  let views: VueWrapper[];
  function reader() {
    let controller!: ReturnType<typeof useSpeech>;
    const view = mount(
      defineComponent({
        setup() {
          controller = useSpeech();
          return () => null;
        },
      }),
    );
    views.push(view);
    return { controller, view };
  }
  beforeEach(() => {
    vi.useFakeTimers();
    localStorage.clear();
    views = [];
    spoken = [];
    engine = Object.assign(new EventTarget(), {
      cancel: vi.fn(),
      speak: vi.fn((u: Utterance) => spoken.push(u)),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => [chinese, english]),
      paused: false,
    });
    engine.pause.mockImplementation(() => {
      engine.paused = true;
    });
    engine.resume.mockImplementation(() => {
      engine.paused = false;
    });
    vi.stubGlobal("speechSynthesis", engine);
    vi.stubGlobal("SpeechSynthesisUtterance", Utterance);
    speech = reader().controller;
  });
  afterEach(() => {
    for (const view of views) if (view.exists()) view.unmount();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });
  it("selects an actual English voice instead of the system's Chinese default", async () => {
    await speech.read("One small step.", "en-GB");
    expect(spoken[0]).toMatchObject({
      text: "One small step.",
      voice: english,
      lang: "en-US",
      rate: 1,
      pitch: 1,
      volume: 1,
    });
  });
  it("reads the whole long text in order with only one outstanding utterance", async () => {
    const text = Array.from(
      { length: 100 },
      (_, i) => `Step ${i + 1}. Take one small action and check your result.`,
    ).join(" ");
    await speech.read(text);
    expect(spoken).toHaveLength(1);
    let index = 0;
    while (speech.speaking.value) {
      const u = spoken[index++]!;
      expect(u.text.length).toBeLessThanOrEqual(160);
      u.onstart?.();
      u.onend?.();
      await vi.advanceTimersByTimeAsync(0);
      expect(spoken.length - index).toBeLessThanOrEqual(1);
    }
    expect(spoken.map((u) => u.text).join(" ")).toBe(text);
    expect(index).toBeGreaterThan(20);
  });
  it("pauses and resumes without replaying or altering the current text", async () => {
    await speech.read("One small step.");
    spoken[0]!.onstart?.();
    speech.pause();
    speech.pause();
    expect(engine.pause).toHaveBeenCalledOnce();
    expect(speech.paused.value).toBe(true);
    speech.resume();
    speech.resume();
    expect(engine.resume).toHaveBeenCalledOnce();
    expect(spoken).toHaveLength(1);
  });
  it("keeps a pause between chunks until Resume is pressed", async () => {
    await speech.read("A small step. ".repeat(50));
    spoken[0]!.onend?.();
    speech.pause();
    await vi.advanceTimersByTimeAsync(0);
    expect(spoken).toHaveLength(1);
    speech.resume();
    expect(spoken).toHaveLength(2);
    expect(speech.paused.value).toBe(false);
  });
  it("stops the pending next chunk when Stop follows an end event", async () => {
    await speech.read("A small step. ".repeat(50));
    spoken[0]!.onend?.();
    stopSpeech();
    await vi.advanceTimersByTimeAsync(0);
    expect(spoken).toHaveLength(1);
    expect(speech.speaking.value).toBe(false);
  });
  it("clears Chrome's paused queue before starting a new reading", async () => {
    await speech.read("First reading.");
    speech.pause();
    speech.stop();
    expect(engine.paused).toBe(false);
    await speech.read("Second reading.");
    expect(spoken[1]?.text).toBe("Second reading.");
    expect(speech.paused.value).toBe(false);
  });
  it("ignores late completion from a canceled reading", async () => {
    await speech.read("First reading.");
    const oldEnd = spoken[0]!.onend!;
    await speech.read("Second reading.");
    oldEnd();
    await vi.advanceTimersByTimeAsync(0);
    expect(spoken).toHaveLength(2);
    expect(speech.speaking.value).toBe(true);
  });
  it("stops the previous reader and does not let its unmount cancel the new reader", async () => {
    await speech.read("First reader.");
    const second = reader();
    await second.controller.read("Second reader.");
    expect(speech.speaking.value).toBe(false);
    const count = engine.cancel.mock.calls.length;
    views[0]!.unmount();
    expect(engine.cancel.mock.calls.length).toBe(count);
    expect(second.controller.speaking.value).toBe(true);
  });
  it("waits for asynchronous voice loading", async () => {
    engine.getVoices.mockReturnValue([]);
    const pending = speech.read("Hello.");
    expect(speech.preparing.value).toBe(true);
    expect(spoken).toHaveLength(0);
    engine.getVoices.mockReturnValue([english]);
    engine.dispatchEvent(new Event("voiceschanged"));
    await pending;
    expect(spoken).toHaveLength(1);
    expect(speech.preparing.value).toBe(false);
  });
  it("cannot resume a voice-loading request canceled by Stop", async () => {
    engine.getVoices.mockReturnValue([]);
    const pending = speech.read("Hello.");
    speech.stop();
    engine.getVoices.mockReturnValue([english]);
    engine.dispatchEvent(new Event("voiceschanged"));
    await pending;
    await vi.advanceTimersByTimeAsync(2000);
    expect(spoken).toHaveLength(0);
    expect(speech.preparing.value).toBe(false);
  });
  it("shows a fallback instead of reading English with a different-language voice", async () => {
    engine.getVoices.mockReturnValue([chinese]);
    await speech.read("Hello.");
    expect(speech.error.value).toBe("speechUnavailable");
    expect(spoken).toHaveLength(0);
  });
  it("times out an empty voice list", async () => {
    engine.getVoices.mockReturnValue([]);
    const pending = speech.read("Hello.");
    await vi.advanceTimersByTimeAsync(2000);
    await pending;
    expect(speech.error.value).toBe("speechUnavailable");
    expect(speech.preparing.value).toBe(false);
  });
  it("treats native interruption as Stop without queuing more speech", async () => {
    await speech.read("A small step. ".repeat(50));
    spoken[0]!.onerror?.({ error: "interrupted" });
    await vi.advanceTimersByTimeAsync(0);
    expect(speech.error.value).toBe("");
    expect(spoken).toHaveLength(1);
    expect(speech.speaking.value).toBe(false);
  });
  it("stops on a synthesis error without skipping or replaying words", async () => {
    await speech.read("A small step. ".repeat(50));
    spoken[0]!.onerror?.({ error: "synthesis-failed" });
    await vi.advanceTimersByTimeAsync(0);
    expect(speech.error.value).toBe("speechFailed");
    expect(spoken).toHaveLength(1);
  });
  it("recovers from a synchronous native failure", async () => {
    engine.speak.mockImplementation(() => {
      throw new Error("unavailable");
    });
    await speech.read("Hello.");
    expect(speech.error.value).toBe("speechFailed");
    expect(speech.speaking.value).toBe(false);
  });
  it("does not leave a silent blocked engine in a permanent Reading state", async () => {
    await speech.read("Hello.");
    await vi.advanceTimersByTimeAsync(8000);
    expect(speech.speaking.value).toBe(false);
    expect(speech.error.value).toBe("speechFailed");
  });
  it("keeps the chosen voice and a bounded rate on this device", async () => {
    speech.setVoice(voiceKey(english));
    speech.setRate(0.85);
    await speech.read("Hello.");
    expect(spoken[0]?.rate).toBe(0.85);
    expect(spoken[0]?.voice).toBe(english);
    const second = reader();
    expect(second.controller.rate.value).toBe(0.85);
    second.controller.setRate(10);
    expect(second.controller.rate.value).toBe(1);
  });
  it("stops and clears state on page hide", async () => {
    await speech.read("Hello.");
    window.dispatchEvent(new Event("pagehide"));
    expect(speech.speaking.value).toBe(false);
    expect(engine.cancel).toHaveBeenCalled();
  });
});

describe("speech text and voice matching", () => {
  it("normalizes whitespace without removing words, numbers or punctuation", () => {
    const text = "  Plan 1.\nUse RM 12.50, then check it.\n\nDon't rush.  ";
    expect(splitSpeechText(text).join(" ")).toBe(
      "Plan 1. Use RM 12.50, then check it. Don't rush.",
    );
    expect(splitSpeechText(" \n ")).toEqual([]);
  });
  it("keeps long text bounded even when it contains no sentence punctuation", () => {
    const text = "Read this slowly ".repeat(300).trim();
    const parts = splitSpeechText(text);
    expect(
      parts.every((p) => p.length <= 160 && p.split(" ").length <= 24),
    ).toBe(true);
    expect(parts.join(" ")).toBe(text);
  });
  it("never cuts a surrogate pair in a long unbroken token", () => {
    expect(splitSpeechText("😀".repeat(300)).join("")).toBe("😀".repeat(300));
  });
  it("prefers a local language-matching voice and keeps natural voices available", () => {
    const remote = { ...english, name: "Remote English", localService: false };
    const natural = { ...english, name: "Natural English" };
    expect(
      matchingVoices([chinese, remote, english, natural], "EN_gb"),
    ).toEqual([natural, english, remote]);
  });
});
