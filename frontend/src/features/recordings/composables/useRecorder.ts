import { computed, onUnmounted, ref, shallowRef } from "vue";
import { stopOtherMedia } from "../playback";
export function useRecorder() {
  const state = ref<"idle" | "preparing" | "recording" | "finishing">("idle"),
    seconds = ref(0),
    error = ref("");
  const file = shallowRef<File | null>(null);
  const active = computed(() => state.value !== "idle");
  let stream: MediaStream | undefined,
    recorder: MediaRecorder | undefined,
    timer: ReturnType<typeof setInterval> | undefined;
  let sequence = 0,
    disposed = false;
  const closeTracks = () => {
    stream?.getTracks().forEach((track) => track.stop());
    stream = undefined;
    clearInterval(timer);
  };
  const formatTime = computed(
    () =>
      `${Math.floor(seconds.value / 60)}:${String(seconds.value % 60).padStart(2, "0")}`,
  );
  function stop() {
    if (state.value === "preparing") {
      sequence++;
      state.value = "idle";
      return;
    }
    if (recorder?.state === "recording") {
      state.value = "finishing";
      clearInterval(timer);
      recorder.stop();
    }
  }
  async function start() {
    if (active.value) return;
    error.value = "";
    if (
      !navigator.mediaDevices?.getUserMedia ||
      typeof MediaRecorder === "undefined"
    ) {
      error.value = "recording.unavailable";
      return;
    }
    const mime = [
      "audio/webm;codecs=opus",
      "audio/mp4",
      "audio/ogg;codecs=opus",
    ].find((type) => MediaRecorder.isTypeSupported(type));
    if (!mime) {
      error.value = "recording.unavailable";
      return;
    }
    const token = ++sequence;
    state.value = "preparing";
    try {
      const opened = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (disposed || token !== sequence) {
        opened.getTracks().forEach((track) => track.stop());
        return;
      }
      stream = opened;
      stopOtherMedia();
      const current = new MediaRecorder(opened, { mimeType: mime });
      recorder = current;
      const chunks: Blob[] = [];
      let size = 0,
        failed = false;
      current.ondataavailable = (event) => {
        if (disposed || token !== sequence) return;
        if (event.data.size) {
          chunks.push(event.data);
          size += event.data.size;
        }
        if (size > 20 * 1024 * 1024) {
          failed = true;
          error.value = "recording.tooLarge";
          stop();
        }
      };
      current.onerror = () => {
        if (disposed || token !== sequence) return;
        failed = true;
        error.value = "recording.failed";
        closeTracks();
        state.value = "idle";
      };
      current.onstop = () => {
        if (disposed || token !== sequence) {
          opened.getTracks().forEach((track) => track.stop());
          return;
        }
        closeTracks();
        if (!failed) {
          const type = current.mimeType || mime,
            ext = type.includes("mp4")
              ? "m4a"
              : type.includes("ogg")
                ? "ogg"
                : "webm";
          if (size)
            file.value = new File(chunks, `recording-${Date.now()}.${ext}`, {
              type,
            });
          else error.value = "recording.empty";
        }
        state.value = "idle";
      };
      current.start(1000);
      file.value = null;
      seconds.value = 0;
      state.value = "recording";
      const started = Date.now();
      timer = setInterval(() => {
        seconds.value = Math.floor((Date.now() - started) / 1000);
        if (seconds.value >= 300) stop();
      }, 250);
    } catch {
      if (!disposed && token === sequence) {
        closeTracks();
        state.value = "idle";
        error.value = "recording.denied";
      }
    }
  }
  function remove() {
    if (!active.value) {
      file.value = null;
      error.value = "";
    }
  }
  onUnmounted(() => {
    disposed = true;
    sequence++;
    closeTracks();
    if (recorder) {
      recorder.onstop = recorder.ondataavailable = recorder.onerror = null;
      if (recorder.state !== "inactive") recorder.stop();
    }
  });
  return {
    state,
    seconds,
    formatTime,
    error,
    file,
    active,
    start,
    stop,
    remove,
  };
}
