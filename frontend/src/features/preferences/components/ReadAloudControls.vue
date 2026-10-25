<script setup lang="ts">
import { computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useSpeech } from "../composables/useSpeech";
import { voiceKey } from "../speechText";
const props = defineProps<{ text: string; language?: string }>();
const { t } = useI18n();
const speech = useSpeech(() => props.language || "en");
const voiceChoice = computed({
  get: () =>
    speech.availableVoices.value.some(
      (voice) => voiceKey(voice) === speech.selectedVoice.value,
    )
      ? speech.selectedVoice.value
      : "",
  set: speech.setVoice,
});
const speedChoice = computed({
  get: () => speech.rate.value,
  set: speech.setRate,
});
watch(
  () => [props.text, props.language],
  () => speech.stop(),
);
defineExpose({ stop: speech.stop });
</script>
<template>
  <div class="read-aloud-controls">
    <div class="reading-actions">
      <el-button
        v-if="!speech.speaking.value"
        :disabled="speech.preparing.value"
        @click="speech.read(text)"
      >
        <span aria-hidden="true">🔊 </span>{{ t(speech.preparing.value ? "speechPreparing" : "readAloud") }}
      </el-button>
      <el-button v-else-if="!speech.paused.value" @click="speech.pause">
        {{
          t("pause")
        }}
      </el-button>
      <el-button v-else @click="speech.resume">
        {{ t("resume") }}
      </el-button>
      <el-button
        v-if="speech.speaking.value || speech.preparing.value"
        @click="speech.stop"
      >
        {{ t("stop") }}
      </el-button>
    </div>
    <p v-if="speech.error.value" role="status">
      {{ t(speech.error.value) }}
    </p>
    <details class="voice-settings">
      <summary>{{ t("voiceSettings") }}</summary>
      <label for="reading-voice">{{ t("readingVoice") }}</label>
      <select id="reading-voice" v-model="voiceChoice">
        <option value="">
          {{ t("automaticVoice") }}
        </option>
        <option
          v-for="voice in speech.availableVoices.value"
          :key="voiceKey(voice)"
          :value="voiceKey(voice)"
        >
          {{ voice.name }} ({{ voice.lang }})
        </option>
      </select>
      <label for="reading-speed">{{ t("readingSpeed") }}</label>
      <select id="reading-speed" v-model.number="speedChoice">
        <option :value="0.85">
          {{ t("speechSlower") }}
        </option>
        <option :value="1">
          {{ t("speechNormal") }}
        </option>
      </select>
    </details>
  </div>
</template>
<style scoped>
.voice-settings {
  margin-block: 0.75rem 1rem;
  max-width: 32rem;
}
.voice-settings summary {
  cursor: pointer;
}
.voice-settings select {
  width: 100%;
}
</style>
