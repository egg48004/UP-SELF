<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { MoodEntry, MentorMoodEntry } from "../types";
import { moodChoices, weekDate } from "../presentation";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import AudioPlayer from "../../recordings/components/AudioPlayer.vue";
import { moodApi } from "../api";
defineProps<{
  rows: (MoodEntry | MentorMoodEntry)[];
  current: number;
  total: number;
  loading: boolean;
  error: string;
  showNotes?: boolean;
}>();
defineEmits<{ page: [number]; retry: [] }>();
const { t } = useI18n();
function note(row: MoodEntry | MentorMoodEntry) {
  return "privateNote" in row ? row.privateNote : row.diaryNote;
}
function audioLoader(row: MoodEntry | MentorMoodEntry) { return () => moodApi.audio(row.recordingId!, !("privateNote" in row)); }
</script>
<template>
  <section class="panel mood-history">
    <h2>{{ t("mood.history") }}</h2>
    <AsyncState :loading="loading" :error="error" @retry="$emit('retry')">
      <p v-if="!rows.length">
        {{ t("mood.noHistory") }}
      </p>
      <ol v-else class="mood-history-list">
        <li v-for="row in rows" :key="row.weekStart">
          <h3>{{ t("mood.week", { date: weekDate(row.weekStart) }) }}</h3>
          <p>
            <span aria-hidden="true">{{
              moodChoices.find((c) => c.level === row.moodLevel)?.emoji
            }} </span><strong>{{ t("mood.level" + row.moodLevel) }}</strong>
          </p>
          <details v-if="showNotes && note(row)">
            <summary>{{ t("mood.diary") }}</summary>
            <p class="plain-text">
              {{ note(row) }}
            </p>
          </details>
          <AudioPlayer v-if="showNotes && row.recordingId" :source-key="'diary-' + row.recordingId" :label="row.recordingName || t('recording.play')" :load="audioLoader(row)" />
        </li>
      </ol>
      <PaginationBar
        :current="current"
        :total="total"
        @change="$emit('page', $event)"
      />
    </AsyncState>
  </section>
</template>
