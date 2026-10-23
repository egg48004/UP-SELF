<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { bookmarkApi } from "../bookmarks";
import type { SavedCourse } from "../bookmarks";
import CourseCard from "../components/CourseCard.vue";
import BookmarkButton from "../components/BookmarkButton.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";

const { t } = useI18n(),
  rows = ref<SavedCourse[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  notice = ref("");
let sequence = 0;
async function load(page = 1) {
  const active = ++sequence;
  current.value = page;
  loading.value = true;
  error.value = "";
  try {
    const result = await bookmarkApi.list(page);
    if (active !== sequence) return;
    const last = Math.max(1, Math.ceil(result.total / 10));
    if (page > last) return await load(last);
    rows.value = result.rows;
    total.value = result.total;
  } catch (e) {
    if (active === sequence) error.value = (e as Error).message;
  } finally {
    if (active === sequence) loading.value = false;
  }
}
async function changed(saved: boolean) {
  if (saved) return;
  notice.value = t("bookmark.removed");
  await load(current.value);
}
onMounted(() => load());
</script>
<template>
  <section>
    <h1><span aria-hidden="true">⭐ </span>{{ t("bookmark.title") }}</h1>
    <p>{{ t("bookmark.intro") }}</p>
    <div class="actions">
      <el-button :disabled="loading" @click="load(current)">
        {{ t("bookmark.refresh") }}
      </el-button>
    </div>
    <p v-if="notice" role="status" aria-live="polite">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div v-if="rows.length" class="course-grid saved-grid">
        <template v-for="(entry, index) in rows" :key="entry.courseId">
          <CourseCard v-if="entry.course" :course="entry.course" :index="index" @bookmark-changed="changed" />
          <article v-else class="panel" :aria-labelledby="'unavailable-course-' + entry.courseId">
            <h2 :id="'unavailable-course-' + entry.courseId">
              {{ t("bookmark.unavailable") }}
            </h2>
            <p>{{ t("bookmark.unavailableHelp") }}</p>
            <BookmarkButton :course-id="entry.courseId" :saved="true" @changed="changed" />
          </article>
        </template>
      </div>
      <div v-else class="empty panel">
        <span aria-hidden="true">⭐</span>
        <h2>{{ t("bookmark.empty") }}</h2>
        <p>{{ t("bookmark.emptyHelp") }}</p>
      </div>
      <PaginationBar v-if="total > 10" :current="current" :total="total" @change="load" />
    </AsyncState>
  </section>
</template>
<style scoped>
.saved-grid { margin-top: 1.5rem; }
</style>
