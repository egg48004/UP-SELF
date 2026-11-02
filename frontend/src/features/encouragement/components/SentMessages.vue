<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { mentorEncouragementApi as api } from "../api";
import type { SentMessage, SentDetails } from "../types";
import { learningDate } from "../../learning/progress";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  rows = ref<SentMessage[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  detail = ref<SentDetails>(),
  detailLoading = ref<number | null>(null),
  detailError = ref("");
let sequence = 0,
  detailSequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  ++detailSequence;
  detail.value = undefined;
  detailLoading.value = null;
  detailError.value = "";
  loading.value = true;
  error.value = "";
  try {
    const r = await api.history(page);
    if (seq !== sequence) return;
    rows.value = r.rows;
    total.value = r.total;
    current.value = page;
  } catch (e) {
    if (seq === sequence) {
      error.value = (e as Error).message;
      rows.value = [];
    }
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
async function recipients(id: number) {
  if (detail.value?.message.id === id) {
    detail.value = undefined;
    return;
  }
  const seq = ++detailSequence;
  detailLoading.value = id;
  detailError.value = "";
  detail.value = undefined;
  try {
    const result = await api.sent(id);
    if (seq === detailSequence) detail.value = result;
  } catch (e) {
    if (seq === detailSequence) detailError.value = (e as Error).message;
  } finally {
    if (seq === detailSequence) detailLoading.value = null;
  }
}
onMounted(() => load());
onBeforeUnmount(() => {
  sequence++;
  detailSequence++;
});
</script>
<template>
  <section :aria-label="t('encouragement.history')">
    <div class="actions">
      <h2>{{ t("encouragement.history") }}</h2>
      <el-button @click="load(current)">
        {{
          t("encouragement.refresh")
        }}
      </el-button>
    </div>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <p v-if="!rows.length">
        {{ t("encouragement.emptySent") }}
      </p>
      <p v-if="detailError" role="alert" class="notice error">
        {{ detailError }}
      </p>
      <ul class="sent-list">
        <li v-for="message in rows" :key="message.id" class="panel">
          <p class="message-text">
            {{ message.body }}
          </p>
          <p class="muted">
            {{ learningDate(message.sentAt) }} ·
            {{
              t("encouragement.recipientCount", {
                count: message.recipientCount,
              })
            }}
          </p>
          <el-button
            :loading="detailLoading === message.id"
            :aria-expanded="detail?.message.id === message.id"
            @click="recipients(message.id)"
          >
            {{
              t(
                detail?.message.id === message.id
                  ? "encouragement.hideRecipients"
                  : "encouragement.recipients",
              )
            }}
          </el-button>
          <ul v-if="detail?.message.id === message.id" class="recipient-names">
            <li v-for="person in detail.recipients" :key="person.id">
              {{ person.name }} <small>{{ person.username }}</small>
            </li>
          </ul>
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
.sent-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.message-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.recipient-names {
  overflow-wrap: anywhere;
}
</style>
