<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { reportApi } from "../api";
import type { Report } from "../api";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { learningDate } from "../../learning/progress";
const { t } = useI18n(),
  rows = ref<Report[]>([]),
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
    const r = await reportApi.mine(page);
    if (seq !== sequence) return;
    const last = Math.max(1, Math.ceil(r.total / 10));
    if (page > last) return await load(last);
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
    <h1><span aria-hidden="true">📋 </span>{{ t("reports.mine") }}</h1>
    <p>{{ t("reports.mineHelp") }}</p>
    <el-button :disabled="loading" @click="load(current)">
      {{
        t("reports.refreshMine")
      }}
    </el-button><AsyncState :loading="loading" :error="error" @retry="load(current)">
      <p v-if="!rows.length">
        {{ t("reports.empty") }}
      </p>
      <ul class="reports-list">
        <li v-for="r in rows" :key="r.id">
          <article class="panel">
            <h2>{{ r.accusedName }}</h2>
            <p>{{ t("reports." + r.reason) }}</p>
            <p>
              <strong>{{ t("reports." + r.status) }}</strong> ·
              {{ learningDate(r.createTime) }}
            </p>
            <p v-if="r.reviewedAt">
              {{ t("reports.reviewed") }}: {{ learningDate(r.reviewedAt) }}
            </p>
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
.reports-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.reports-list article {
  overflow-wrap: anywhere;
}
</style>
