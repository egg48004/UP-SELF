<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Attempt } from "../types";
import { malaysiaDate } from "../api";
import WorkFiles from "./WorkFiles.vue";
defineProps<{ attempts: Attempt[] }>();
const { t } = useI18n();
</script>
<template>
  <section v-if="attempts.length">
    <h2>{{ t("assignment.history") }}</h2>
    <article v-for="a in attempts" :key="a.id" class="panel attempt-card">
      <div class="page-heading">
        <h3>{{ t("assignment.attempt", { number: a.attemptNo }) }}</h3>
        <span class="status">{{ t("assignment." + a.status) }}</span>
      </div>
      <p class="muted">
        {{ malaysiaDate(a.submittedAt) }} MYT
        <span v-if="a.late">· {{ t("assignment.late") }}</span>
      </p>
      <p v-if="a.answerText" class="plain-text">
        {{ a.answerText }}
      </p>
      <WorkFiles :files="a.files" />
      <section
        v-if="a.status === 'GRADED' || a.status === 'RETURNED'"
        class="feedback-panel"
      >
        <h3>💬 {{ t("assignment.feedback") }}</h3>
        <p v-if="a.score !== null" class="grade-result">
          {{ t("assignment.scoreResult", { score: a.score, max: a.maxScore }) }}
        </p>
        <p class="plain-text">
          {{ a.feedback }}
        </p>
        <p class="muted">{{ malaysiaDate(a.reviewedAt) }} MYT</p>
      </section>
      <p v-else role="status">
        {{ t("assignment.waiting") }}
      </p>
    </article>
  </section>
</template>
