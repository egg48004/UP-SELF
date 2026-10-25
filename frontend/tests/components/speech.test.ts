import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import type { VueWrapper } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
import CourseReader from "../../src/features/learning/components/CourseReader.vue";
import en from "../../src/shared/i18n/en";

const api = vi.hoisted(() => ({ resources: vi.fn(), access: vi.fn() }));
vi.mock("../../src/features/learning/api", () => ({ learningApi: api }));
class Utterance {
  text: string;
  onstart: (() => void) | null = null;
  onend: (() => void) | null = null;
  onerror: (() => void) | null = null;
  constructor(text: string) {
    this.text = text;
  }
}
describe("lesson reading controls", () => {
  let wrapper: VueWrapper;
  let engine: EventTarget & {
    cancel: ReturnType<typeof vi.fn>;
    speak: ReturnType<typeof vi.fn>;
    pause: ReturnType<typeof vi.fn>;
    resume: ReturnType<typeof vi.fn>;
    getVoices: ReturnType<typeof vi.fn>;
    paused: boolean;
  };
  const chapter = {
    id: 1,
    courseId: 1,
    title: "A lesson",
    body: "Read one small step. ".repeat(40),
    languageCode: "en",
    status: "PUBLISHED",
    sortOrder: 1,
  };
  const render = async () => {
    wrapper = mount(CourseReader, {
      props: { chapter },
      global: {
        components: { ElButton },
        plugins: [
          createI18n({ legacy: false, locale: "en", messages: { en } }),
        ],
      },
    });
    await flushPromises();
  };
  const button = (label: string) =>
    wrapper.findAll("button").find((b) => b.text().endsWith(label))!;
  beforeEach(() => {
    vi.spyOn(HTMLMediaElement.prototype, "pause").mockImplementation(() => {});
    localStorage.clear();
    api.resources.mockResolvedValue([]);
    api.access.mockResolvedValue("/fixture.mp4");
    engine = Object.assign(new EventTarget(), {
      cancel: vi.fn(),
      speak: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => [
        {
          name: "Samantha",
          voiceURI: "english",
          lang: "en-US",
          localService: true,
          default: true,
        },
      ]),
      paused: false,
    });
    vi.stubGlobal("speechSynthesis", engine);
    vi.stubGlobal("SpeechSynthesisUtterance", Utterance);
  });
  afterEach(() => {
    wrapper?.unmount();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  it("connects Read, Pause, Resume and Stop to the playback controller", async () => {
    await render();
    await button("Read aloud").trigger("click");
    expect(engine.speak).toHaveBeenCalledOnce();
    await button("Pause").trigger("click");
    expect(engine.pause).toHaveBeenCalledOnce();
    await button("Resume").trigger("click");
    expect(engine.resume).toHaveBeenCalledOnce();
    await button("Stop").trigger("click");
    expect(button("Read aloud")).toBeDefined();
    expect(button("Pause")).toBeUndefined();
  });
  it("stops the old text when a chapter body changes without changing its ID", async () => {
    await render();
    await button("Read aloud").trigger("click");
    const old = engine.speak.mock.calls[0]![0] as Utterance;
    await wrapper.setProps({
      chapter: { ...chapter, body: "Updated lesson text." },
    });
    expect(old.onend).toBeNull();
    expect(button("Read aloud")).toBeDefined();
    await button("Read aloud").trigger("click");
    expect(engine.speak.mock.calls.at(-1)![0]).toMatchObject({
      text: "Updated lesson text.",
    });
  });
  it("stops playback when voice or speed changes and uses the saved choice", async () => {
    await render();
    await button("Read aloud").trigger("click");
    await wrapper.get("#reading-speed").setValue("0.85");
    expect(button("Read aloud")).toBeDefined();
    await wrapper.get("#reading-voice").setValue("english|en-US|Samantha");
    await button("Read aloud").trigger("click");
    expect(engine.speak.mock.calls.at(-1)![0]).toMatchObject({
      rate: 0.85,
      pitch: 1,
      lang: "en-US",
      voice: { name: "Samantha" },
    });
  });
  it("shows only three ordinary voices and two speeds", async () => {
    engine.getVoices.mockReturnValue(
      [
        "Zarvox",
        "Whisper",
        "Cellos",
        "Bubbles",
        "Samantha",
        "Alex",
        "Daniel",
        "Karen",
        "Moira",
      ].map((name) => ({
        name,
        voiceURI: name,
        lang: "en-US",
        localService: true,
        default: false,
      })),
    );
    await render();
    const options = wrapper.findAll("#reading-voice option");
    expect(options).toHaveLength(4); // Automatic plus at most three voices.
    expect(options[0]!.text()).toBe("Automatic");
    expect(options.slice(1).map((option) => option.text())).toEqual([
      "Alex (en-US)",
      "Daniel (en-US)",
      "Karen (en-US)",
    ]);
    expect(
      wrapper.findAll("#reading-speed option").map((option) => option.text()),
    ).toEqual(["Slower", "Normal"]);
    await button("Read aloud").trigger("click");
    expect(engine.speak.mock.calls[0]![0]).toMatchObject({
      voice: { name: "Alex" },
      rate: 1,
    });
  });
  it("displays Automatic and Normal for an old effect voice and fast speed", async () => {
    localStorage.setItem(
      "upself.speech.v1",
      JSON.stringify({
        voice: "effect|en-US|Whisper",
        rate: 1.15,
      }),
    );
    await render();
    expect(
      (wrapper.get("#reading-voice").element as HTMLSelectElement).value,
    ).toBe("");
    expect(
      (wrapper.get("#reading-speed").element as HTMLSelectElement).value,
    ).toBe("1");
    await button("Read aloud").trigger("click");
    expect(engine.speak.mock.calls[0]![0]).toMatchObject({
      voice: { name: "Samantha" },
      rate: 1,
    });
  });
  it("video playback stops speech and resets the lesson buttons", async () => {
    api.resources.mockResolvedValue([
      {
        id: 10,
        chapterId: 1,
        kind: "VIDEO",
        title: "A video",
        languageCode: "en",
        status: "PUBLISHED",
      },
    ]);
    await render();
    await button("Read aloud").trigger("click");
    await wrapper.get("video").trigger("play");
    expect(button("Read aloud")).toBeDefined();
    expect(button("Pause")).toBeUndefined();
    expect((engine.speak.mock.calls[0]![0] as Utterance).onend).toBeNull();
  });
});
