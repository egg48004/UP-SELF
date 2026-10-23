<script setup lang="ts">
import { useI18n } from "vue-i18n";
import CourseFacts from "./CourseFacts.vue";
import type { Course } from "../types";
defineProps<{ course: Course; index: number }>();
const { t } = useI18n();
</script>
<template>
  <article class="course-card">
    <div :class="['course-art', 'art-' + (index % 3)]" aria-hidden="true">
      <span>{{ ["🌱", "🧭", "💡"][index % 3] }}</span><small>UP-SELF</small>
    </div>
    <div class="course-card-body">
      <span class="eyebrow">{{ t("myCourses") }}</span>
      <h2>{{ course.title }}</h2>
      <p class="course-summary">
        {{ course.description }}
      </p>
      <CourseFacts :course="course" />
      <RouterLink class="button-link" :to="'/student/courses/' + course.id">
        {{ t("openCourse") }} <span aria-hidden="true">→</span>
      </RouterLink>
    </div>
  </article>
</template>
