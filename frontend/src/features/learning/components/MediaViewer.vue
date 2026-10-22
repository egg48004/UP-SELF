<script setup lang="ts">
import { ref, watch, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Resource } from "../types";
const props = defineProps<{ resource: Resource }>(),
  { t } = useI18n(),
  src = ref(""),
  caption = ref(""),
  error = ref(""),
  loading = ref(true),
  attempt = ref(0),
  video = ref<HTMLVideoElement | null>(null);
let sequence = 0,
  disposed = false;
async function load() {
  const n = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const urls = await Promise.all([
      learningApi.access(props.resource.id),
      props.resource.captionResourceId
        ? learningApi.access(props.resource.captionResourceId)
        : Promise.resolve(""),
    ]);
    if (n === sequence && !disposed) {
      src.value = urls[0] || "";
      caption.value = urls[1] || "";
    }
  } catch (e) {
    if (n === sequence) error.value = (e as Error).message;
  } finally {
    if (n === sequence) loading.value = false;
  }
}
async function failed() {
  if (attempt.value < 1) {
    attempt.value++;
    await load();
  } else error.value = t("mediaError");
}
function stopSpeech() {
  if ("speechSynthesis" in window) speechSynthesis.cancel();
}
function retry() {
  attempt.value = 0;
  void load();
}
watch(
  () => props.resource.id,
  () => {
    attempt.value = 0;
    src.value = "";
    void load();
  },
  { immediate: true },
);
onUnmounted(() => {
  disposed = true;
  sequence++;
  video.value?.pause();
  src.value = "";
  caption.value = "";
});
</script>
<template>
  <figure class="media-block">
    <figcaption>
      <h3>{{ resource.title }}</h3>
    </figcaption>
    <p v-if="loading" role="status">
      {{ t("mediaLoading") }}
    </p>
    <div v-else-if="error" role="alert" class="notice error">
      <p>{{ error }}</p>
      <el-button @click="retry">
        {{ t("tryMedia") }}
      </el-button>
    </div>
    <template v-else>
      <img
        v-if="resource.kind === 'IMAGE'"
        :src="src"
        :alt="resource.altText || ''"
        @error="failed"
      /><template v-else-if="resource.kind === 'VIDEO'">
        <video
          :key="src"
          ref="video"
          controls
          preload="metadata"
          :src="src"
          :aria-label="resource.title"
          @error="failed"
          @play="stopSpeech"
        >
          <track
            v-if="caption"
            :src="caption"
            kind="captions"
            :srclang="resource.languageCode"
            :label="t('caption')"
            default
          />
        </video>
        <p v-if="!caption" class="muted">
          {{ t("noCaptions") }}
        </p>
        <details v-if="resource.transcript">
          <summary>{{ t("showTranscript") }}</summary>
          <p class="plain-text">
            {{ resource.transcript }}
          </p>
        </details>
      </template><a v-else :href="src" target="_blank" rel="noopener noreferrer">{{ t("viewCaption") }}
        <span class="sr-only">{{ t("opensNewTab") }}</span></a>
    </template>
  </figure>
</template>
