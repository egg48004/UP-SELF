<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { statisticsApi } from "../statistics";
import type { StatisticsTotals, StatisticsCourse } from "../statistics";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  summary = ref<StatisticsTotals>(),
  rows = ref<StatisticsCourse[]>([]),
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
    const [s, r] = await Promise.all([
      statisticsApi.totals(),
      statisticsApi.courses(page),
    ]);
    if (seq !== sequence) return;
    const last = Math.max(1, Math.ceil(r.total / 10));
    if (page > last) return await load(last);
    summary.value = s;
    rows.value = r.rows;
    total.value = r.total;
    current.value = page;
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
const statuses = ["notSubmitted", "submitted", "graded", "returned"] as const;
function count(key: (typeof statuses)[number]) {
  return summary.value?.[`${key}Count`] ?? 0;
}
onMounted(() => load());
</script>
<template>
  <section>
    <h1><span aria-hidden="true">📊 </span>{{ t("stats.title") }}</h1>
    <p>{{ t("stats.intro") }}</p>
    <el-button :disabled="loading" @click="load(current)">
      {{
        t("stats.refresh")
      }}
    </el-button>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <template v-if="summary">
        <div v-if="!summary.courseCount" class="panel empty">
          <h2>{{ t("stats.empty") }}</h2>
          <p>{{ t("stats.emptyHelp") }}</p>
        </div>
        <template v-else>
          <dl class="stats-metrics">
            <div class="panel">
              <dt>{{ t("stats.courses") }}</dt>
              <dd>{{ summary.courseCount }}</dd>
            </div>
            <div class="panel">
              <dt>{{ t("stats.chapters") }}</dt>
              <dd>
                {{ summary.completedChapters }} / {{ summary.totalChapters }}
              </dd>
            </div>
            <div class="panel">
              <dt>{{ t("stats.assignments") }}</dt>
              <dd>{{ summary.assignmentCount }}</dd>
            </div>
          </dl>
          <section class="panel">
            <h2>{{ t("stats.chapterProgress") }}</h2>
            <p v-if="!summary.totalChapters">
              {{ t("stats.noChapters") }}
            </p>
            <template v-else>
              <p>
                {{
                  t("stats.completed", {
                    done: summary.completedChapters,
                    total: summary.totalChapters,
                  })
                }}
                · {{ t("stats.percent", { percent: summary.percent }) }}
              </p>
              <progress
                :value="summary.percent"
                max="100"
                :aria-label="t('stats.chapterProgress')"
              />
            </template>
            <p>{{ t("stats.progressHelp") }}</p>
          </section>
          <section class="panel">
            <h2>{{ t("stats.assignmentStatus") }}</h2>
            <p v-if="!summary.assignmentCount">
              {{ t("stats.noAssignments") }}
            </p>
            <dl v-else class="stats-statuses">
              <div v-for="s in statuses" :key="s">
                <dt>{{ t("stats." + s) }}</dt>
                <dd>{{ count(s) }}</dd>
              </div>
            </dl>
          </section>
          <section>
            <h2>{{ t("stats.byCourse") }}</h2>
            <p>{{ t("stats.scope") }}</p>
            <ul class="stats-courses">
              <li v-for="c in rows" :key="c.courseId" class="panel">
                <h3>
                  <RouterLink :to="'/student/courses/' + c.courseId">
                    {{
                      c.courseTitle
                    }}
                  </RouterLink>
                </h3>
                <template v-if="c.totalChapters">
                  <p>
                    {{
                      t("stats.completed", {
                        done: c.completedChapters,
                        total: c.totalChapters,
                      })
                    }}
                    · {{ t("stats.percent", { percent: c.percent }) }}
                  </p>
                  <progress
                    :value="c.percent"
                    max="100"
                    :aria-label="t('stats.bar', { name: c.courseTitle })"
                  />
                </template>
                <p v-else>
                  {{ t("stats.noChapters") }}
                </p>
                <p>
                  {{
                    t("stats.courseAssignments", { count: c.assignmentCount })
                  }}
                </p>
              </li>
            </ul>
            <PaginationBar
              v-if="total > 10"
              :current="current"
              :total="total"
              @change="load"
            />
          </section>
        </template>
      </template>
    </AsyncState>
  </section>
</template>
<style scoped>
.stats-metrics,
.stats-statuses {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 13rem), 1fr));
  gap: 1rem;
}
.stats-metrics dd,
.stats-statuses dd {
  margin: 0.4rem 0 0;
  font-size: 1.5rem;
  font-weight: 700;
}
.stats-courses {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.stats-courses li {
  overflow-wrap: anywhere;
}
progress {
  display: block;
  width: 100%;
  height: 1rem;
  accent-color: var(--primary);
}
section.panel {
  margin-block: 1.5rem;
}
</style>
