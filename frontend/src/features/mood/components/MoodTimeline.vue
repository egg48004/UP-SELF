<script setup lang="ts">
import { ref, toRef } from "vue";
import { useI18n } from "vue-i18n";
import { useMoodTimeline } from "../composables/useMoodTimeline";
import MoodTrend from "./MoodTrend.vue";
import MoodHistory from "./MoodHistory.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const viewMode = ref<"chart" | "table">("chart");
const props = defineProps<{
    enabled: boolean;
    studentId?: number;
    privateNotes?: boolean;
  }>(),
  { t } = useI18n();
const {
  rows,
  trend,
  current,
  total,
  weeks,
  historyLoading,
  trendLoading,
  historyError,
  trendError,
  history,
  chart,
  reload,
} = useMoodTimeline(toRef(props, "enabled"), toRef(props, "studentId"));
defineExpose({ reload });
</script>
<template>
  <section class="panel">
    <div class="page-heading">
      <h2>{{ t("mood.trends") }}</h2>
      <div>
        <label for="mood-weeks">{{ t("mood.range") }}</label><select id="mood-weeks" v-model.number="weeks">
          <option v-for="n in [4, 12, 26, 52]" :key="n" :value="n">
            {{ t("mood.weeks" + n) }}
          </option>
        </select>
      </div>
    </div>
    <AsyncState :loading="trendLoading" :error="trendError" @retry="chart">
      <MoodTrend v-if="trend" v-model:mode="viewMode" :trend="trend" />
    </AsyncState>
  </section>
  <MoodHistory
    :rows="rows"
    :current="current"
    :total="total"
    :loading="historyLoading"
    :error="historyError"
    :private-notes="privateNotes"
    @page="history"
    @retry="history(current)"
  />
</template>
