import { defineStore } from "pinia";
import { computed, onScopeDispose, ref, watch, watchEffect } from "vue";
import { accessibilityApi } from "./api";
import type { PlatformDefaults, ReadingSettings } from "./api";
const key = "upself.preferences.v2",
  legacyKey = "upself.preferences.v1";
function saved(): Partial<ReadingSettings> {
  try {
    const raw = JSON.parse(
      localStorage.getItem(key) ?? localStorage.getItem(legacyKey) ?? "{}",
    );
    if (!raw || typeof raw !== "object") return {};
    const result: Partial<ReadingSettings> = {};
    if (["standard", "large", "extraLarge"].includes(raw.size))
      result.size = raw.size;
    if (["standard", "highContrast"].includes(raw.contrast))
      result.contrast = raw.contrast;
    if (typeof raw.motion === "boolean") result.motion = raw.motion;
    return result;
  } catch {
    return {};
  }
}
export const usePreferences = defineStore("preferences", () => {
  const defaults = ref<PlatformDefaults>({
    size: "standard",
    contrast: "standard",
    motion: false,
    version: 0,
  });
  const overrides = ref<Partial<ReadingSettings>>(saved()),
    defaultsError = ref(""),
    loadingDefaults = ref(false);
  const media =
    typeof matchMedia === "function"
      ? matchMedia("(prefers-reduced-motion: reduce)")
      : undefined;
  const systemMotion = ref(media?.matches ?? false);
  function systemChanged() {
    systemMotion.value = media?.matches ?? false;
  }
  media?.addEventListener?.("change", systemChanged);
  onScopeDispose(() => media?.removeEventListener?.("change", systemChanged));
  function set<K extends keyof ReadingSettings>(
    field: K,
    value: ReadingSettings[K],
  ) {
    overrides.value = { ...overrides.value, [field]: value };
  }
  const size = computed({
    get: () => overrides.value.size ?? defaults.value.size,
    set: (v: ReadingSettings["size"]) => set("size", v),
  });
  const contrast = computed({
    get: () => overrides.value.contrast ?? defaults.value.contrast,
    set: (v: ReadingSettings["contrast"]) => set("contrast", v),
  });
  const motion = computed({
    get: () =>
      overrides.value.motion ?? (defaults.value.motion || systemMotion.value),
    set: (v: boolean) => set("motion", v),
  });
  const hasOverrides = computed(() => Object.keys(overrides.value).length > 0);
  watch(
    overrides,
    (value) => {
      try {
        localStorage.setItem(key, JSON.stringify(value));
        localStorage.removeItem(legacyKey);
      } catch {
        /* Settings still work when browser storage is unavailable. */
      }
    },
    { deep: true, flush: "sync" },
  );
  watchEffect(() => {
    document.documentElement.dataset.size = size.value;
    document.documentElement.dataset.contrast = contrast.value;
    document.documentElement.dataset.motion = String(motion.value);
  });
  function acceptDefaults(value: PlatformDefaults) {
    if (
      !value ||
      !["standard", "large", "extraLarge"].includes(value.size) ||
      !["standard", "highContrast"].includes(value.contrast) ||
      typeof value.motion !== "boolean" ||
      !Number.isInteger(value.version) ||
      value.version < 0
    )
      throw new Error("Platform reading defaults could not be read.");
    defaults.value = {
      size: value.size,
      contrast: value.contrast,
      motion: value.motion,
      version: value.version,
    };
  }
  let pending: Promise<void> | undefined;
  function loadDefaults() {
    if (pending) return pending;
    loadingDefaults.value = true;
    defaultsError.value = "";
    pending = (async () => {
      try {
        acceptDefaults(await accessibilityApi.read());
      } catch {
        defaultsError.value =
          "Platform defaults are unavailable. Your current settings still work.";
      } finally {
        loadingDefaults.value = false;
        pending = undefined;
      }
    })();
    return pending;
  }
  function reset() {
    overrides.value = {};
  }
  return {
    size,
    contrast,
    motion,
    hasOverrides,
    defaultsError,
    loadingDefaults,
    loadDefaults,
    acceptDefaults,
    reset,
  };
});
