<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { progressApi } from "../progress";
import type { StudentOverview } from "../progress";
import { moodChoices, weekDate } from "../../mood/presentation";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  rows = ref<StudentOverview[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(n = 1) {
  const active = ++sequence;
  current.value = n;
  loading.value = true;
  error.value = "";
  try {
    const r = await progressApi.overview(n, keyword.value);
    if(active !== sequence) return;
    rows.value = r.rows;
    total.value = r.total;
  } catch (e) {
    if(active === sequence) error.value = (e as Error).message;
  } finally {
    if(active === sequence) loading.value = false;
  }
}
onMounted(() => load());
</script>
<template>
  <section>
    <h1>{{ t("progress.overview") }}</h1>
    <p>{{ t("progress.overviewHelp") }}</p>
    <form class="search-bar" @submit.prevent="load()">
      <label class="sr-only" for="overview-search">{{
        t("progress.search")
      }}</label><input
        id="overview-search"
        v-model="keyword"
        maxlength="100"
        :placeholder="t('progress.search')"
      /><el-button native-type="submit">
        {{ t("search") }}
      </el-button><el-button
        @click="
          keyword = '';
          load();
        "
      >
        {{ t("clear") }}
      </el-button>
    </form>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("progress.student") }}</th>
              <th>{{ t("progress.course") }}</th>
              <th>{{ t("progress.learning") }}</th>
              <th>{{ t("progress.assignments") }}</th>
              <th>{{ t("progress.mood") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.courseId + ':' + r.studentId">
              <td>
                {{ r.studentName }}<br /><small>{{ r.username }}</small>
              </td>
              <td>{{ r.courseTitle }}</td>
              <td>
                {{
                  t("progress.count", {
                    done: r.completedChapters,
                    total: r.totalChapters,
                  })
                }}<br />{{
                  r.totalChapters
                    ? Math.round((100 * r.completedChapters) / r.totalChapters)
                    : 0
                }}%
              </td>
              <td>
                <p>
                  {{ t("progress.workCount", { count: r.assignmentCount }) }}
                </p>
                <p>{{ t("progress.pending", { count: r.submittedCount }) }}</p>
                <p>{{ t("progress.graded", { count: r.gradedCount }) }}</p>
                <p>{{ t("progress.returned", { count: r.returnedCount }) }}</p>
                <p>
                  {{
                    t("progress.remaining", {
                      count: Math.max(
                        0,
                        r.assignmentCount -
                          r.submittedCount -
                          r.gradedCount -
                          r.returnedCount,
                      ),
                    })
                  }}
                </p>
                <RouterLink
                  :to="{
                    path: '/mentor/assignments',
                    query: { courseId: r.courseId },
                  }"
                >
                  {{ t("progress.viewWork") }}
                </RouterLink>
              </td>
              <td>
                <template v-if="r.moodLevel">
                  <p>
                    {{
                      moodChoices.find((m) => m.level === r.moodLevel)?.emoji
                    }}
                    {{ t("mood.level" + r.moodLevel) }}
                  </p>
                  <p>{{ t('mood.week', {date: weekDate(r.moodWeek!)}) }}</p>
                </template>
                <p v-else>
                  {{ t("progress.noMood") }}
                </p>
                <RouterLink :to="'/mentor/mood/' + r.studentId">
                  {{
                    t("progress.viewMood")
                  }}
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!rows.length">
          {{ t("progress.noStudents") }}
        </p>
      </div>
      <PaginationBar
        v-if="total"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
  </section>
</template>
