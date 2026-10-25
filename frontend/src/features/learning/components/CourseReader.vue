<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Chapter, Resource } from "../types";
import MediaViewer from "./MediaViewer.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import ReadAloudControls from "../../preferences/components/ReadAloudControls.vue";
const props = defineProps<{ chapter: Chapter }>(),
  { t } = useI18n(),
  resources = ref<Resource[]>([]),
  loading = ref(false),
  error = ref(""),
  reading = ref<InstanceType<typeof ReadAloudControls> | null>(null);
let seq = 0;
async function load() {
  const n = ++seq;
  reading.value?.stop();
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
    <ReadAloudControls
      v-if="chapter.body"
      ref="reading"
      :text="chapter.body"
      :language="chapter.languageCode"
    />
    <p class="plain-text lesson-body">
      {{ chapter.body }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <MediaViewer
        v-for="r in resources.filter((r) => r.kind !== 'CAPTION')"
        :key="r.id"
        :resource="r"
        @play="reading?.stop()"
      />
      <p v-if="!resources.length && !chapter.body">
        {{ t("emptyChapter") }}
      </p>
    </AsyncState>
  </article>
</template>
