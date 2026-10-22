import { ref, onUnmounted } from "vue";
export function useSpeech() {
  let utterance: SpeechSynthesisUtterance | undefined;
  const speaking = ref(false),
    paused = ref(false),
    error = ref("");
  function stop() {
    if (utterance) {
      utterance.onend = null;
      utterance.onerror = null;
      utterance = undefined;
    }
    if ("speechSynthesis" in window) speechSynthesis.cancel();
    speaking.value = false;
    paused.value = false;
  }
  function read(text: string, lang = "en") {
    stop();
    error.value = "";
    if (
      !("speechSynthesis" in window) ||
      !speechSynthesis
        .getVoices()
        .some((v) => v.lang.startsWith(lang.split("-")[0] || "en"))
    ) {
      error.value = "speechUnavailable";
      return;
    }
    const u = new SpeechSynthesisUtterance(text);
    utterance = u;
    u.lang = lang;
    u.onend = () => {
      if (utterance !== u) return;
      utterance = undefined;
      speaking.value = false;
      paused.value = false;
    };
    u.onerror = () => {
      if (utterance !== u) return;
      utterance = undefined;
      speaking.value = false;
      paused.value = false;
      error.value = "speechUnavailable";
    };
    speaking.value = true;
    try {
      speechSynthesis.speak(u);
    } catch {
      stop();
      error.value = "speechUnavailable";
    }
  }
  function pause() {
    if (!speaking.value) return;
    speechSynthesis.pause();
    paused.value = true;
  }
  function resume() {
    if (!speaking.value) return;
    speechSynthesis.resume();
    paused.value = false;
  }
  onUnmounted(stop);
  return { speaking, paused, error, read, stop, pause, resume };
}
