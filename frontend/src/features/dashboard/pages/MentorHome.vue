<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { assignmentApi } from "../../assignments/api";
import type { Assignment } from "../../assignments/types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n(),
  work = ref<Assignment[]>([]),
  busy = ref(true),
  error = ref("");
async function load() {
  busy.value = true;
  error.value = "";
  try {
    work.value = (await assignmentApi.list(1, "", undefined, true, 5)).rows;
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
    <p class="eyebrow">
      {{ t("dashboard.mentor") }}
    </p>
    <h1>{{ t("dashboard.mentorTitle") }}</h1>
    <p>{{ t("dashboard.mentorIntro") }}</p>
    <section class="panel">
      <h2>{{ t("progress.overview") }}</h2>
      <p>{{ t("progress.overviewHelp") }}</p>
      <RouterLink class="button-link" to="/mentor/students">
        {{ t("progress.overview") }} →
      </RouterLink>
    </section>
    <div class="dashboard-grid">
      <section class="panel">
        <h2>{{ t("efficacy.studentCheckIns") }}</h2>
        <p>{{ t("efficacy.mentorIntro") }}</p>
        <RouterLink class="button-link" to="/mentor/mood">
          {{ t("efficacy.studentCheckIns") }} →
        </RouterLink>
      </section>
      <section class="panel">
        <h2>{{ t("dashboard.teachingCourses") }}</h2>
        <p>{{ t("dashboard.prepareHelp") }}</p>
        <RouterLink class="button-link" to="/manage/courses">
          {{ t("dashboard.teachingCourses") }} →
        </RouterLink>
      </section>
      <section class="panel">
        <h2>{{ t("assignment.pending") }}</h2>
        <p>{{ t("dashboard.reviewHelp") }}</p>
        <AsyncState :loading="busy" :error="error" @retry="load">
          <p v-if="!work.length">
            {{ t("assignment.noSubmissions") }}
          </p>
          <RouterLink
            v-for="a in work"
            :key="a.id"
            class="dashboard-row"
            :to="'/mentor/assignments/' + a.id"
          >
            <strong>{{ a.title }}</strong><span>{{
              t("assignment.pendingCount", { count: a.pendingCount })
            }}</span>
          </RouterLink>
        </AsyncState><RouterLink to="/mentor/assignments">
          {{ t("assignment.teaching") }} →
        </RouterLink>
      </section>
    </div>
  </section>
</template>
