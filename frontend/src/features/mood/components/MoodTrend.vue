<script setup lang="ts">
import { computed, useId } from "vue";
import { useI18n } from "vue-i18n";
import type { MoodTrend } from "../types";
import { consecutiveRuns, weekDate } from "../presentation";
const props = defineProps<{ trend: MoodTrend }>(),
  { t } = useI18n(),
  uid = useId();
const mode = defineModel<"chart" | "table">("mode", { default: "chart" });
const x = (index: number) =>
    125 + (index * 640) / Math.max(1, props.trend.points.length - 1),
  y = (level: number) => 220 - (level - 1) * 45;
const runs = computed(() => consecutiveRuns(props.trend.points));
const hasRecords = computed(() => runs.value.length > 0);
const ticks = computed(() => [
  0,
  Math.floor((props.trend.points.length - 1) / 2),
  props.trend.points.length - 1,
]);
</script>
<template>
  <div>
    <div class="button-row mood-view-switch">
      <el-button :aria-pressed="mode === 'chart'" @click="mode = 'chart'">
        {{ t("mood.chart") }}
      </el-button><el-button :aria-pressed="mode === 'table'" @click="mode = 'table'">
        {{ t("mood.table") }}
      </el-button>
    </div>
    <p :id="uid + '-help'" class="muted">
      {{ t("mood.gapHelp") }}
    </p>
    <p v-if="!hasRecords">
      {{ t("mood.noTrend") }}
    </p>
    <div
      v-if="mode === 'chart'"
      class="mood-chart-scroll"
      tabindex="0"
      :aria-label="t('mood.chartName')"
    >
      <svg
        class="mood-chart"
        viewBox="0 0 800 280"
        role="img"
        :aria-labelledby="uid + '-title ' + uid + '-desc'"
      >
        <title :id="uid + '-title'">{{ t("mood.chartName") }}</title>
        <desc :id="uid + '-desc'">{{ t("mood.chartDescription") }}</desc>
        <g v-for="level in [1, 2, 3, 4, 5]" :key="level">
          <line
            x1="125"
            x2="765"
            :y1="y(level)"
            :y2="y(level)"
            class="mood-grid-line"
          />
          <text x="114" :y="y(level) + 5" text-anchor="end" class="mood-axis">
            {{ t("mood.level" + level) }}
          </text>
        </g>
        <polyline
          v-for="(run, i) in runs.filter((r) => r.length > 1)"
          :key="i"
          :points="run.map((p) => x(p.index) + ',' + y(p.level)).join(' ')"
          class="mood-trend-line"
        />
        <g v-for="run in runs" :key="run[0]!.index">
          <circle
            v-for="p in run"
            :key="p.index"
            :cx="x(p.index)"
            :cy="y(p.level)"
            r="5"
            class="mood-trend-point"
          >
            <title>
              {{
                t("mood.recordLabel", {
                  week: weekDate(trend.points[p.index]!.weekStart),
                  mood: t("mood.level" + p.level),
                })
              }}
            </title>
          </circle>
        </g>
        <text
          v-for="i in ticks"
          :key="i"
          :x="x(i)"
          y="258"
          :text-anchor="
            i === 0 ? 'start' : i === trend.points.length - 1 ? 'end' : 'middle'
          "
          class="mood-axis"
        >
          {{ weekDate(trend.points[i]!.weekStart) }}
        </text>
      </svg>
    </div>
    <div v-else class="table-scroll">
      <table>
        <caption class="sr-only">
          {{
            t("mood.chartName")
          }}
        </caption>
        <thead>
          <tr>
            <th scope="col">
              {{ t("mood.thisWeek") }}
            </th>
            <th scope="col">
              {{ t("mood.mood") }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="point in trend.points" :key="point.weekStart">
            <th scope="row">
              {{ weekDate(point.weekStart) }}
            </th>
            <td>
              {{
                point.moodLevel === null
                  ? t("mood.noRecord")
                  : t("mood.level" + point.moodLevel)
              }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
