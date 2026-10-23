<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Course } from "../types";
defineProps<{ course: Course }>();
const { t } = useI18n();
</script>
<template>
  <p class="course-facts">
    <span>{{ t("progress.difficulty") }}:
      {{ t("progress." + course.difficulty) }}</span>
    ·
    <span>{{ t("progress.minutes", { count: course.estimatedMinutes }) }}</span>
  </p>
</template>
