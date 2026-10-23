<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useId, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { EfficacyTrend } from "../types";
const props = defineProps<{ trend: EfficacyTrend }>(), { t } = useI18n(), uid = useId();
const mode = defineModel<"chart" | "table">("mode", { default: "chart" });
const container = ref<HTMLElement | null>(null), width = ref(720);
let observer: ResizeObserver | undefined;
watch(container, element => {
  observer?.disconnect();
  if (!element) return;
  const resize = () => { if (container.value === element && element.clientWidth) width.value = Math.max(240, element.clientWidth); };
  resize();
  if (typeof ResizeObserver !== "undefined") { observer = new ResizeObserver(resize); observer.observe(element); }
}, { flush: "post" });
onBeforeUnmount(() => observer?.disconnect());
const day = (date: string) => Date.parse(date + "T00:00:00Z");
const x = (date: string) => 42 + (width.value - 66) * (day(date) - day(props.trend.startDate)) / Math.max(86400000, day(props.trend.endDate) - day(props.trend.startDate));
const y = (score: number) => 220 - (score - 1) * 45;
const points = computed(() => props.trend.points.map(point => x(point.assessmentDate) + "," + y(point.score)).join(" "));
</script>
<template>
  <div>
    <div class="actions">
      <el-button :aria-pressed="mode === 'chart'" @click="mode = 'chart'">
        {{ t("mood.chart") }}
      </el-button>
      <el-button :aria-pressed="mode === 'table'" @click="mode = 'table'">
        {{ t("mood.table") }}
      </el-button>
    </div>
    <p class="muted">
      {{ t("efficacy.pointsHelp") }}
    </p>
    <p v-if="!trend.points.length">
      {{ t("efficacy.noPoints") }}
    </p>
    <div v-if="mode === 'chart' && trend.points.length" ref="container" class="efficacy-chart-scroll" tabindex="0" :aria-label="t('efficacy.chartName')">
      <svg class="efficacy-chart" :viewBox="'0 0 ' + width + ' 280'" role="img" :aria-labelledby="uid + '-title ' + uid + '-description'">
        <title :id="uid + '-title'">{{ t("efficacy.chartName") }}</title>
        <desc :id="uid + '-description'">{{ t("efficacy.chartDescription") }}</desc>
        <g v-for="level in [1, 2, 3, 4, 5]" :key="level">
          <line x1="42" :x2="width - 24" :y1="y(level)" :y2="y(level)" class="efficacy-grid-line" />
          <text x="30" :y="y(level) + 5" text-anchor="end" class="efficacy-axis">{{ level }}</text>
        </g>
        <polyline v-if="trend.points.length > 1" :points="points" class="efficacy-trend-line" />
        <circle v-for="point in trend.points" :key="point.assessmentDate" :cx="x(point.assessmentDate)" :cy="y(point.score)" r="5" class="efficacy-trend-point">
          <title>{{ point.assessmentDate }}: {{ Number(point.score).toFixed(2) }} / 5</title>
        </circle>
        <text x="42" y="258" class="efficacy-axis">{{ trend.startDate }}</text>
        <text :x="width - 24" y="258" text-anchor="end" class="efficacy-axis">{{ trend.endDate }}</text>
      </svg>
    </div>
    <div v-else-if="mode === 'table' && trend.points.length" class="table-scroll">
      <table>
        <caption class="sr-only">
          {{ t("efficacy.chartName") }}
        </caption>
        <thead>
          <tr>
            <th scope="col">
              {{ t("efficacy.date") }}
            </th><th scope="col">
              {{ t("efficacy.value") }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="point in trend.points" :key="point.assessmentDate">
            <th scope="row">
              {{ point.assessmentDate }}
            </th><td>{{ Number(point.score).toFixed(2) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
