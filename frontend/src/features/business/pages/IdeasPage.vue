<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ideaApi } from "../api";
import type { BusinessIdea } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { learningDate } from "../../learning/progress";
const { t } = useI18n(),
  rows = ref<BusinessIdea[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const r = await ideaApi.list(page);
    if (seq !== sequence) return;
    rows.value = r.rows;
    total.value = r.total;
    current.value = page;
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
onMounted(() => load());
</script>
<template>
  <section>
    <RouterLink to="/student/business">
      ← {{ t("business.back") }}
    </RouterLink>
    <h1><span aria-hidden="true">🌱 </span>{{ t("business.ideas") }}</h1>
    <p>{{ t("business.ideasHelp") }}</p>
    <RouterLink class="button-link" to="/student/business/ideas/new">
      {{
        t("business.create")
      }}
    </RouterLink>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div v-if="!rows.length" class="panel">
        <h2>{{ t("business.empty") }}</h2>
        <p>{{ t("business.emptyHelp") }}</p>
      </div>
      <ul v-else class="idea-list">
        <li v-for="idea in rows" :key="idea.id">
          <article class="panel">
            <h2>{{ idea.title }}</h2>
            <p v-if="idea.offer" class="idea-offer">
              {{ idea.offer }}
            </p>
            <p class="muted">
              {{ learningDate(idea.updateTime) }}
            </p>
            <RouterLink
              class="button-link"
              :to="'/student/business/ideas/' + idea.id"
            >
              {{ t("business.view") }}
            </RouterLink>
          </article>
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
.idea-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.idea-offer {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
