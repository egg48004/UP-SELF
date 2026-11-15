<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { marketApi } from "../api";
import type { MarketTask } from "../types";
import MarketTaskEditor from "../components/MarketTaskEditor.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  { t } = useI18n();
const courseId = computed(() => Number(route.query.courseId) || undefined),
  rows = ref<MarketTask[]>([]),
  current = ref(1),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  editing = ref(false);
let sequence = 0;
const base = computed(() =>
  auth.isStudent ? "/student/market" : "/mentor/market",
);
async function load() {
  const n = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const r = await marketApi.list(courseId.value, current.value);
    if (n === sequence) {
      rows.value = r.rows;
      total.value = r.total;
    }
  } catch (e) {
    if (n === sequence) error.value = (e as Error).message;
  } finally {
    if (n === sequence) busy.value = false;
  }
}
watch(
  courseId,
  () => {
    current.value = 1;
    void load();
  },
  { immediate: true },
);
onBeforeUnmount(() => sequence++);
async function saved(id: number) {
  editing.value = false;
  await router.push(base.value + "/" + id);
}
</script>
<template>
  <section>
    <RouterLink
      v-if="courseId"
      class="back-link"
      :to="
        (auth.isStudent ? '/student/courses/' : '/manage/courses/') + courseId
      "
    >
      ← {{ t("market.backCourse") }}
    </RouterLink>
    <RouterLink
      v-else
      class="back-link"
      :to="auth.isStudent ? '/student/business' : '/manage/courses'"
    >
      ← {{ t("market.backCourse") }}
    </RouterLink>
    <div class="page-heading">
      <h1>{{ t("market.title") }}</h1>
      <el-button
        v-if="auth.isMentor"
        :disabled="!courseId || busy"
        @click="editing = true"
      >
        {{ t("market.new") }}
      </el-button>
    </div>
    <p>{{ t("market.intro") }}</p>
    <el-button :disabled="busy" @click="load">
      {{
        t("market.refresh")
      }}
    </el-button>
    <AsyncState :loading="busy" :error="error" @retry="load">
      <p v-if="!rows.length">
        {{ t(auth.isStudent ? "market.emptyStudent" : "market.empty") }}
      </p>
      <ul class="market-task-list">
        <li v-for="task in rows" :key="task.id" class="panel">
          <p class="eyebrow">
            {{ task.courseTitle }}
          </p>
          <h2>{{ task.title }}</h2>
          <p>{{ task.objective }}</p>
          <p class="status">
            {{ t("market." + task.status.toLowerCase()) }}
          </p>
          <p v-if="auth.isStudent && task.attemptCount">
            ✓ {{ t("market.completed") }}
          </p>
          <RouterLink class="button-link" :to="base + '/' + task.id">
            {{
              t("market.open")
            }}
          </RouterLink>
        </li>
      </ul>
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
    </AsyncState><MarketTaskEditor
      v-if="courseId"
      v-model="editing"
      :course-id="courseId"
      @saved="saved"
    />
  </section>
</template>
<style scoped>
.market-task-list {
  list-style: none;
  padding: 0;
}
.market-task-list li {
  overflow-wrap: anywhere;
}
</style>
