<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { peerApi, type PeerFeedback } from "./api";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ assignmentId: number; readonly: boolean }>();
const { t } = useI18n(),
  rows = ref<PeerFeedback[]>([]),
  current = ref(1),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  selected = ref<PeerFeedback | null>(null),
  reason = ref("");
let sequence = 0;
async function load() {
  const n = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const r = await peerApi.manage(props.assignmentId, current.value);
    if (n === sequence) {
      rows.value = r.rows;
      total.value = r.total;
    }
  } catch (e) {
    if (n === sequence) error.value = (e as Error).message;
  } finally {
    if (n === sequence) busy.value = false;
  }
}
async function hide() {
  if (busy.value || !selected.value) return;
  if (!reason.value.trim()) return;
  busy.value = true;
  error.value = "";
  try {
    await peerApi.hide(
      props.assignmentId,
      selected.value.id,
      selected.value.version,
      reason.value,
    );
    selected.value = null;
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(
  () => props.assignmentId,
  () => {
    current.value = 1;
    selected.value = null;
    void load();
  },
  { immediate: true },
);
onBeforeUnmount(() => sequence++);
</script>
<template>
  <section class="panel peer-feedback" aria-labelledby="mentor-peer-heading">
    <h2 id="mentor-peer-heading">
      {{ t("peer.manage") }}
    </h2>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <el-button :disabled="busy" @click="load">
      {{
        t("peer.refresh")
      }}
    </el-button>
    <p v-if="!rows.length">
      {{ t("peer.empty") }}
    </p>
    <article v-for="row in rows" :key="row.id" class="panel">
      <p>
        <strong>{{ t("peer.reviewer") }}:</strong> {{ row.reviewerName }} →
        <strong>{{ t("peer.recipient") }}:</strong> {{ row.studentName }}
      </p>
      <p v-if="row.earlierSubmission" class="muted">
        {{ t("peer.earlier") }}
      </p>
      <p v-if="!row.sentAt">
        {{ t("peer.pending") }}
      </p>
      <template v-else>
        <p class="plain-text">
          {{ row.strength }}
        </p>
        <p class="plain-text">
          {{ row.suggestion }}
        </p>
        <p v-if="row.hidden" class="notice">
          {{ t("peer.hiddenStatus") }}: {{ row.hideReason }}
        </p>
        <el-button
          v-else
          :disabled="readonly || busy"
          @click="
            selected = row;
            reason = '';
          "
        >
          {{ t("peer.hide") }}
        </el-button>
      </template>
    </article>
    <PaginationBar
      :current="current"
      :total="total"
      @change="
        (n) => {
          current = n;
          load();
        }
      "
    />
    <el-dialog
      :model-value="!!selected"
      :title="t('peer.hide')"
      :close-on-click-modal="!busy"
      :close-on-press-escape="!busy"
      :before-close="
        (done: () => void) => {
          if (!busy) {
            selected = null;
            done();
          }
        }
      "
      @update:model-value="
        (value: boolean) => {
          if (!value && !busy) selected = null;
        }
      "
    >
      <form @submit.prevent="hide">
        <label for="peer-hide-reason">{{ t("peer.reason") }}</label><textarea
          id="peer-hide-reason"
          v-model="reason"
          required
          maxlength="300"
          :disabled="busy"
        />
        <div class="button-row">
          <el-button :disabled="busy" @click="selected = null">
            {{
              t("peer.cancel")
            }}
          </el-button><el-button native-type="submit" :disabled="busy || !reason.trim()">
            {{
              t("peer.save")
            }}
          </el-button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>
<style scoped>
.peer-feedback {
  overflow-wrap: anywhere;
  min-width: 0;
}
</style>
