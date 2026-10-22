<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { learningApi } from "../api";
import type { Course } from "../types";
import CourseForm from "../components/CourseForm.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter(),
  rows = ref<Course[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  creating = ref(false);
let seq = 0;
async function load() {
  const n = ++seq;
  loading.value = true;
  error.value = "";
  try {
    const r = await learningApi.courses({
      current: current.value,
      pageSize: 10,
      keyword: keyword.value,
    });
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
function page(n: number) {
  current.value = n;
  void load();
}
async function saved(id: number) {
  creating.value = false;
  await router.push("/manage/courses/" + id);
}
onMounted(load);
</script>
<template>
  <section>
    <div class="page-heading">
      <h1>{{ t("courses") }}</h1>
      <el-button
        v-if="auth.can('learning:course:add')"
        type="primary"
        @click="creating = true"
      >
        {{ t("createCourse") }}
      </el-button>
    </div>
    <form class="search-bar" @submit.prevent="search">
      <label class="sr-only" for="manage-search">{{ t("search") }}</label><input
        id="manage-search"
        v-model="keyword"
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
      <div class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("courseTitle") }}</th>
              <th>{{ t("mentor") }}</th>
              <th>{{ t("status") }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in rows" :key="c.id">
              <td>{{ c.title }}</td>
              <td>{{ c.mentorName || "—" }}</td>
              <td>
                <span :class="['status', c.status.toLowerCase()]">{{
                  t(c.status.toLowerCase())
                }}</span>
              </td>
              <td>
                <RouterLink :to="'/manage/courses/' + c.id">
                  {{
                    t("openCourse")
                  }}
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!rows.length">
          {{ t(keyword ? "noResults" : "noCourses") }}
        </p>
      </div>
      <PaginationBar
        :current="current"
        :total="total"
        @change="page"
      />
    </AsyncState><el-dialog
      v-model="creating"
      :title="t('createCourse')"
      width="min(580px,95vw)"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <CourseForm v-if="creating" @saved="saved" @cancel="creating = false" />
    </el-dialog>
  </section>
</template>
