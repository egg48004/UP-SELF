<script setup lang="ts">
import { ref, watch, onBeforeUnmount, computed } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { useI18n } from "vue-i18n";
import { peerApi, type PeerState } from "./api";
import WorkFiles from "../components/WorkFiles.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const props = defineProps<{ assignmentId: number; submitted: boolean }>();
const { t } = useI18n(),
  state = ref<PeerState | null>(null),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  strength = ref(""),
  suggestion = ref("");
const dirty = computed(
  () =>
    !state.value?.outgoing?.sentAt && !!(strength.value || suggestion.value),
);
let sequence = 0;
async function load() {
  if (busy.value) return;
  if (dirty.value && !(await confirmAction(t("peer.unsaved")))) return;
  const n = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const value = await peerApi.state(props.assignmentId);
    if (n === sequence) {
      state.value = value;
      strength.value = "";
      suggestion.value = "";
    }
  } catch (e) {
    if (n === sequence) error.value = (e as Error).message;
  } finally {
    if (n === sequence) busy.value = false;
  }
}
async function allocate() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    state.value = await peerApi.allocate(props.assignmentId);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function send() {
  if (busy.value || !state.value?.outgoing) return;
  if (!strength.value.trim()) {
    error.value = t("peer.required");
    document.getElementById("peer-strength")?.focus();
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    await peerApi.send(
      props.assignmentId,
      state.value.outgoing.version,
      strength.value,
      suggestion.value,
    );
    strength.value = "";
    suggestion.value = "";
    notice.value = t("peer.sent");
    state.value = await peerApi.state(props.assignmentId);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onBeforeRouteLeave(
  async () =>
    !busy.value && (!dirty.value || (await confirmAction(t("peer.unsaved")))),
);
function unload(e: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    e.preventDefault();
    e.returnValue = "";
  }
}
window.addEventListener("beforeunload", unload);
onBeforeUnmount(() => {
  sequence++;
  window.removeEventListener("beforeunload", unload);
});
watch(
  () => [props.assignmentId, props.submitted],
  () => {
    state.value = null;
    strength.value = "";
    suggestion.value = "";
    void load();
  },
  { immediate: true },
);
</script>
<template>
  <section
    class="panel peer-feedback"
    aria-labelledby="peer-heading"
    :aria-busy="busy"
  >
    <h2 id="peer-heading">
      {{ t("peer.title") }}
    </h2>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <p v-if="notice" role="status">
      {{ notice }}
    </p>
    <el-button :disabled="busy" @click="load">
      {{
        t("peer.refresh")
      }}
    </el-button>
    <template v-if="state">
      <p v-if="!state.enabled">
        {{ t("peer.off") }}
      </p>
      <template v-if="!state.outgoing">
        <p v-if="!submitted">
          {{ t("peer.first") }}
        </p>
        <el-button
          v-if="state.enabled"
          :disabled="busy || !submitted"
          @click="allocate"
        >
          {{ t("peer.allocate") }}
        </el-button>
      </template>
      <template v-else>
        <h3>{{ t("peer.work") }}</h3>
        <p v-if="state.outgoing.earlierSubmission" class="notice">
          {{ t("peer.earlier") }}
        </p>
        <template v-if="state.outgoing.files">
          <p class="plain-text">
            {{ state.outgoing.answerText }}
          </p>
          <WorkFiles :files="state.outgoing.files" />
        </template>
        <p v-else>
          {{ t("peer.unavailable") }}
        </p>
        <p v-if="state.outgoing.hidden" class="notice">
          {{ t("peer.hidden") }}
        </p>
        <section v-else-if="state.outgoing.sentAt">
          <h3>{{ t("peer.sentTitle") }}</h3>
          <p class="plain-text">
            {{ state.outgoing.strength }}
          </p>
          <p class="plain-text">
            {{ state.outgoing.suggestion }}
          </p>
        </section>
        <form
          v-else-if="state.enabled && state.outgoing.files"
          novalidate
          @submit.prevent="send"
        >
          <fieldset :disabled="busy">
            <label for="peer-strength">{{ t("peer.strength") }}</label>
            <p id="peer-strength-help" class="muted">
              {{ t("peer.strengthHelp") }}
            </p>
            <textarea
              id="peer-strength"
              v-model="strength"
              rows="3"
              maxlength="500"
              required
              aria-describedby="peer-strength-help"
            />
            <label for="peer-suggestion">{{ t("peer.suggestion") }}</label>
            <p id="peer-suggestion-help" class="muted">
              {{ t("peer.suggestionHelp") }}
            </p>
            <textarea
              id="peer-suggestion"
              v-model="suggestion"
              rows="3"
              maxlength="500"
              aria-describedby="peer-suggestion-help"
            />
            <el-button native-type="submit" type="primary" :disabled="busy">
              {{
                t("peer.send")
              }}
            </el-button>
          </fieldset>
        </form>
      </template>
      <h3>{{ t("peer.received") }}</h3>
      <p v-if="!state.received.length">
        {{ t("peer.empty") }}
      </p>
      <article
        v-for="feedback in state.received"
        :key="feedback.id"
        class="panel"
      >
        <p v-if="feedback.earlierSubmission" class="muted">
          {{ t("peer.earlier") }}
        </p>
        <strong>{{ t("peer.strength") }}</strong>
        <p class="plain-text">
          {{ feedback.strength }}
        </p>
        <template v-if="feedback.suggestion">
          <strong>{{ t("peer.suggestion") }}</strong>
          <p class="plain-text">
            {{ feedback.suggestion }}
          </p>
        </template>
      </article>
    </template>
  </section>
</template>
<style scoped>
.peer-feedback {
  overflow-wrap: anywhere;
  min-width: 0;
}
fieldset {
  min-width: 0;
}
</style>
