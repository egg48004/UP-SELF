<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { moodApi } from "../api";
import type { MoodPerson } from "../types";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n(),
  rows = ref<MoodPerson[]>([]),
  current = ref(1),
  total = ref(0),
  keyword = ref(""),
  loading = ref(false),
  error = ref("");
let seq = 0;
async function load() {
  const n = ++seq;
  loading.value = true;
  error.value = "";
  try {
    const r = await moodApi.students(current.value, keyword.value);
    if (n === seq) {
      rows.value = r.rows;
      total.value = r.total;
    }
  } catch (e) {
    if (n === seq) error.value = (e as Error).message;
  } finally {
    if (n === seq) loading.value = false;
  }
}
function search() {
  current.value = 1;
  void load();
}
onMounted(load);
</script>
<template>
  <section>
    <p class="eyebrow">
      {{ t("dashboard.mentor") }}
    </p>
    <h1>{{ t("efficacy.studentCheckIns") }}</h1>
    <p>{{ t("efficacy.mentorIntro") }}</p>
    <form class="search-bar" @submit.prevent="search">
      <label class="sr-only" for="mood-student-search">{{ t("search") }}</label><input
        id="mood-student-search"
        v-model="keyword"
        maxlength="100"
        :placeholder="t('search')"
      /><el-button native-type="submit">
        {{ t("search") }}
      </el-button><el-button
        :disabled="!keyword"
        @click="
          keyword = '';
          search();
        "
      >
        {{ t("clear") }}
      </el-button>
    </form>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <p v-if="!rows.length" class="panel">
        {{ t("mood.noStudents") }}
      </p>
      <div v-else class="panel table-scroll">
        <table>
          <thead>
            <tr>
              <th scope="col">
                {{ t("mood.student") }}
              </th>
              <th scope="col">
                {{ t("actions") }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="person in rows" :key="person.id">
              <td>
                {{ person.displayName }} <small>({{ person.username }})</small>
              </td>
              <td>
                <RouterLink
                  :to="'/mentor/mood/' + person.id"
                  :aria-label="t('efficacy.openCheckIns') + ': ' + person.username"
                >
                  {{ t("efficacy.openCheckIns") }} →
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :current="current"
        :total="total"
        @change="
          (n) => {
            current = n;
            load();
          }
        "
      />
    </AsyncState>
  </section>
</template>
