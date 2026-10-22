<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Course } from "../types";
import CourseCard from "../components/CourseCard.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  rows = ref<Course[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load() {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const r = await learningApi.courses({
      current: current.value,
      pageSize: 10,
      keyword: keyword.value,
    });
    if (seq === sequence) {
      rows.value = r.rows;
      total.value = r.total;
    }
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
function search() {
  current.value = 1;
  void load();
}
function clear() {
  keyword.value = "";
  search();
}
function page(n: number) {
  current.value = n;
  void load();
}
onMounted(load);
</script>
<template>
  <section>
    <div class="student-heading">
      <p class="eyebrow">
        <span aria-hidden="true">🌤️ </span>{{ t("tagline") }}
      </p>
      <h1>{{ t("myCourses") }}</h1>
    </div>
    <form class="search-bar" @submit.prevent="search">
      <label class="sr-only" for="course-search">{{ t("search") }}</label><input
        id="course-search"
        v-model="keyword"
        :placeholder="t('search')"
      /><el-button type="primary" native-type="submit">
        {{
          t("search")
        }}
      </el-button><el-button :disabled="!keyword" @click="clear">
        {{
          t("clear")
        }}
      </el-button>
    </form>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <div v-if="rows.length" class="course-grid">
        <CourseCard
          v-for="(c, index) in rows"
          :key="c.id"
          :course="c"
          :index="index"
        />
      </div>
      <div v-else class="empty panel">
        <span aria-hidden="true">🌱</span>
        <h2>{{ t(keyword ? "noResults" : "noCourses") }}</h2>
        <p v-if="!keyword">
          {{ t("noCoursesHelp") }}
        </p>
      </div>
      <PaginationBar
        v-if="total > 0"
        :current="current"
        :total="total"
        @change="page"
      />
    </AsyncState>
  </section>
</template>
