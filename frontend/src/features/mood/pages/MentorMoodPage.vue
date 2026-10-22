<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { moodApi } from "../api";
import type { MoodPerson } from "../types";
import MoodTimeline from "../components/MoodTimeline.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(),
  { t } = useI18n(),
  id = computed(() => Number(route.params.studentId)),
  person = ref<MoodPerson | null>(null),
  loading = ref(false),
  error = ref("");
let seq = 0;
async function load() {
  const n = ++seq;
  person.value = null;
  loading.value = true;
  error.value = "";
  try {
    const r = await moodApi.student(id.value);
    if (n === seq) person.value = r;
  } catch (e) {
    if (n === seq) error.value = (e as Error).message;
  } finally {
    if (n === seq) loading.value = false;
  }
}
watch(id, load, { immediate: true });
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/mentor/mood">
      ← {{ t("mood.back") }}
    </RouterLink><AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="person">
        <p class="eyebrow">
          {{ t("mood.readOnly") }}
        </p>
        <h1>
          {{ person.displayName }} <small>({{ person.username }})</small>
        </h1>
        <p>{{ t("mood.mentorPrivate") }}</p>
        <MoodTimeline
          :key="person.id"
          :enabled="true"
          :student-id="person.id"
        />
      </template>
    </AsyncState>
  </section>
</template>
