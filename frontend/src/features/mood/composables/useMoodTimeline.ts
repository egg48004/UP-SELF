import { ref, watch } from "vue";
import type { Ref } from "vue";
import { moodApi } from "../api";
import type { MoodEntry, MoodPoint, MoodTrend } from "../types";
export function useMoodTimeline(
  enabled: Ref<boolean>,
  studentId: Ref<number | undefined>,
) {
  const rows = ref<(MoodEntry | MoodPoint)[]>([]),
    trend = ref<MoodTrend | null>(null),
    current = ref(1),
    total = ref(0),
    weeks = ref(12),
    historyLoading = ref(false),
    trendLoading = ref(false),
    historyError = ref(""),
    trendError = ref("");
  let h = 0,
    g = 0;
  async function history(page = current.value) {
    if (!enabled.value) return;
    current.value = page;
    const seq = ++h;
    historyLoading.value = true;
    historyError.value = "";
    try {
      const r = await moodApi.history(page, studentId.value);
      if (seq === h) {
        rows.value = r.rows;
        total.value = r.total;
      }
    } catch (e) {
      if (seq === h) historyError.value = (e as Error).message;
    } finally {
      if (seq === h) historyLoading.value = false;
    }
  }
  async function chart() {
    if (!enabled.value) return;
    const seq = ++g;
    trendLoading.value = true;
    trendError.value = "";
    try {
      const r = await moodApi.trend(weeks.value, studentId.value);
      if (seq === g) trend.value = r;
    } catch (e) {
      if (seq === g) trendError.value = (e as Error).message;
    } finally {
      if (seq === g) trendLoading.value = false;
    }
  }
  function reload() {
    return Promise.all([history(1), chart()]);
  }
  watch(
    [enabled, studentId],
    () => {
      h++;
      g++;
      rows.value = [];
      trend.value = null;
      total.value = 0;
      current.value = 1;
      historyLoading.value = false;
      trendLoading.value = false;
      historyError.value = "";
      trendError.value = "";
      if (enabled.value) void reload();
    },
    { immediate: true },
  );
  watch(weeks, chart);
  return {
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
  };
}
