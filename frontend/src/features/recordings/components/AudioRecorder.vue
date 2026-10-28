<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRecorder } from "../composables/useRecorder";
import { stopOtherMedia } from "../playback";
const model = defineModel<File | null>({ default: null });
const props = defineProps<{
  disabled?: boolean;
  uploadable?: boolean;
  uploadDisabled?: boolean;
  maxSeconds?: number;
  maxBytes?: number;
  limitHelp?: string;
  sizeError?: string;
  helpText?: string;
}>();
const emit = defineEmits<{
  activity: [active: boolean];
  upload: [file: File];
}>();
const { t } = useI18n(),
  recording = useRecorder({
    maxSeconds: props.maxSeconds,
    maxBytes: props.maxBytes,
  }),
  preview = ref("");
const previewAudio = ref<HTMLAudioElement | null>(null);
watch(recording.file, (file) => {
  model.value = file;
});
watch(model, (file) => {
  previewAudio.value?.pause();
  if (preview.value) URL.revokeObjectURL(preview.value);
  preview.value = file ? URL.createObjectURL(file) : "";
  if (!file && !recording.active.value) recording.remove();
});
watch(recording.active, (active) => emit("activity", active), {
  immediate: true,
});
function beforeUnload(event: BeforeUnloadEvent) {
  if (model.value || recording.active.value) {
    event.preventDefault();
    event.returnValue = "";
  }
}
window.addEventListener("beforeunload", beforeUnload);
onBeforeUnmount(() => {
  previewAudio.value?.pause();
  window.removeEventListener("beforeunload", beforeUnload);
  if (preview.value) URL.revokeObjectURL(preview.value);
  emit("activity", false);
});
</script>
<template>
  <section class="voice-recorder" :aria-label="t('recording.title')">
    <h3><span aria-hidden="true">🎙 </span>{{ t("recording.title") }}</h3>
    <p>{{ helpText || t("recording.help") }}</p>
    <div class="button-row">
      <el-button
        v-if="!recording.active.value"
        :disabled="disabled"
        @click="recording.start"
      >
        {{ t("recording.record") }}
      </el-button>
      <el-button
        v-else-if="recording.state.value === 'preparing'"
        @click="recording.stop"
      >
        {{ t("recording.cancel") }}
      </el-button>
      <el-button
        v-else
        :disabled="recording.state.value === 'finishing'"
        @click="recording.stop"
      >
        {{ t("recording.stop") }}
      </el-button>
      <el-button
        v-if="model && !recording.active.value"
        :disabled="disabled"
        @click="recording.remove"
      >
        {{ t("recording.remove") }}
      </el-button>
      <el-button
        v-if="model && uploadable"
        :disabled="disabled || uploadDisabled || recording.active.value"
        @click="emit('upload', model)"
      >
        {{ t("recording.upload") }}
      </el-button>
    </div>
    <p v-if="recording.state.value === 'recording'" role="status">
      {{ t("recording.recording", { time: recording.formatTime.value }) }}
    </p>
    <p v-else-if="recording.state.value !== 'idle'" role="status">
      {{
        t(
          recording.state.value === "preparing"
            ? "recording.preparing"
            : "recording.finishing",
        )
      }}
    </p>
    <audio
      v-if="preview"
      ref="previewAudio"
      :src="preview"
      controls
      preload="metadata"
      :aria-label="t('recording.preview')"
      @play="stopOtherMedia($event.target as HTMLMediaElement)"
    />
    <p v-if="recording.error.value" role="alert" class="notice error">
      {{
        recording.error.value === "recording.tooLarge" && sizeError
          ? sizeError
          : t(recording.error.value)
      }}
    </p>
    <p class="muted">
      {{ limitHelp || t("recording.timeHelp") }}
    </p>
  </section>
</template>
<style scoped>
.voice-recorder {
  margin-block: 1rem;
}
audio {
  display: block;
  width: 100%;
  max-width: 30rem;
  margin-block: 1rem;
}
</style>
