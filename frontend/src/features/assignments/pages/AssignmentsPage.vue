<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { assignmentApi, malaysiaDate } from "../api";
import type { Assignment } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AssignmentEditor from "../components/AssignmentEditor.vue";
const auth = useAuthStore(),
  route = useRoute(),
  router = useRouter(),
  { t } = useI18n(),
  rows = ref<Assignment[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  creating = ref(false),
  pending = ref(false);
const courseId = computed(() => {
  const n = Number(route.query.courseId);
  return Number.isSafeInteger(n) && n > 0 ? n : undefined;
});
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await assignmentApi.list(
      current.value,
      keyword.value,
      courseId.value,
      pending.value,
    );
    rows.value = r.rows;
    total.value = r.total;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function search() {
  current.value = 1;
  void load();
}
watch(courseId, search, { immediate: true });
async function created(id: number) {
  await router.push("/mentor/assignments/" + id);
}
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">
          {{ t(auth.isStudent ? "dashboard.student" : "dashboard.mentor") }}
        </p>
        <h1>
          {{ t(auth.isStudent ? "assignment.myWork" : "assignment.teaching") }}
        </h1>
      </div>
      <el-button v-if="auth.isMentor" type="primary" @click="creating = true">
        {{ t("assignment.new") }}
      </el-button>
    </div>
    <form class="search-row" @submit.prevent="search">
      <label class="sr-only" for="assignment-search">{{ t("search") }}</label
      ><input
        id="assignment-search"
        v-model="keyword"
        :placeholder="t('search')"
      /><el-button native-type="submit">
        {{ t("search") }} </el-button
      ><el-button
        :disabled="!keyword"
        @click="
          keyword = '';
          search();
        "
      >
        {{ t("clear") }}
      </el-button>
    </form>
    <div v-if="courseId" class="button-row">
      <RouterLink
        :to="
          auth.isStudent
            ? '/student/courses/' + courseId
            : '/manage/courses/' + courseId
        "
      >
        {{ t("assignment.courseLink") }} </RouterLink
      ><RouterLink
        :to="auth.isStudent ? '/student/assignments' : '/mentor/assignments'"
      >
        {{ t("assignment.allCourses") }}
      </RouterLink>
    </div>
    <label v-if="auth.isMentor" class="check"
      ><input v-model="pending" type="checkbox" @change="search" />{{
        t("assignment.pendingOnly")
      }}</label
    ><AsyncState :loading="loading" :error="error" @retry="load">
      <p v-if="!rows.length" class="panel">
        {{ t("assignment.empty") }}
      </p>
      <div class="assignment-list">
        <article v-for="a in rows" :key="a.id" class="panel assignment-card">
          <p class="eyebrow">
            {{ a.courseTitle }}
          </p>
          <h2>{{ a.title }}</h2>
          <p v-if="a.dueAt">{{ malaysiaDate(a.dueAt) }} MYT</p>
          <p v-else class="muted">
            {{ t("assignment.noDue") }}
          </p>
          <p>
            <span class="status">{{
              t(
                "assignment." +
                  (auth.isStudent ? a.myStatus || "notStarted" : a.status),
              )
            }}</span
            ><span v-if="auth.isMentor && a.pendingCount">
              ·
              {{
                t("assignment.pendingCount", { count: a.pendingCount })
              }}</span
            >
          </p>
          <RouterLink
            class="button-link"
            :to="
              (auth.isStudent
                ? '/student/assignments/'
                : '/mentor/assignments/') + a.id
            "
          >
            {{ t("assignment.open") }} →
          </RouterLink>
        </article>
      </div>
      <PaginationBar
        :current="current"
        :total="total"
        @change="
          (n) => {
            current = n;
            load();
          }
        "
      /> </AsyncState
    ><AssignmentEditor
      v-if="auth.isMentor"
      v-model="creating"
      :course-id="courseId"
      @saved="created"
    />
  </section>
</template>
