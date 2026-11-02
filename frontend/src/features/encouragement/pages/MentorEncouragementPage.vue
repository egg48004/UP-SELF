<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import { useRoute, onBeforeRouteLeave, onBeforeRouteUpdate } from "vue-router";
import { useI18n } from "vue-i18n";
import { mentorEncouragementApi as api } from "../api";
import { useEncouragementComposer } from "../useEncouragementComposer";
import RecipientPicker from "../components/RecipientPicker.vue";
import SentMessages from "../components/SentMessages.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  route = useRoute(),
  composer = useEncouragementComposer(),
  { selected, body, busy, error, notice, dirty } = composer;
const tab = ref(route.query.tab === "history" ? "history" : "compose"),
  prefillError = ref(""),
  prefilling = ref(false);
let sequence = 0;
async function prefill() {
  const seq = ++sequence;
  prefillError.value = "";
  if (!route.query.studentId) return;
  const id = Number(route.query.studentId);
  if (!Number.isSafeInteger(id) || id < 1) {
    prefillError.value = t("encouragement.prefill");
    return;
  }
  prefilling.value = true;
  try {
    const person = await api.student(id);
    if (seq === sequence) selected.value = [person];
  } catch (e) {
    if (seq === sequence) prefillError.value = (e as Error).message;
  } finally {
    if (seq === sequence) prefilling.value = false;
  }
}
async function switchTab(next: string) {
  if (busy.value || next === tab.value) return;
  if (dirty.value && !(await confirmAction(t("encouragement.leave")))) return;
  if (dirty.value) composer.reset();
  tab.value = next;
}
const canLeave = async () =>
  !busy.value &&
  (!dirty.value || (await confirmAction(t("encouragement.leave"))));
onBeforeRouteLeave(canLeave);
onBeforeRouteUpdate(canLeave);
function unload(event: BeforeUnloadEvent) {
  if (dirty.value) {
    event.preventDefault();
    event.returnValue = "";
  }
}
onMounted(() => {
  void prefill();
  window.addEventListener("beforeunload", unload);
});
watch(
  () => route.query.studentId,
  () => {
    composer.reset();
    tab.value = "compose";
    void prefill();
  },
);
onBeforeUnmount(() => {
  sequence++;
  composer.dispose();
  window.removeEventListener("beforeunload", unload);
});
</script>
<template>
  <section>
    <RouterLink to="/mentor/students">
      ← {{ t("encouragement.backStudents") }}
    </RouterLink>
    <h1>{{ t("encouragement.title") }}</h1>
    <p>{{ t("encouragement.mentorHelp") }}</p>
    <nav class="actions" :aria-label="t('encouragement.sections')">
      <el-button
        :aria-pressed="tab === 'compose'"
        :disabled="busy"
        @click="switchTab('compose')"
      >
        {{ t("encouragement.compose") }}
      </el-button><el-button
        :aria-pressed="tab === 'history'"
        :disabled="busy"
        @click="switchTab('history')"
      >
        {{ t("encouragement.history") }}
      </el-button>
    </nav>
    <p v-if="notice" role="status" class="notice">
      {{ notice }}
    </p>
    <template v-if="tab === 'compose'">
      <p v-if="prefillError" role="alert" class="notice error">
        {{ prefillError }}
      </p>
      <RecipientPicker v-model="selected" :disabled="busy || prefilling" />
      <form
        class="panel stack"
        :aria-busy="busy"
        @submit.prevent="composer.send"
      >
        <label for="encouragement-body">{{ t("encouragement.body") }}</label>
        <textarea
          id="encouragement-body"
          v-model="body"
          rows="5"
          maxlength="2000"
          required
          :disabled="busy"
          aria-describedby="encouragement-help"
        ></textarea>
        <p id="encouragement-help" class="muted">
          {{ t("encouragement.bodyHelp") }}
        </p>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <el-button
          native-type="submit"
          type="primary"
          :loading="busy"
          :disabled="busy || prefilling || !selected.length || !body.trim()"
        >
          {{ t("encouragement.send") }}
        </el-button>
      </form>
    </template>
    <SentMessages v-else />
  </section>
</template>
