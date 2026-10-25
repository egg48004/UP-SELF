import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
  toValue,
} from "vue";
import type { MaybeRefOrGetter } from "vue";
import { matchingVoices, splitSpeechText, voiceKey } from "../speechText";

const preferenceKey = "upself.speech.v1";
const speeds = [0.85, 1];
let activeStop: (() => void) | undefined;

/** Route changes and videos stop the controller as well as the native queue. */
export function stopSpeech() {
  if (activeStop) activeStop();
  else if ("speechSynthesis" in window) window.speechSynthesis.cancel();
}

export function useSpeech(language: MaybeRefOrGetter<string> = "en") {
  const engine =
    "speechSynthesis" in window ? window.speechSynthesis : undefined;
  const voices = shallowRef<SpeechSynthesisVoice[]>([]);
  const speaking = ref(false),
    paused = ref(false),
    preparing = ref(false),
    error = ref("");
  const selectedVoice = ref(""),
    rate = ref(1);
  let utterance: SpeechSynthesisUtterance | undefined;
  let chunks: string[] = [],
    nextIndex = 0,
    generation = 0;
  let nextTimer: ReturnType<typeof setTimeout> | undefined;
  let startTimer: ReturnType<typeof setTimeout> | undefined;
  let cancelVoiceWait: (() => void) | undefined;
  let currentVoice: SpeechSynthesisVoice | undefined;
  const availableVoices = computed(() =>
    matchingVoices(voices.value, toValue(language)),
  );

  try {
    const saved = JSON.parse(localStorage.getItem(preferenceKey) || "{}");
    selectedVoice.value = typeof saved.voice === "string" ? saved.voice : "";
    rate.value = speeds.includes(saved.rate) ? saved.rate : 1;
  } catch {
    /* Use normal speech when storage is unavailable or invalid. */
  }

  function refreshVoices() {
    voices.value = engine?.getVoices() || [];
  }
  function clearUtterance() {
    if (utterance) {
      utterance.onstart = utterance.onend = utterance.onerror = null;
      utterance = undefined;
    }
    clearTimeout(startTimer);
  }
  function stop() {
    generation++;
    clearTimeout(nextTimer);
    cancelVoiceWait?.();
    cancelVoiceWait = undefined;
    clearUtterance();
    chunks = [];
    nextIndex = 0;
    if (activeStop === stop) {
      activeStop = undefined;
      engine?.cancel();
      // Chrome can retain a paused queue after cancel(). Do not toggle pause on a timer.
      if (engine?.paused) engine.resume();
    }
    speaking.value = paused.value = preparing.value = false;
    error.value = "";
  }
  function fail(key: string) {
    stop();
    error.value = key;
  }
  function persist() {
    try {
      localStorage.setItem(
        preferenceKey,
        JSON.stringify({ voice: selectedVoice.value, rate: rate.value }),
      );
    } catch {
      /* Reading still works without preference storage. */
    }
  }
  function setVoice(value: string) {
    stop();
    selectedVoice.value = availableVoices.value.some(
      (voice) => voiceKey(voice) === value,
    )
      ? value
      : "";
    persist();
  }
  function setRate(value: number) {
    stop();
    rate.value = speeds.includes(value) ? value : 1;
    persist();
  }
  function waitForVoices(lang: string) {
    return new Promise<void>((resolve) => {
      const finish = () => {
        clearTimeout(timer);
        engine?.removeEventListener("voiceschanged", changed);
        cancelVoiceWait = undefined;
        resolve();
      };
      const changed = () => {
        refreshVoices();
        if (matchingVoices(voices.value, lang).length) finish();
      };
      const timer = setTimeout(finish, 2000);
      cancelVoiceWait = finish;
      engine?.addEventListener("voiceschanged", changed);
      changed();
    });
  }
  function speakNext(token: number) {
    if (!engine || token !== generation || paused.value) return;
    if (nextIndex >= chunks.length) {
      stop();
      return;
    }
    const u = new SpeechSynthesisUtterance(chunks[nextIndex++]!);
    utterance = u;
    u.voice = currentVoice!;
    u.lang = currentVoice!.lang;
    u.rate = rate.value;
    u.pitch = 1;
    u.volume = 1;
    u.onstart = () => {
      if (utterance === u && token === generation) clearTimeout(startTimer);
    };
    u.onend = () => {
      if (utterance !== u || token !== generation) return;
      clearUtterance();
      // Enqueue only after completion. A pause between chunks must stay paused.
      nextTimer = setTimeout(() => speakNext(token), 0);
    };
    u.onerror = (event) => {
      if (utterance !== u || token !== generation) return;
      if (event.error === "canceled" || event.error === "interrupted") stop();
      else fail("speechFailed");
    };
    try {
      startTimer = setTimeout(() => {
        if (utterance === u && token === generation && !paused.value)
          fail("speechFailed");
      }, 8000);
      engine.speak(u);
    } catch {
      fail("speechFailed");
    }
  }
  async function read(text: string, lang = toValue(language)) {
    stopSpeech();
    stop();
    error.value = "";
    if (!engine || !("SpeechSynthesisUtterance" in window)) {
      error.value = "speechUnavailable";
      return;
    }
    chunks = splitSpeechText(text);
    if (!chunks.length) return;
    activeStop = stop;
    const token = generation;
    refreshVoices();
    if (!matchingVoices(voices.value, lang).length) {
      preparing.value = true;
      await waitForVoices(lang);
      if (token !== generation) return;
      preparing.value = false;
    }
    const candidates = matchingVoices(voices.value, lang);
    if (
      selectedVoice.value &&
      !candidates.some((voice) => voiceKey(voice) === selectedVoice.value)
    ) {
      selectedVoice.value = "";
      persist();
    }
    currentVoice =
      candidates.find((voice) => voiceKey(voice) === selectedVoice.value) ||
      candidates[0];
    if (!currentVoice) {
      fail("speechUnavailable");
      return;
    }
    if (engine.paused) engine.resume();
    speaking.value = true;
    speakNext(token);
  }
  function pause() {
    if (!speaking.value || paused.value) return;
    paused.value = true;
    clearTimeout(startTimer);
    if (utterance) engine?.pause();
  }
  function resume() {
    if (!speaking.value || !paused.value) return;
    paused.value = false;
    if (utterance) engine?.resume();
    else {
      clearTimeout(nextTimer);
      speakNext(generation);
    }
  }
  onMounted(() => {
    refreshVoices();
    engine?.addEventListener("voiceschanged", refreshVoices);
    window.addEventListener("pagehide", stop);
  });
  onUnmounted(() => {
    stop();
    engine?.removeEventListener("voiceschanged", refreshVoices);
    window.removeEventListener("pagehide", stop);
  });
  return {
    speaking,
    paused,
    preparing,
    error,
    availableVoices,
    selectedVoice,
    rate,
    read,
    stop,
    pause,
    resume,
    setVoice,
    setRate,
  };
}
