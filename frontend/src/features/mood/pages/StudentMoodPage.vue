<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { useI18n } from "vue-i18n";
import { moodApi } from "../api";
import type { MoodCurrent } from "../types";
import { moodChoices, weekDate } from "../presentation";
import MoodTimeline from "../components/MoodTimeline.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  current = ref<MoodCurrent | null>(null),
  loading = ref(true),
  loadError = ref(""),
  error = ref(""),
  busy = ref(false),
  accepted = ref(false),
  level = ref<number | null>(null),
  privateNote = ref(""),
  notice = ref(""),
  timeline = ref<InstanceType<typeof MoodTimeline> | null>(null);
const dirty = computed(
  () => !current.value?.entry && (level.value !== null || !!privateNote.value),
);
async function guard() {
  if (busy.value) return false;
  return !dirty.value || (await confirmAction(t("mood.leave")));
}
onBeforeRouteLeave(guard);
function beforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    e.preventDefault();
    e.returnValue = "";
  }
}
onMounted(() => window.addEventListener("beforeunload", beforeUnload));
onBeforeUnmount(() => window.removeEventListener("beforeunload", beforeUnload));
async function load() {
  loading.value = true;
  loadError.value = "";
  try {
    current.value = await moodApi.current();
  } catch (e) {
    loadError.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function refresh() {
  if (!(await guard())) return;
  level.value = null;
  privateNote.value = "";
  notice.value = "";
  error.value = "";
  await load();
  await timeline.value?.reload();
}
async function acknowledge() {
  if (!current.value || !accepted.value || busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    current.value = await moodApi.acknowledge(
      current.value.noticeVersion,
      true,
    );
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (!current.value || level.value === null || busy.value) return;
  busy.value = true;
  if (!(await confirmAction(t("mood.saveConfirm")))) {
    busy.value = false;
    return;
  }
  error.value = "";
  try {
    current.value = await moodApi.record(
      current.value.weekStart,
      level.value,
      privateNote.value,
    );
    level.value = null;
    privateNote.value = "";
    notice.value = t("mood.saved");
    await timeline.value?.reload();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="student-mood">
    <div class="course-banner">
      <span aria-hidden="true">🌤️</span>
      <div>
        <p class="eyebrow">
          {{ t("mood.myMood") }}
        </p>
        <h1>{{ t("mood.title") }}</h1>
        <p>{{ t("mood.intro") }}</p>
      </div>
    </div>
    <AsyncState :loading="loading" :error="loadError" @retry="load">
      <template v-if="current">
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
          <el-button :disabled="busy" @click="refresh">
            {{ t("retry") }}
          </el-button>
        </p>
        <p v-if="notice" role="status" class="notice">
          {{ notice }}
        </p>
        <section v-if="!current.acknowledged" class="panel mood-notice">
          <h2>{{ t("mood.noticeTitle") }}</h2>
          <p>{{ t("mood.noticeIntro") }}</p>
          <p>
            <strong>{{ t("mood.noticePrivate") }}</strong>
          </p>
          <p>{{ t("mood.noticeChanges") }}</p>
          <h3>{{ t("mood.viewers") }}</h3>
          <ul v-if="current.viewers.length">
            <li v-for="person in current.viewers" :key="person.id">
              {{ person.displayName }} ({{ person.username }})
            </li>
          </ul>
          <p v-else>
            {{ t("mood.viewersOnly") }}
          </p>
          <p>{{ t("mood.viewersFuture") }}</p>
          <label class="mood-consent"><input v-model="accepted" type="checkbox" :disabled="busy" />{{
            t("mood.noticeAgree")
          }}</label>
          <div class="button-row">
            <el-button
              type="primary"
              :disabled="!accepted || busy"
              :loading="busy"
              @click="acknowledge"
            >
              {{ t("mood.noticeContinue") }}
            </el-button><RouterLink to="/student/home">
              {{ t("mood.notNow") }}
            </RouterLink>
          </div>
        </section>
        <template v-else>
          <details class="panel mood-sharing">
            <summary>{{ t("mood.viewers") }}</summary>
            <p>{{ t("mood.sharing") }}</p>
            <ul>
              <li>{{ t("mood.self") }}</li>
              <li v-for="person in current.viewers" :key="person.id">
                {{ person.displayName }} ({{ person.username }})
              </li>
            </ul>
            <p v-if="!current.viewers.length">
              {{ t("mood.viewersOnly") }}
            </p>
            <p>{{ t("mood.viewersFuture") }}</p>
            <p>{{ t("mood.noticeChanges") }}</p>
          </details>
          <section class="panel mood-week">
            <div class="page-heading">
              <div>
                <h2>{{ t("mood.thisWeek") }}</h2>
                <p>
                  {{
                    t("mood.weekRange", {
                      start: weekDate(current.weekStart),
                      end: weekDate(current.weekEnd),
                    })
                  }}
                </p>
              </div>
              <el-button :disabled="busy" @click="refresh">
                {{ t("mood.refresh") }}
              </el-button>
            </div>
            <p class="muted">
              {{ t("mood.timeZone") }}
            </p>
            <div v-if="current.entry">
              <p class="mood-saved-value">
                <span aria-hidden="true">{{
                  moodChoices.find(
                    (c) => c.level === current!.entry!.moodLevel,
                  )?.emoji
                }} </span>{{ t("mood.level" + current.entry.moodLevel) }}
              </p>
              <p>
                {{
                  t("mood.nextWeek", { date: weekDate(current.nextWeekStart) })
                }}
              </p>
              <details v-if="current.entry.privateNote">
                <summary>{{ t("mood.privateNote") }}</summary>
                <p class="plain-text">
                  {{ current.entry.privateNote }}
                </p>
              </details>
            </div>
            <form v-else @submit.prevent="save">
              <fieldset :disabled="busy" class="mood-fieldset">
                <legend>{{ t("mood.choose") }}</legend>
                <p>{{ t("mood.everyMood") }}</p>
                <div class="mood-choices">
                  <label
                    v-for="choice in moodChoices"
                    :key="choice.level"
                    :class="['mood-choice', { chosen: level === choice.level }]"
                  ><input
                    v-model="level"
                    type="radio"
                    name="mood-level"
                    :value="choice.level"
                    required
                  /><span aria-hidden="true" class="mood-emoji">{{
                    choice.emoji
                  }}</span><span>{{ t("mood.level" + choice.level) }}</span></label>
                </div>
                <label for="mood-note">{{ t("mood.note") }}</label>
                <p id="mood-note-help" class="muted">
                  {{ t("mood.noteHelp") }}
                </p>
                <textarea
                  id="mood-note"
                  v-model="privateNote"
                  maxlength="2000"
                  rows="4"
                  aria-describedby="mood-note-help"
                />
              </fieldset>
              <p class="muted">
                {{ t("mood.onePerWeek") }}
              </p>
              <el-button
                native-type="submit"
                type="primary"
                :disabled="level === null || busy"
                :loading="busy"
              >
                {{ t("mood.save") }}
              </el-button>
            </form>
          </section>
          <MoodTimeline
            ref="timeline"
            :enabled="current.acknowledged"
            private-notes
          />
        </template>
      </template>
    </AsyncState>
  </section>
</template>
