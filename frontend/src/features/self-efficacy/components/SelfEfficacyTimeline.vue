<script setup lang="ts">
import { ref, useId, watch } from "vue";
import { useI18n } from "vue-i18n";
import { efficacyApi } from "../api";
import type { EfficacyEntry, EfficacyPoint, EfficacyTrend, EfficacyInstrument } from "../types";
import SelfEfficacyTrend from "./SelfEfficacyTrend.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ enabled: boolean; studentId?: number; instrument?: EfficacyInstrument }>();
const { t } = useI18n(), uid = useId(), rows = ref<(EfficacyEntry | EfficacyPoint)[]>([]), trend = ref<EfficacyTrend | null>(null), current = ref(1), total = ref(0), weeks = ref(26), loadingHistory = ref(false), loadingTrend = ref(false), historyError = ref(""), trendError = ref("");
let h = 0, g = 0;
async function history(page = current.value) {
  if (!props.enabled) return;
  const seq = ++h; current.value = page; loadingHistory.value = true; historyError.value = "";
  try { const result = await efficacyApi.history(page, props.studentId); if (seq === h) { rows.value = result.rows; total.value = result.total; } }
  catch (e) { if (seq === h) historyError.value = (e as Error).message; }
  finally { if (seq === h) loadingHistory.value = false; }
}
async function chart() {
  if (!props.enabled) return;
  const seq = ++g; loadingTrend.value = true; trendError.value = "";
  try { const result = await efficacyApi.trend(weeks.value, props.studentId); if (seq === g) trend.value = result; }
  catch (e) { if (seq === g) trendError.value = (e as Error).message; }
  finally { if (seq === g) loadingTrend.value = false; }
}
function reload() { return Promise.all([history(1), chart()]); }
watch(() => [props.enabled, props.studentId], () => {
  h++; g++; rows.value = []; trend.value = null; total.value = 0; current.value = 1;
  loadingHistory.value = false; loadingTrend.value = false; historyError.value = ""; trendError.value = "";
  if (props.enabled) void reload();
}, { immediate: true });
watch(weeks, chart);
defineExpose({ reload });
function ownAnswers(row: EfficacyEntry | EfficacyPoint) {
  return props.studentId === undefined && "answerOne" in row && row.instrumentVersion === props.instrument?.version ? [row.answerOne, row.answerTwo, row.answerThree] : [];
}
</script>
<template>
  <section class="panel">
    <div class="page-heading">
      <h2>{{ t("efficacy.trend") }}</h2>
      <div>
        <label :for="uid + '-weeks'">{{ t("mood.range") }}</label><select :id="uid + '-weeks'" v-model.number="weeks">
          <option v-for="n in [4, 12, 26, 52]" :key="n" :value="n">
            {{ t("mood.weeks" + n) }}
          </option>
        </select>
      </div>
    </div>
    <AsyncState :loading="loadingTrend" :error="trendError" @retry="chart">
      <SelfEfficacyTrend v-if="trend" :trend="trend" />
    </AsyncState>
  </section>
  <section class="panel efficacy-history">
    <h2>{{ t("efficacy.history") }}</h2>
    <AsyncState :loading="loadingHistory" :error="historyError" @retry="history()">
      <p v-if="!rows.length">
        {{ t("efficacy.empty") }}
      </p>
      <ol v-else class="efficacy-history-list">
        <li v-for="row in rows" :key="row.assessmentDate">
          <h3>{{ row.assessmentDate }}</h3><p>{{ t("efficacy.score", { score: Number(row.score).toFixed(2) }) }}</p>
          <details v-if="ownAnswers(row).length">
            <summary>{{ t("efficacy.answers") }}</summary><ol>
              <li v-for="(answer, index) in ownAnswers(row)" :key="index">
                <p>{{ instrument!.questions[index] }}</p><strong>{{ instrument!.responses[answer - 1] }}</strong>
              </li>
            </ol>
          </details>
        </li>
      </ol>
      <PaginationBar v-if="total > 0" :current="current" :total="total" @change="history" />
    </AsyncState>
  </section>
</template>
