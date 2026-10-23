<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { learningApi } from "../api";
import type { Course, Chapter, Post, Member, State } from "../types";
import type { Candidate } from "../../accounts/api";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
import CourseFacts from "../components/CourseFacts.vue";
import CourseForm from "../components/CourseForm.vue";
import CourseMemberPicker from "../components/CourseMemberPicker.vue";
import PostEditor from "../components/PostEditor.vue";
import PostList from "../components/PostList.vue";
import CourseReader from "../components/CourseReader.vue";
const { t } = useI18n(),
  auth = useAuthStore(),
  route = useRoute(),
  router = useRouter(),
  id = computed(() => Number(route.params.id)),
  course = ref<Course | null>(null),
  chapters = ref<Chapter[]>([]),
  posts = ref<Post[]>([]),
  members = ref<Member[]>([]),
  loading = ref(false),
  error = ref(""),
  busy = ref(false),
  editing = ref(false),
  picker = ref<"mentor" | "student" | null>(null),
  postDialog = ref(false),
  post = ref<Post>(),
  preview = ref<Chapter>(),
  newChapter = ref("");
const editable = computed(() => course.value?.status !== "ARCHIVED");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [c, ch, p, m] = await Promise.all([
      learningApi.course(id.value),
      learningApi.chapters(id.value),
      learningApi.posts(id.value),
      learningApi.members(id.value),
    ]);
    course.value = c;
    chapters.value = ch;
    posts.value = p;
    members.value = m;
  } catch (e) {
    course.value = null;
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function action(fn: () => Promise<unknown>) {
  error.value = "";
  busy.value = true;
  try {
    await fn();
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function state(s: State) {
  if (s === "ARCHIVED" && !(await confirmAction(t("confirmArchive")))) return;
  await action(() => learningApi.state(id.value, s));
}
async function selected(u: Candidate) {
  await action(async () => {
    if (picker.value === "mentor") await learningApi.assign(id.value, u.id);
    else await learningApi.enroll(id.value, u.id);
    picker.value = null;
  });
}
async function removeStudent(m: Member) {
  if (await confirmAction(t("confirmRemove")))
    await action(() => learningApi.unenroll(id.value, m.studentId));
}
async function createChapter() {
  error.value = "";
  busy.value = true;
  try {
    const cid = await learningApi.createChapter(id.value, {
      title: newChapter.value,
      body: "",
      sortOrder: chapters.value.length,
      status: "DRAFT",
      languageCode: "en",
    });
    newChapter.value = "";
    await router.push("/manage/courses/" + id.value + "/chapters/" + cid);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function deleteChapter(ch: Chapter) {
  if (await confirmAction(t("confirmDelete")))
    await action(() => learningApi.deleteChapter(ch.id));
}
function editPost(p?: Post) {
  post.value = p;
  postDialog.value = true;
}
async function postSaved() {
  postDialog.value = false;
  await load();
}
async function offline(p: Post) {
  if (await confirmAction(t("confirmOffline")))
    await action(() => learningApi.offline(p.id));
}
async function deletePost(p: Post) {
  if (await confirmAction(t("confirmDelete")))
    await action(() => learningApi.deletePost(p.id));
}
watch(id, load, { immediate: true });
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/manage/courses">
      ← {{ t("backCourses") }}
    </RouterLink>
    <p v-if="error && course" class="notice error" role="alert">
      {{ error }}
    </p>
    <AsyncState :loading="loading" :error="course ? '' : error" @retry="load">
      <template v-if="course">
        <div class="page-heading">
          <div>
            <p class="eyebrow">
              {{ t("courseManage") }}
            </p>
            <h1>{{ course.title }}</h1>
            <RouterLink
              v-if="auth.isMentor"
              class="button-link"
              :to="{
                path: '/mentor/assignments',
                query: { courseId: course.id },
              }"
            >
              {{ t("assignment.teaching") }} →
            </RouterLink>
            <span :class="['status', course.status.toLowerCase()]">{{
              t(course.status.toLowerCase())
            }}</span>
          </div>
          <el-button
            v-if="auth.can('learning:course:edit')"
            @click="editing = true"
          >
            {{ t("edit") }}
          </el-button>
        </div>
        <p class="plain-text">
          {{ course.description }}
        </p>
        <CourseFacts :course="course" />
        <p v-if="!editable" class="notice">
          {{ t("archivedHelp") }}
        </p>
        <section class="panel">
          <div class="page-heading">
            <h2>{{ t("mentor") }}: {{ course.mentorName || "—" }}</h2>
            <el-button
              v-if="auth.can('learning:course:assign')"
              :disabled="busy"
              @click="picker = 'mentor'"
            >
              {{ t("assignMentor") }}
            </el-button>
          </div>
          <div v-if="auth.can('learning:course:publish')" class="actions">
            <el-button
              v-if="course.status !== 'PUBLISHED'"
              type="primary"
              :disabled="!course.mentorId || busy"
              @click="state('PUBLISHED')"
            >
              {{ t("publish") }}
            </el-button><el-button
              v-if="course.status !== 'DRAFT'"
              :disabled="busy"
              @click="state('DRAFT')"
            >
              {{ t("unpublish") }}
            </el-button><el-button
              v-if="course.status !== 'ARCHIVED'"
              :disabled="busy"
              @click="state('ARCHIVED')"
            >
              {{ t("archive") }}
            </el-button><span v-if="!course.mentorId" class="muted">{{
              t("disabledPublish")
            }}</span>
          </div>
        </section>
        <section class="panel">
          <div class="page-heading">
            <h2>{{ t("chapters") }}</h2>
          </div>
          <form
            v-if="auth.can('learning:chapter:edit') && editable"
            class="search-bar"
            @submit.prevent="createChapter"
          >
            <label class="sr-only" for="new-chapter">{{
              t("chapterTitle")
            }}</label><input
              id="new-chapter"
              v-model="newChapter"
              :placeholder="t('chapterTitle')"
              required
              maxlength="150"
            /><el-button native-type="submit" type="primary" :loading="busy">
              {{ t("createChapter") }}
            </el-button>
          </form>
          <ul class="content-list">
            <li v-for="ch in chapters" :key="ch.id">
              <span>{{ ch.title }}
                <small>{{ t(ch.status.toLowerCase()) }}</small></span>
              <div class="actions">
                <el-button @click="preview = ch">
                  {{ t("preview") }}
                </el-button><RouterLink
                  v-if="auth.can('learning:chapter:edit') && editable"
                  :to="'/manage/courses/' + id + '/chapters/' + ch.id"
                >
                  {{ t("edit") }}
                </RouterLink><el-button
                  v-if="auth.can('learning:chapter:edit') && editable"
                  :disabled="busy"
                  @click="deleteChapter(ch)"
                >
                  {{ t("delete") }}
                </el-button>
              </div>
            </li>
          </ul>
          <p v-if="!chapters.length">
            {{ t("noChapters") }}
          </p>
        </section>
        <section class="panel">
          <div class="page-heading">
            <h2>{{ t("posts") }}</h2>
            <el-button
              v-if="auth.can('learning:post:add') && editable"
              type="primary"
              @click="editPost()"
            >
              {{ t("createPost") }}
            </el-button>
          </div>
          <PostList :posts="posts">
            <template #actions="{ post: p }">
              <div class="actions">
                <span class="status">{{ t(p.status.toLowerCase()) }}</span><el-button
                  v-if="
                    auth.can('learning:post:edit') &&
                      p.createdBy === auth.user?.id &&
                      editable
                  "
                  @click="editPost(p)"
                >
                  {{ t("edit") }}
                </el-button><el-button
                  v-if="
                    auth.can('learning:post:offline') &&
                      p.status === 'PUBLISHED' &&
                      editable &&
                      (p.createdBy === auth.user?.id ||
                        auth.can('learning:course:edit'))
                  "
                  :disabled="busy"
                  @click="offline(p)"
                >
                  {{ t("offline") }}
                </el-button><el-button
                  v-if="
                    auth.can('learning:post:edit') &&
                      p.createdBy === auth.user?.id &&
                      editable
                  "
                  :disabled="busy"
                  @click="deletePost(p)"
                >
                  {{ t("delete") }}
                </el-button>
              </div>
            </template>
          </PostList>
        </section>
        <section class="panel">
          <div class="page-heading">
            <h2>{{ t("members") }} ({{ members.length }})</h2>
            <el-button
              v-if="auth.can('learning:course:assign')"
              :disabled="busy"
              @click="picker = 'student'"
            >
              {{ t("addStudent") }}
            </el-button>
          </div>
          <ul class="content-list">
            <li v-for="m in members" :key="m.studentId">
              <span>{{ m.nickName }} <small>{{ m.userName }}</small></span><el-button
                v-if="auth.can('learning:course:assign')"
                :disabled="busy"
                @click="removeStudent(m)"
              >
                {{ t("remove") }}
              </el-button>
            </li>
          </ul>
        </section>
        <el-dialog
          v-model="editing"
          :title="t('edit')"
          width="min(580px,95vw)"
          destroy-on-close
          :close-on-click-modal="false"
        >
          <CourseForm
            v-if="editing"
            :course="course"
            @saved="
              editing = false;
              load();
            "
            @cancel="editing = false"
          />
        </el-dialog><el-dialog
          :model-value="!!picker"
          :title="t(picker === 'mentor' ? 'assignMentor' : 'addStudent')"
          width="min(650px,95vw)"
          destroy-on-close
          :close-on-click-modal="false"
          @update:model-value="picker = null"
        >
          <CourseMemberPicker
            v-if="picker"
            :role="picker"
            @selected="selected"
          />
        </el-dialog><el-dialog
          v-model="postDialog"
          :title="t(post ? 'edit' : 'createPost')"
          width="min(650px,95vw)"
          destroy-on-close
          :close-on-click-modal="false"
        >
          <PostEditor
            v-if="postDialog"
            :course-id="id"
            :post="post"
            @saved="postSaved"
            @cancel="postDialog = false"
          />
        </el-dialog><el-dialog
          :model-value="!!preview"
          :title="t('preview')"
          width="min(900px,96vw)"
          destroy-on-close
          @update:model-value="preview = undefined"
        >
          <CourseReader v-if="preview" :chapter="preview" />
        </el-dialog>
      </template>
    </AsyncState>
  </section>
</template>
