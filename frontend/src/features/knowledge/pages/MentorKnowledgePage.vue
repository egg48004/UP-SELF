<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { knowledgeApi } from "../api";
import type { KnowledgeQuestion } from "../types";
import KnowledgeQuestionEditor from "../components/KnowledgeQuestionEditor.vue";
import KnowledgeResponses from "../components/KnowledgeResponses.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  route = useRoute(),
  rows = ref<KnowledgeQuestion[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  loadError = ref(""),
  error = ref(""),
  notice = ref(""),
  busy = ref(false),
  editing = ref(false),
  selected = ref<KnowledgeQuestion>(),
  responses = ref<KnowledgeQuestion>();
const courseId = computed(() => {
  const id = Number(route.query.courseId);
  return Number.isSafeInteger(id) && id > 0 ? id : undefined;
});
let sequence = 0;
async function load(page = current.value) {
  const seq = ++sequence;
  current.value = page;
  loading.value = true;
  loadError.value = "";
  try {
    const result = await knowledgeApi.manage(page, courseId.value);
    if (seq === sequence) {
      rows.value = result.rows;
      total.value = result.total;
    }
  } catch (e) {
    if (seq === sequence) loadError.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
function edit(question?: KnowledgeQuestion) {
  selected.value = question;
  editing.value = true;
  error.value = "";
  notice.value = "";
}
async function saved() {
  editing.value = false;
  notice.value = t("knowledge.questionSaved");
  await load();
}
async function change(
  question: KnowledgeQuestion,
  status: KnowledgeQuestion["status"],
) {
  if (busy.value) return;
  if (
    status === "ARCHIVED" &&
    !(await confirmAction(t("knowledge.archiveConfirm")))
  )
    return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    await knowledgeApi.update(question.id, {
      questionText: question.questionText,
      status,
      version: question.version,
    });
    notice.value = t("knowledge.questionSaved");
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function remove(question: KnowledgeQuestion) {
  if (busy.value || !(await confirmAction(t("knowledge.deleteConfirm"))))
    return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    await knowledgeApi.remove(question.id, question.version);
    notice.value = t("knowledge.questionRemoved");
    await load(
      Math.max(1, rows.value.length === 1 ? current.value - 1 : current.value),
    );
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(
  courseId,
  () => {
    responses.value = undefined;
    void load(1);
  },
  { immediate: true },
);
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">
          {{ t("dashboard.mentor") }}
        </p>
        <h1>{{ t("knowledge.questions") }}</h1>
        <p>{{ t("knowledge.mentorIntro") }}</p>
      </div>
      <el-button type="primary" @click="edit()">
        {{ t("knowledge.newQuestion") }}
      </el-button>
    </div>
    <RouterLink v-if="courseId" to="/mentor/knowledge">
      {{ t("knowledge.allQuestions") }}
    </RouterLink>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <p v-if="notice" role="status">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="loadError" @retry="load()">
      <p v-if="!rows.length" class="panel">
        {{ t("knowledge.emptyManage") }}
      </p>
      <article v-for="question in rows" :key="question.id" class="panel">
        <p class="eyebrow">
          {{ question.courseTitle }}
        </p>
        <h2>{{ question.questionText }}</h2>
        <p>
          <span class="status">{{ t(question.status.toLowerCase()) }}</span> ·
          {{ t("knowledge.answerCount", { count: question.answerCount || 0 }) }}
        </p>
        <div class="actions">
          <el-button
            :disabled="busy || question.courseStatus === 'ARCHIVED'"
            @click="edit(question)"
          >
            {{ t("edit") }}
          </el-button>
          <el-button
            v-if="question.status !== 'PUBLISHED'"
            :disabled="busy || question.courseStatus === 'ARCHIVED'"
            @click="change(question, 'PUBLISHED')"
          >
            {{ t("publish") }}
          </el-button>
          <el-button
            v-if="question.status !== 'DRAFT'"
            :disabled="busy || question.courseStatus === 'ARCHIVED'"
            @click="change(question, 'DRAFT')"
          >
            {{ t("unpublish") }}
          </el-button>
          <el-button
            v-if="question.status !== 'ARCHIVED'"
            :disabled="busy || question.courseStatus === 'ARCHIVED'"
            @click="change(question, 'ARCHIVED')"
          >
            {{ t("archive") }}
          </el-button>
          <el-button
            :disabled="
              busy ||
                !!question.answerCount ||
                question.courseStatus === 'ARCHIVED'
            "
            @click="remove(question)"
          >
            {{ t("delete") }}
          </el-button>
          <el-button
            :disabled="question.courseStatus !== 'PUBLISHED'"
            @click="responses = question"
          >
            {{ t("knowledge.answers") }}
          </el-button>
        </div>
        <p v-if="question.answerCount" class="muted">
          {{ t("knowledge.locked") }}
        </p>
      </article>
      <PaginationBar :current="current" :total="total" @change="load" />
    </AsyncState>
    <el-dialog
      v-model="editing"
      :title="t(selected ? 'knowledge.editQuestion' : 'knowledge.newQuestion')"
      width="min(650px,95vw)"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <KnowledgeQuestionEditor
        v-if="editing"
        :question="selected"
        :course-id="courseId"
        @saved="saved"
        @cancel="editing = false"
      />
    </el-dialog>
    <el-dialog
      :model-value="!!responses"
      :title="t('knowledge.responses')"
      width="min(800px,95vw)"
      destroy-on-close
      @update:model-value="responses = undefined"
    >
      <template v-if="responses">
        <h2>{{ responses.questionText }}</h2>
        <KnowledgeResponses :question-id="responses.id" />
      </template>
    </el-dialog>
  </section>
</template>
