<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { knowledgeApi } from "../api";
import type { KnowledgeAnswer, KnowledgeQuestion } from "../types";
import KnowledgeAnswerForm from "../components/KnowledgeAnswerForm.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  route = useRoute(),
  rows = ref<KnowledgeQuestion[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
const courseId = computed(() => {
  const id = Number(route.query.courseId);
  return Number.isSafeInteger(id) && id > 0 ? id : undefined;
});
let sequence = 0;
async function load(page = current.value) {
  current.value = page;
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const result = await knowledgeApi.own(page, courseId.value);
    if (seq === sequence) {
      rows.value = result.rows;
      total.value = result.total;
    }
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
function saved(question: KnowledgeQuestion, answer: KnowledgeAnswer) {
  question.choice = answer.choice;
  question.answeredAt = answer.updateTime;
}
watch(courseId, () => load(1), { immediate: true });
</script>
<template>
  <section>
    <div class="course-banner">
      <span aria-hidden="true">💡</span>
      <div>
        <p class="eyebrow">
          {{ t("efficacy.checkIn") }}
        </p>
        <h1>{{ t("knowledge.title") }}</h1>
        <p>{{ t("knowledge.intro") }}</p>
      </div>
    </div>
    <p>{{ t("knowledge.sharing") }}</p>
    <RouterLink v-if="courseId" to="/student/knowledge">
      {{ t("knowledge.allQuestions") }}
    </RouterLink>
    <AsyncState :loading="loading" :error="error" @retry="load()">
      <p v-if="!rows.length" class="panel">
        {{ t("knowledge.empty") }}
      </p>
      <KnowledgeAnswerForm
        v-for="question in rows"
        :key="question.id + '-' + question.version"
        :question="question"
        @saved="saved(question, $event)"
      />
      <PaginationBar :current="current" :total="total" @change="load" />
    </AsyncState>
  </section>
</template>
