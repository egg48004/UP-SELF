<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { questionnaireApi } from "../api";
import { safeQuestionnaireUrl } from "../links";
import { useQuestionnaireList } from "../useQuestionnaireList";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n();
const { rows, current, total, loading, error, load } = useQuestionnaireList(
  questionnaireApi.visible,
);
</script>
<template>
  <section>
    <h1><span aria-hidden="true">📋 </span>{{ t("questionnaire.title") }}</h1>
    <p>{{ t("questionnaire.intro") }}</p>
    <p class="muted">
      {{ t("questionnaire.external") }}
    </p>
    <el-button :disabled="loading" @click="load()">
      {{
        t("questionnaire.refresh")
      }}
    </el-button>
    <AsyncState :loading="loading" :error="error" @retry="load()">
      <p v-if="!rows.length" class="panel">
        {{ t("questionnaire.empty") }}
      </p>
      <ul v-else class="questionnaire-list">
        <li v-for="row in rows" :key="row.id" class="panel">
          <h2>{{ row.title }}</h2>
          <a
            v-if="safeQuestionnaireUrl(row.url)"
            :href="safeQuestionnaireUrl(row.url) || undefined"
            target="_blank"
            rel="noopener noreferrer"
            :aria-label="t('questionnaire.open') + ': ' + row.title"
          >
            {{ t("questionnaire.open") }} <span aria-hidden="true">↗</span>
          </a>
          <p v-else>
            {{ t("questionnaire.unavailable") }}
          </p>
        </li>
      </ul>
      <PaginationBar
        v-if="total > 10"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
  </section>
</template>
<style scoped>
.questionnaire-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.questionnaire-list li {
  overflow-wrap: anywhere;
}
.questionnaire-list a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  gap: 0.5rem;
}
</style>
