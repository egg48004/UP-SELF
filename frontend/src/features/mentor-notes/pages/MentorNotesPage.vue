<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, onBeforeRouteLeave, onBeforeRouteUpdate } from "vue-router";
import { useI18n } from "vue-i18n";
import { noteApi } from "../api";
import type { MentorNote, NoteStudent } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { learningDate } from "../../learning/progress";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  route = useRoute(),
  sid = computed(() => Number(route.params.studentId));
const student = ref<NoteStudent>(),
  rows = ref<MentorNote[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
const open = ref(false),
  editing = ref<MentorNote>(),
  text = ref(""),
  original = ref(""),
  saving = ref(false),
  actionError = ref(""),
  notice = ref("");
let sequence = 0;
async function load(page = 1) {
  const active = ++sequence, target = sid.value;
  loading.value = true;
  error.value = "";
  try {
    const [person, r] = await Promise.all([noteApi.student(target), noteApi.list(target, page)]);
    if (active !== sequence) return;
    student.value = person;
    rows.value = r.rows;
    total.value = r.total;
    current.value = page;
  } catch (e) {
    if (active !== sequence) return;
    error.value = (e as Error).message;
    rows.value = [];
    student.value = undefined;
    open.value = false;
    text.value = "";
  } finally {
    if (active === sequence) loading.value = false;
  }
}
function edit(note?: MentorNote) {
  editing.value = note;
  text.value = note?.content || "";
  original.value = text.value;
  actionError.value = "";
  open.value = true;
  notice.value = "";
}
function dirty() {
  return open.value && text.value !== original.value;
}
async function close() {
  if (
    !saving.value &&
    (!dirty() || (await confirmAction(t("mentorNotes.leave"))))
  )
    open.value = false;
}
async function save() {
  if (saving.value) return;
  actionError.value = "";
  if (!text.value.trim()) {
    actionError.value = t("mentorNotes.required");
    document.getElementById("note-content")?.focus();
    return;
  }
  saving.value = true;
  try {
    if (editing.value)
      await noteApi.edit(sid.value, editing.value, text.value.trim());
    else await noteApi.create(sid.value, text.value.trim());
    open.value = false;
    notice.value = t("mentorNotes.saved");
    await load(editing.value ? current.value : 1);
  } catch (e) {
    actionError.value = (e as Error).message;
  } finally {
    saving.value = false;
  }
}
const canLeave = async () => !saving.value && (!dirty() || (await confirmAction(t("mentorNotes.leave"))));
onBeforeRouteLeave(canLeave);
onBeforeRouteUpdate(canLeave);
watch(sid, () => {open.value=false; text.value=""; notice.value=""; rows.value=[]; student.value=undefined; load();});
onMounted(() => load());
</script>
<template>
  <section>
    <RouterLink to="/mentor/students">
      ← {{ t("mentorNotes.back") }}
    </RouterLink>
    <h1>{{ t("mentorNotes.title") }}</h1>
    <p v-if="student">
      <strong>{{ student.name }}</strong> · {{ student.username }}
    </p>
    <p>{{ t("mentorNotes.privacy") }}</p>
    <p v-if="notice" class="notice" role="status">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div class="actions">
        <el-button type="primary" :disabled="saving || open" @click="edit()">
          {{
            t("mentorNotes.add")
          }}
        </el-button><el-button :disabled="saving || open" @click="load(current)">
          {{
            t("mentorNotes.refreshed")
          }}
        </el-button>
      </div>
      <section v-if="open" class="panel">
        <h2>{{ t(editing ? "mentorNotes.edit" : "mentorNotes.add") }}</h2>
        <form class="stack" :aria-busy="saving" @submit.prevent="save">
          <label for="note-content">{{ t("mentorNotes.content") }}</label>
          <textarea
            id="note-content"
            v-model="text"
            rows="5"
            maxlength="2000"
            required
            :disabled="saving"
            aria-describedby="note-help"
          ></textarea>
          <p id="note-help" class="muted">
            {{ t("mentorNotes.help") }}
          </p>
          <p v-if="actionError" role="alert" class="notice error">
            {{ actionError }}
          </p>
          <div class="actions">
            <el-button type="primary" native-type="submit" :loading="saving">
              {{
                t("mentorNotes.save")
              }}
            </el-button><el-button :disabled="saving" @click="close">
              {{
                t("cancel")
              }}
            </el-button>
          </div>
        </form>
      </section>
      <p v-if="!rows.length && !open">
        {{ t("mentorNotes.empty") }}
      </p>
      <ul class="note-list">
        <li v-for="note in rows" :key="note.id">
          <article class="panel">
            <p class="note-content">
              {{ note.content }}
            </p>
            <p class="muted">
              {{ t("mentorNotes.created") }}:
              {{ learningDate(note.createTime) }} ·
              {{ t("mentorNotes.updated") }}:
              {{ learningDate(note.updateTime) }}
            </p>
            <el-button
              :disabled="saving || open"
              :aria-label="
                t('mentorNotes.edit') + ' · ' + learningDate(note.createTime)
              "
              @click="edit(note)"
            >
              {{ t("mentorNotes.edit") }}
            </el-button>
          </article>
        </li>
      </ul>
      <PaginationBar
        v-if="total > 10"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
  </section>
</template>
<style scoped>
.note-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.note-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
