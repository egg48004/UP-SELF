<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../../learning/api";
import type { Course } from "../../learning/types";
import { assignmentApi } from "../../assignments/api";
import type { Assignment } from "../../assignments/types";
import CourseCard from "../../learning/components/CourseCard.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n(),
  courses = ref<Course[]>([]),
  work = ref<Assignment[]>([]),
  busy = ref(true),
  error = ref("");
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const [c, a] = await Promise.all([
      learningApi.courses({ current: 1, pageSize: 3 }),
      assignmentApi.list(1, "", undefined, false, 3),
    ]);
    courses.value = c.rows;
    work.value = a.rows;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section>
    <div class="course-banner">
      <span aria-hidden="true">🌱</span>
      <div>
        <p class="eyebrow">
          {{ t("dashboard.student") }}
        </p>
        <h1>{{ t("dashboard.studentTitle") }}</h1>
        <p>{{ t("dashboard.studentIntro") }}</p>
      </div>
    </div>
    <AsyncState :loading="busy" :error="error" @retry="load">
      <div class="dashboard-grid">
        <section class="panel">
          <h2>📝 {{ t("assignment.myWork") }}</h2>
          <p v-if="!work.length">
            {{ t("assignment.empty") }}
          </p>
          <RouterLink
            v-for="a in work"
            :key="a.id"
            class="dashboard-row"
            :to="'/student/assignments/' + a.id"
          >
            <strong>{{ a.title }}</strong
            ><span>{{
              t("assignment." + (a.myStatus || "notStarted"))
            }}</span> </RouterLink
          ><RouterLink to="/student/assignments">
            {{ t("assignment.myWork") }} →
          </RouterLink>
        </section>
        <section class="panel">
          <h2>📚 {{ t("myCourses") }}</h2>
          <p v-if="!courses.length">
            {{ t("dashboard.emptyCourses") }}
          </p>
          <CourseCard
            v-for="(c, index) in courses"
            :key="c.id"
            :course="c"
            :index="index"
          /><RouterLink to="/student/courses">
            {{ t("dashboard.continueLearning") }} →
          </RouterLink>
        </section>
      </div>
    </AsyncState>
  </section>
</template>
