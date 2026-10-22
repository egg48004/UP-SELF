<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Chapter, Resource } from "../types";
import MediaViewer from "./MediaViewer.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { useSpeech } from "../../preferences/composables/useSpeech";
const props = defineProps<{ chapter: Chapter }>(),
  { t } = useI18n(),
  resources = ref<Resource[]>([]),
  loading = ref(false),
  error = ref(""),
  speech = useSpeech();
let seq = 0;
async function load() {
  const n = ++seq;
  speech.stop();
  loading.value = true;
  error.value = "";
  try {
    const r = await learningApi.resources(props.chapter.id);
    if (n === seq) resources.value = r;
  } catch (e) {
    if (n === seq) error.value = (e as Error).message;
  } finally {
    if (n === seq) loading.value = false;
  }
}
watch(() => props.chapter.id, load, { immediate: true });
</script>
<template>
  <article class="reader">
    <h2>{{ chapter.title }}</h2>
    <div v-if="chapter.body" class="reading-actions">
      <el-button
        v-if="!speech.speaking.value"
        @click="speech.read(chapter.body, chapter.languageCode)"
      >
        <span aria-hidden="true">🔊 </span>{{ t("readAloud") }}
      </el-button><el-button v-else-if="!speech.paused.value" @click="speech.pause">
        {{
          t("pause")
        }}
      </el-button><el-button v-else @click="speech.resume">
        {{ t("resume") }}
      </el-button><el-button v-if="speech.speaking.value" @click="speech.stop">
        {{
          t("stop")
        }}
      </el-button>
      <p v-if="speech.error.value" role="status">
        {{ t(speech.error.value) }}
      </p>
    </div>
    <p class="plain-text lesson-body">
      {{ chapter.body }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <MediaViewer
        v-for="r in resources.filter((r) => r.kind !== 'CAPTION')"
        :key="r.id"
        :resource="r"
      />
      <p v-if="!resources.length && !chapter.body">
        {{ t("emptyChapter") }}
      </p>
    </AsyncState>
  </article>
</template>
