<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Course, Chapter, Post } from "../types";
import CourseFacts from "../components/CourseFacts.vue";
import BookmarkButton from "../components/BookmarkButton.vue";
import { progressApi } from "../progress";
import type { LearningProgress } from "../progress";
import CourseReader from "../components/CourseReader.vue";
import PostList from "../components/PostList.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(),
  { t } = useI18n(),
  course = ref<Course | null>(null),
  chapters = ref<Chapter[]>([]),
  posts = ref<Post[]>([]),
  index = ref(0),
  progress = ref<LearningProgress | null>(null),
  saving = ref(false),
  progressError = ref(""),
  notice = ref(""),
  loading = ref(false),
  error = ref(""),
  tab = ref<"chapters" | "posts">("chapters");
const chapter = computed(() => chapters.value[index.value]);
const completed = computed(
  () =>
    !!progress.value?.chapters.find((c) => c.chapterId === chapter.value?.id)
      ?.completedAt,
);
let loadSequence = 0;
async function visited(id: number, sequence: number) {
  try {
    await progressApi.visit(id);
  } catch (e) {
    if (sequence === loadSequence) progressError.value = (e as Error).message;
  }
}
async function mark() {
  if (!chapter.value || saving.value) return;
  const id = Number(route.params.id),
    ch = chapter.value.id,
    sequence = loadSequence;
  saving.value = true;
  progressError.value = "";
  notice.value = "";
  try {
    await progressApi.complete(ch, !completed.value);
    const result = await progressApi.summary(id);
    if (sequence === loadSequence) {
      progress.value = result;
      notice.value = t("progress.saved");
    }
  } catch (e) {
    if (sequence === loadSequence) progressError.value = (e as Error).message;
  } finally {
    saving.value = false;
  }
}
async function load() {
  const sequence = ++loadSequence;
  loading.value = true;
  progressError.value = "";
  notice.value = "";
  error.value = "";
  try {
    const id = Number(route.params.id);
    const [c, ch, p, pr] = await Promise.all([
      learningApi.course(id),
      learningApi.chapters(id),
      learningApi.posts(id),
      progressApi.summary(id),
    ]);
    if (sequence !== loadSequence) return;
    course.value = c;
    progress.value = pr;
    chapters.value = ch;
    posts.value = p;
    const requested = Number(route.query.chapterId) || pr.lastChapterId;
    index.value = Math.max(
      0,
      ch.findIndex((item) => item.id === requested),
    );
    if (ch[index.value]) await visited(ch[index.value]!.id, sequence);
  } catch (e) {
    if (sequence !== loadSequence) return;
    course.value = null;
    error.value = (e as Error).message;
  } finally {
    if (sequence === loadSequence) loading.value = false;
  }
}
async function choose(n: number) {
  if (saving.value || !chapters.value[n]) return;
  index.value = n;
  tab.value = "chapters";
  notice.value = "";
  progressError.value = "";
  saving.value = true;
  try {
    await visited(chapters.value[n]!.id, loadSequence);
  } finally {
    saving.value = false;
  }
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
            <CourseFacts :course="course" />
            <BookmarkButton :course-id="course.id" :saved="course.bookmarked" :title="course.title" @changed="course.bookmarked = $event" />
          </div>
        </div>
        <section
          v-if="progress"
          class="panel learning-progress"
          :aria-label="t('progress.title')"
        >
          <p>
            {{
              t("progress.count", {
                done: progress.completed,
                total: progress.total,
              })
            }}
            · {{ progress.percent }}%
          </p>
          <progress
            :value="progress.completed"
            :max="progress.total || 1"
            :aria-label="t('progress.title')"
          ></progress>
          <RouterLink to="/student/history">
            {{ t("progress.history") }} →
          </RouterLink>
        </section>
        <RouterLink
          class="button-link"
          :to="{ path: '/student/assignments', query: { courseId: course.id } }"
        >
          📝 {{ t("assignment.myWork") }}
        </RouterLink>
        <RouterLink class="button-link" :to="'/student/courses/'+course.id+'/discussions'"><span aria-hidden="true">💬 </span>{{t('chat.title')}}</RouterLink>
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
                  :disabled="saving"
                  @click="choose(n)"
                >
                  <span class="chapter-number">{{ n + 1 }}</span><span class="chapter-label"><span>{{ ch.title }}</span>
                    <span
                      v-if="
                        progress?.chapters.find((p) => p.chapterId === ch.id)
                          ?.completedAt
                      "
                      class="chapter-completed"
                    >✓ {{ t("progress.done") }}</span></span>
                </button>
              </li>
            </ol>
            <p v-if="!chapters.length">
              {{ t("noChapters") }}
            </p>
          </nav>
          <div class="panel reader-panel">
            <CourseReader v-if="chapter" :key="chapter.id" :chapter="chapter" />
            <div v-if="chapter" class="stack completion-controls">
              <p>{{ t("progress.help") }}</p>
              <el-button
                :loading="saving"
                :aria-pressed="completed"
                @click="mark"
              >
                {{
                  t(completed ? "progress.undo" : "progress.mark")
                }}
              </el-button>
              <p v-if="notice" role="status">
                {{ notice }}
              </p>
              <p v-if="progressError" role="alert" class="notice error">
                {{ progressError }}
              </p>
            </div>
            <nav
              v-if="chapters.length"
              class="pagination"
              :aria-label="t('chapters')"
            >
              <el-button
                :disabled="index === 0 || saving"
                @click="choose(index - 1)"
              >
                ← {{ t("previous") }}
              </el-button><span>{{ index + 1 }} / {{ chapters.length }}</span><el-button
                :disabled="index >= chapters.length - 1 || saving"
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
