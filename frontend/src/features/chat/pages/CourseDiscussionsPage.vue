<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { learningApi } from "../../learning/api";
import { createChatApi } from "../api";
import type { ChatGroup } from "../types";
import ChatRoom from "../components/ChatRoom.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const route = useRoute(),
  auth = useAuthStore(),
  { t } = useI18n(),
  api = createChatApi(auth.isStudent),
  courseId = computed(() => Number(route.params.id));
const courseTitle = ref(""),
  groups = ref<ChatGroup[]>([]),
  current = ref(1),
  total = ref(0),
  selected = ref<number | null>(null),
  loading = ref(false),
  error = ref(""),
  creating = ref(false),
  title = ref(""),
  busy = ref(false),
  dirty = ref(false);
let sequence = 0;
const back = computed(() =>
  auth.isStudent
    ? "/student/courses/" + courseId.value
    : "/manage/courses/" + courseId.value,
);
async function load(page = 1) {
  const active = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const [c, g] = await Promise.all([
      learningApi.course(courseId.value),
      api.groups(courseId.value, page),
    ]);
    if (active !== sequence) return;
    courseTitle.value = c.title;
    groups.value = g.rows;
    total.value = g.total;
    current.value = page;
  } catch (e) {
    if (active === sequence) error.value = (e as Error).message;
  } finally {
    if (active === sequence) loading.value = false;
  }
}
function updated(group: ChatGroup) { groups.value = groups.value.map(row => row.id === group.id ? {...row, ...group} : row); }
async function choose(id: number) {
  if (id === selected.value) return;
  if (dirty.value && !(await confirmAction(t("chat.leave")))) return;
  selected.value = id;
  dirty.value = false;
}
async function create() {
  if (!title.value.trim()) return;
  busy.value = true;
  error.value = "";
  try {
    const id = await api.create(courseId.value, title.value.trim());
    creating.value = false;
    title.value = "";
    await load();
    await choose(id);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(
  courseId,
  () => {
    selected.value = null;
    void load();
  },
  { immediate: true },
);
</script>
<template>
  <section>
    <RouterLink :to="back">
      ← {{ t("chat.back") }}
    </RouterLink>
    <h1>{{ t("chat.groups") }}</h1>
    <p>{{ courseTitle }}</p>
    <p>{{ t("chat.help") }}</p>
    <el-button v-if="auth.isMentor" @click="creating = true">
      {{
        t("chat.create")
      }}
    </el-button>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <nav class="panel" aria-label="Discussion groups">
        <ul class="group-list">
          <li v-for="g in groups" :key="g.id">
            <el-button :aria-pressed="selected === g.id" @click="choose(g.id)">
              {{ g.title }} ·
              {{
                t(g.status === "CLOSED" ? "chat.closed" : "chat.active")
              }}
            </el-button>
          </li>
        </ul>
        <p v-if="!groups.length">
          {{ t("chat.empty") }}
        </p>
        <PaginationBar
          :current="current"
          :page-size="10"
          :total="total"
          @change="load"
        />
      </nav>
    </AsyncState>
    <ChatRoom
      v-if="selected"
      :id="selected"
      :key="selected"
      :api="api"
      :manager="auth.isMentor"
      @dirty="dirty = $event"
      @changed="load(current)"
      @updated="updated"
    />
    <el-dialog
      v-model="creating"
      :title="t('chat.newGroup')"
      width="min(500px,95vw)"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <form class="stack" @submit.prevent="create">
        <label for="group-name">{{ t("chat.name") }}</label><input
          id="group-name"
          v-model="title"
          maxlength="100"
          required
          :disabled="busy"
        />
        <div class="button-row">
          <el-button native-type="submit" :loading="busy">
            {{
              t("chat.create")
            }}
          </el-button><el-button :disabled="busy" @click="creating = false">
            {{
              t("chat.cancel")
            }}
          </el-button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>
<style scoped>
.group-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  list-style: none;
  padding: 0;
}
.group-list li {
  min-width: 0;
  max-width: 100%;
}
.group-list :deep(button) {
  height: auto;
  min-height: 2.75rem;
  white-space: normal;
  overflow-wrap: anywhere;
  text-align: left;
}
</style>
