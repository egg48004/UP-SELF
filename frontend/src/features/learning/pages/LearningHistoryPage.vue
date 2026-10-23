<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { progressApi, learningDate } from "../progress";
import type { LearningItem } from "../progress";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  rows = ref<LearningItem[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(n = current.value) {
  const active = ++sequence;
  current.value = n;
  loading.value = true;
  error.value = "";
  try {
    const r = await progressApi.history(n);
    if(active !== sequence) return;
    rows.value = r.rows;
    total.value = r.total;
  } catch (e) {
    if(active === sequence) error.value = (e as Error).message;
  } finally {
    if(active === sequence) loading.value = false;
  }
}
onMounted(() => load());
</script>
<template>
  <section>
    <h1>📖 {{ t("progress.history") }}</h1>
    <p>{{ t("progress.historyHelp") }}</p>
    <AsyncState :loading="loading" :error="error" @retry="load()">
      <p v-if="!rows.length" class="panel">
        {{ t("progress.historyEmpty") }}
      </p>
      <ol class="stack history-list">
        <li v-for="row in rows" :key="row.chapterId" class="panel">
          <h2>{{ row.chapterTitle }}</h2>
          <p>{{ row.courseTitle }}</p>
          <p>
            {{ t("progress.completedAt") }}: {{ learningDate(row.completedAt) }}
          </p>
          <RouterLink
            class="button-link"
            :to="{
              path: '/student/courses/' + row.courseId,
              query: { chapterId: row.chapterId },
            }"
          >
            {{ t("progress.open") }} →
          </RouterLink>
        </li>
      </ol>
      <PaginationBar
        v-if="total"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
  </section>
</template>
