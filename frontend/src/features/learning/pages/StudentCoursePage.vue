<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Course, Chapter, Post } from "../types";
import CourseReader from "../components/CourseReader.vue";
import PostList from "../components/PostList.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(),
  { t } = useI18n(),
  course = ref<Course | null>(null),
  chapters = ref<Chapter[]>([]),
  posts = ref<Post[]>([]),
  index = ref(0),
  loading = ref(false),
  error = ref(""),
  tab = ref<"chapters" | "posts">("chapters");
const chapter = computed(() => chapters.value[index.value]);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const id = Number(route.params.id);
    const [c, ch, p] = await Promise.all([
      learningApi.course(id),
      learningApi.chapters(id),
      learningApi.posts(id),
    ]);
    course.value = c;
    chapters.value = ch;
    posts.value = p;
    index.value = 0;
  } catch (e) {
    course.value = null;
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function choose(n: number) {
  index.value = n;
  tab.value = "chapters";
}
watch(() => route.params.id, load, { immediate: true });
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/student/courses">
      ← {{ t("backCourses") }}
    </RouterLink><AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="course">
        <div class="course-banner">
          <span aria-hidden="true">🌱</span>
          <div>
            <p class="eyebrow">
              {{ t("myCourses") }}
            </p>
            <h1>{{ course.title }}</h1>
            <p>{{ course.description }}</p>
          </div>
        </div>
        <div class="learning-tabs">
          <el-button
            :type="tab === 'chapters' ? 'primary' : 'default'"
            :aria-pressed="tab === 'chapters'"
            @click="tab = 'chapters'"
          >
            <span aria-hidden="true">📖 </span>{{ t("chapters") }}
          </el-button><el-button
            :type="tab === 'posts' ? 'primary' : 'default'"
            :aria-pressed="tab === 'posts'"
            @click="tab = 'posts'"
          >
            <span aria-hidden="true">📣 </span>{{ t("posts") }}
          </el-button>
        </div>
        <div v-if="tab === 'chapters'" class="learning-workspace">
          <nav class="chapter-nav panel" :aria-label="t('chapters')">
            <h2>{{ t("chapters") }}</h2>
            <ol>
              <li v-for="(ch, n) in chapters" :key="ch.id">
                <button
                  :class="{ selected: index === n }"
                  :aria-current="index === n ? 'step' : undefined"
                  @click="choose(n)"
                >
                  <span class="chapter-number">{{ n + 1 }}</span>{{ ch.title }}
                </button>
              </li>
            </ol>
            <p v-if="!chapters.length">
              {{ t("noChapters") }}
            </p>
          </nav>
          <div class="panel reader-panel">
            <CourseReader v-if="chapter" :key="chapter.id" :chapter="chapter" />
            <nav
              v-if="chapters.length"
              class="pagination"
              :aria-label="t('chapters')"
            >
              <el-button :disabled="index === 0" @click="choose(index - 1)">
                ← {{ t("previous") }}
              </el-button><span>{{ index + 1 }} / {{ chapters.length }}</span><el-button
                :disabled="index >= chapters.length - 1"
                @click="choose(index + 1)"
              >
                {{ t("next") }} →
              </el-button>
            </nav>
          </div>
        </div>
        <section v-else class="panel narrow">
          <h2>{{ t("posts") }}</h2>
          <PostList :posts="posts" />
        </section>
      </template>
    </AsyncState>
  </section>
</template>
