<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { knowledgeApi } from "../api";
import { learningApi } from "../../learning/api";
import type { Course } from "../../learning/types";
import type { KnowledgeQuestion, QuestionInput } from "../types";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const props = defineProps<{
  question?: KnowledgeQuestion;
  courseId?: number;
}>();
const emit = defineEmits<{ saved: []; cancel: [] }>();
const { t } = useI18n(),
  form = reactive<QuestionInput>({
    questionText: props.question?.questionText || "",
    status: props.question?.status || "DRAFT",
    version: props.question?.version || 0,
  });
const selected = ref<{ id: number; title: string } | null>(
    props.question
      ? { id: props.question.courseId, title: props.question.courseTitle }
      : null,
  ),
  courses = ref<Course[]>([]),
  current = ref(1),
  total = ref(0),
  keyword = ref(""),
  loading = ref(false),
  busy = ref(false),
  error = ref("");
async function search(page = 1) {
  loading.value = true;
  error.value = "";
  current.value = page;
  try {
    const result = await learningApi.courses({
      current: page,
      pageSize: 10,
      keyword: keyword.value,
    });
    courses.value = result.rows;
    total.value = result.total;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function save() {
  if (!selected.value || busy.value || !form.questionText.trim()) return;
  busy.value = true;
  error.value = "";
  try {
    if (props.question) await knowledgeApi.update(props.question.id, form);
    else await knowledgeApi.create(selected.value.id, form);
    emit("saved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onMounted(async () => {
  if (props.question) return;
  if (props.courseId) {
    try {
      selected.value = await learningApi.course(props.courseId);
    } catch (e) {
      error.value = (e as Error).message;
    }
  } else await search();
});
</script>
<template>
  <div>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <template v-if="!selected">
      <h3>{{ t("knowledge.chooseCourse") }}</h3>
      <form class="search-row" @submit.prevent="search()">
        <label for="knowledge-course-search" class="sr-only">{{
          t("search")
        }}</label><input
          id="knowledge-course-search"
          v-model="keyword"
          maxlength="100"
        /><el-button native-type="submit">
          {{ t("search") }}
        </el-button>
      </form>
      <AsyncState :loading="loading" @retry="search()">
        <ul class="content-list">
          <li v-for="course in courses" :key="course.id">
            <span>{{ course.title }}</span><el-button
              :disabled="course.status === 'ARCHIVED'"
              @click="selected = course"
            >
              {{ t("confirm") }}
            </el-button>
          </li>
        </ul>
        <p v-if="!courses.length">
          {{ t("noCourses") }}
        </p>
        <PaginationBar :current="current" :total="total" @change="search" />
      </AsyncState>
    </template>
    <form v-else @submit.prevent="save">
      <p>
        {{ t("knowledge.selectedCourse") }}:
        <strong>{{ selected.title }}</strong>
      </p>
      <el-button
        v-if="!question && !courseId"
        :disabled="busy"
        @click="
          selected = null;
          search();
        "
      >
        {{ t("knowledge.changeCourse") }}
      </el-button>
      <fieldset :disabled="busy" class="question-fields">
        <label for="knowledge-question-text">{{
          t("knowledge.question")
        }}</label>
        <p id="knowledge-question-help" class="muted">
          {{ t("knowledge.questionHelp") }}
        </p>
        <p v-if="question?.answerCount" class="notice">
          {{ t("knowledge.locked") }}
        </p>
        <textarea
          id="knowledge-question-text"
          v-model="form.questionText"
          :readonly="!!question?.answerCount"
          maxlength="500"
          rows="3"
          required
          aria-describedby="knowledge-question-help"
        />
        <label for="knowledge-question-status">{{ t("status") }}</label>
        <select id="knowledge-question-status" v-model="form.status">
          <option value="DRAFT">
            {{ t("draft") }}
          </option>
          <option value="PUBLISHED">
            {{ t("published") }}
          </option>
          <option value="ARCHIVED">
            {{ t("archived") }}
          </option>
        </select>
      </fieldset>
      <div class="actions">
        <el-button native-type="submit" type="primary" :loading="busy">
          {{ t("save") }}
        </el-button><el-button :disabled="busy" @click="emit('cancel')">
          {{ t("cancel") }}
        </el-button>
      </div>
    </form>
  </div>
</template>
<style scoped>
.question-fields {
  border: 0;
  padding: 0;
  min-width: 0;
  margin-block: 1rem;
}
</style>
