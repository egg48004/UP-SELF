<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { marketApi } from "../api";
import type { Participant, MarketAttempt } from "../types";
import { malaysiaDate } from "../../assignments/api";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ taskId: number }>();
const { t } = useI18n(),
  rows = ref<Participant[]>([]),
  attempts = ref<MarketAttempt[]>([]),
  selected = ref<Participant | null>(null),
  current = ref(1),
  total = ref(0),
  attemptPage = ref(1),
  attemptTotal = ref(0),
  busy = ref(false),
  error = ref("");
let sequence = 0;
async function history() {
  if (!selected.value) return;
  const token = sequence,
    studentId = selected.value.studentId,
    taskId = props.taskId;
  const r = await marketApi.history(
    props.taskId,
    attemptPage.value,
    selected.value.studentId,
  );
  if (
    token !== sequence ||
    props.taskId !== taskId ||
    selected.value?.studentId !== studentId
  )
    return;
  attempts.value = r.rows;
  attemptTotal.value = r.total;
}
async function load() {
  const n = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const r = await marketApi.participants(props.taskId, current.value);
    if (n !== sequence) return;
    rows.value = r.rows;
    total.value = r.total;
    if (selected.value) await history();
  } catch (e) {
    if (n === sequence) error.value = (e as Error).message;
  } finally {
    if (n === sequence) busy.value = false;
  }
}
async function select(row: Participant) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  selected.value = row;
  attemptPage.value = 1;
  try {
    await history();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function historyPage(n: number) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  attemptPage.value = n;
  try {
    await history();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(
  () => props.taskId,
  () => {
    selected.value = null;
    current.value = 1;
    void load();
  },
  { immediate: true },
);
onBeforeUnmount(() => sequence++);
</script>
<template>
  <section class="panel market-participants" :aria-busy="busy">
    <h2>{{ t("market.records") }}</h2>
    <el-button :disabled="busy" @click="load">
      {{ t("market.refresh") }}
    </el-button>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <p v-if="!rows.length">
      {{ t("market.noParticipants") }}
    </p>
    <article v-for="row in rows" :key="row.studentId" class="panel">
      <h3>{{ row.studentName }} ({{ row.username }})</h3>
      <p v-if="!row.enrolled">
        {{ t("market.former") }}
      </p>
      <p>
        {{ row.attemptCount ? t("market.completed") : t("market.notStarted") }}
        · {{ t("market.attempts") }}: {{ row.attemptCount }}
      </p>
      <p v-if="row.firstChoice">
        {{ t("market.first") }}: {{ row.firstChoice }}
      </p>
      <p v-if="row.lastAttemptAt">
        {{ t("market.last") }}: {{ malaysiaDate(row.lastAttemptAt) }} MYT
      </p>
      <el-button
        :disabled="busy || !row.attemptCount"
        :aria-label="t('market.viewResponses') + ': ' + row.username"
        @click="select(row)"
      >
        {{ t("market.viewResponses") }}
      </el-button>
    </article>
    <PaginationBar
      :current="current"
      :total="total"
      @change="
        (n) => {
          if (!busy) {
            current = n;
            load();
          }
        }
      "
    />
    <section v-if="selected">
      <h3>{{ t("market.history") }} — {{ selected.username }}</h3>
      <article v-for="a in attempts" :key="a.id" class="panel">
        <strong>{{
          t(a.firstAttempt ? "market.firstAttempt" : "market.laterAttempt")
        }}</strong>
        <p>{{ malaysiaDate(a.createTime) }} MYT</p>
        <p class="plain-text">
          {{ a.label }}
        </p>
        <h4>{{ t("market.outcome") }}</h4>
        <p class="plain-text">
          {{ a.outcome }}
        </p>
        <h4>{{ t("market.why") }}</h4>
        <p class="plain-text">
          {{ a.explanation }}
        </p>
        <h4>{{ t("market.reflect") }}</h4>
        <p class="plain-text">
          {{ a.reflection || t("market.noReflection") }}
        </p>
      </article>
      <PaginationBar
        :current="attemptPage"
        :total="attemptTotal"
        @change="historyPage"
      />
    </section>
  </section>
</template>
