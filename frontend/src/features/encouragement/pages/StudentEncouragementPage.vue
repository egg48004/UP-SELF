<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { studentEncouragementApi as api } from "../api";
import type { InboxMessage } from "../types";
import { learningDate } from "../../learning/progress";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  rows = ref<InboxMessage[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const result = await api.inbox(page);
    if (seq !== sequence) return;
    rows.value = result.rows;
    total.value = result.total;
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
onMounted(() => load());
onBeforeUnmount(() => {
  sequence++;
});
</script>
<template>
  <section class="inbox">
    <RouterLink to="/student/home">
      ← {{ t("encouragement.backHome") }}
    </RouterLink>
    <h1>
      <span aria-hidden="true">🌱 </span>{{ t("encouragement.inboxTitle") }}
    </h1>
    <div class="actions">
      <el-button :disabled="loading" @click="load(current)">
        {{
          t("encouragement.refresh")
        }}
      </el-button>
    </div>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <p v-if="!rows.length" class="panel">
        {{ t("encouragement.empty") }}
      </p>
      <ol class="inbox-list">
        <li v-for="message in rows" :key="message.id">
          <article class="panel">
            <h2>{{ t("encouragement.from", { name: message.mentorName }) }}</h2>
            <p class="encouragement-body">
              {{ message.body }}
            </p>
            <p class="muted">
              <time :datetime="message.sentAt">{{
                learningDate(message.sentAt)
              }}</time>
            </p>
          </article>
        </li>
      </ol>
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
.inbox {
  max-width: 54rem;
  margin-inline: auto;
}
.inbox-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.encouragement-body {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 1.1rem;
  line-height: 1.8;
}
</style>
