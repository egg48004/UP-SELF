<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { MoodEntry, MoodPoint } from "../types";
import { moodChoices, weekDate } from "../presentation";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
defineProps<{
  rows: (MoodEntry | MoodPoint)[];
  current: number;
  total: number;
  loading: boolean;
  error: string;
  privateNotes?: boolean;
}>();
defineEmits<{ page: [number]; retry: [] }>();
const { t } = useI18n();
function note(row: MoodEntry | MoodPoint) {
  return "privateNote" in row ? row.privateNote : "";
}
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
          <details v-if="privateNotes && note(row)">
            <summary>{{ t("mood.privateNote") }}</summary>
            <p class="plain-text">
              {{ note(row) }}
            </p>
          </details>
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
