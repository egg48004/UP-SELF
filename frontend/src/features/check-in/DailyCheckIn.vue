<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { data } from "../../shared/http/client";
interface State {
  today: string;
  checkedIn: boolean;
  streak: number;
  history: string[];
}
const { t } = useI18n(),
  state = ref<State | null>(null),
  busy = ref(false),
  error = ref("");
let active = true;
async function load(write = false) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const next = await data<State>("/client/learning/check-in", {
      method: write ? "POST" : "GET",
    });
    if (active) state.value = next;
  } catch (e) {
    if (active) error.value = (e as Error).message;
  } finally {
    if (active) busy.value = false;
  }
}
function refresh() {
  void load();
}
onMounted(() => {
  void load();
  window.addEventListener("focus", refresh);
});
onBeforeUnmount(() => {
  active = false;
  window.removeEventListener("focus", refresh);
});
</script>
<template>
  <section
    class="panel daily-check-in"
    aria-labelledby="daily-check-in-title"
    :aria-busy="busy"
  >
    <h2 id="daily-check-in-title">
      {{ t("dailyCheckIn.title") }}
    </h2>
    <p>{{ t("dailyCheckIn.help") }}</p>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <template v-if="state">
      <p role="status">
        <template v-if="state.checkedIn">
          <span aria-hidden="true">✓ </span>{{ t("dailyCheckIn.done") }} ·
          {{ state.today }}
        </template>
      </p>
      <p v-if="state.streak">
        {{ t("dailyCheckIn.streak", { count: state.streak }, state.streak) }}
      </p>
      <p v-else>
        {{ t("dailyCheckIn.fresh") }}
      </p>
      <el-button
        type="primary"
        :disabled="busy || state.checkedIn"
        @click="load(true)"
      >
        {{
          t(state.checkedIn ? "dailyCheckIn.done" : "dailyCheckIn.check")
        }}
      </el-button>
      <details>
        <summary>{{ t("dailyCheckIn.history") }}</summary>
        <p>{{ t("dailyCheckIn.historyHelp") }}</p>
        <ul v-if="state.history.length">
          <li v-for="day in state.history" :key="day">
            <time :datetime="day">{{ day }}</time>
          </li>
        </ul>
        <p v-else>
          {{ t("dailyCheckIn.empty") }}
        </p>
      </details>
    </template>
    <p v-else-if="busy" role="status">
      {{ t("dailyCheckIn.loading") }}
    </p>
    <el-button :disabled="busy" @click="load()">
      {{
        t("dailyCheckIn.refresh")
      }}
    </el-button>
  </section>
</template>
<style scoped>
.daily-check-in {
  overflow-wrap: anywhere;
}
.daily-check-in details {
  margin-block: 1rem;
}
</style>
