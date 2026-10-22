import { defineStore } from "pinia";
import { ref, watchEffect } from "vue";
export const usePreferences = defineStore("preferences", () => {
  let stored: { size?: string; contrast?: string; motion?: boolean } = {};
  try {
    stored = JSON.parse(localStorage.getItem("upself.preferences.v1") || "{}");
  } catch {
    /* invalid local preference resets safely */
  }
  const size = ref(
      ["standard", "large", "extraLarge"].includes(stored.size || "")
        ? stored.size!
        : "standard",
    ),
    contrast = ref(
      stored.contrast === "highContrast" ? "highContrast" : "standard",
    ),
    motion = ref(
      stored.motion ?? matchMedia("(prefers-reduced-motion: reduce)").matches,
    );
  watchEffect(() => {
    document.documentElement.dataset.size = size.value;
    document.documentElement.dataset.contrast = contrast.value;
    document.documentElement.dataset.motion = String(motion.value);
    localStorage.setItem(
      "upself.preferences.v1",
      JSON.stringify({
        size: size.value,
        contrast: contrast.value,
        motion: motion.value,
      }),
    );
  });
  function reset() {
    size.value = "standard";
    contrast.value = "standard";
    motion.value = matchMedia("(prefers-reduced-motion: reduce)").matches;
  }
  return { size, contrast, motion, reset };
});
