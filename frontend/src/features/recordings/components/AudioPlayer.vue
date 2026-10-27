<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { stopOtherMedia } from "../playback";
const props = defineProps<{
  sourceKey: string;
  label: string;
  load: () => Promise<Blob>;
}>();
const { t } = useI18n(),
  url = ref(""),
  loading = ref(false),
  error = ref(""),
  audio = ref<HTMLAudioElement | null>(null);
let sequence = 0;
function clear() {
  sequence++;
  audio.value?.pause();
  if (url.value) URL.revokeObjectURL(url.value);
  url.value = "";
  loading.value = false;
  error.value = "";
}
async function listen() {
  if (loading.value) return;
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const blob = await props.load();
    if (seq !== sequence) return;
    url.value = URL.createObjectURL(blob);
    await nextTick();
    try {
      await audio.value?.play();
    } catch {
      error.value = t("recording.playFailed");
    }
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
watch(() => props.sourceKey, clear);
onBeforeUnmount(clear);
</script>
<template>
  <div class="audio-player">
    <el-button
      v-if="!url"
      :loading="loading"
      :aria-label="t('recording.listen') + ': ' + label"
      @click="listen"
    >
      <span aria-hidden="true">▶ </span>{{ t("recording.listen") }}
    </el-button>
    <audio
      v-else
      ref="audio"
      :src="url"
      controls
      preload="metadata"
      :aria-label="label"
      @error="error = t('recording.cannotPlay')"
      @play="stopOtherMedia($event.target as HTMLMediaElement)"
    />
    <p v-if="error" role="status">
      {{ error }}
    </p>
  </div>
</template>
<style scoped>
audio {
  display: block;
  width: 100%;
  max-width: 30rem;
}
.audio-player {
  width: 100%;
  max-width: 30rem;
  flex: 1 1 16rem;
  min-width: 0;
}
</style>
