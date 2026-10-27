<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { knowledgeApi } from "../api";
import { knowledgeChoices, knowledgeDate } from "../presentation";
import type { KnowledgeAnswer } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ questionId: number }>();
const { t } = useI18n(),
  rows = ref<KnowledgeAnswer[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(page = current.value) {
  const seq = ++sequence;
  current.value = page;
  loading.value = true;
  error.value = "";
  try {
    const result = await knowledgeApi.answers(props.questionId, page);
    if (seq === sequence) {
      rows.value = result.rows;
      total.value = result.total;
    }
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
watch(
  () => props.questionId,
  () => {
    rows.value = [];
    total.value = 0;
    void load(1);
  },
  { immediate: true },
);
</script>
<template>
  <AsyncState :loading="loading" :error="error" @retry="load()">
    <p v-if="!rows.length">
      {{ t("knowledge.noAnswers") }}
    </p>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th scope="col">
              {{ t("mood.student") }}
            </th>
            <th scope="col">
              {{ t("knowledge.responses") }}
            </th>
            <th scope="col">
              {{ t("knowledge.updated") }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.studentName }} ({{ row.username }})</td>
            <td>
              {{
                t(
                  knowledgeChoices.find((item) => item.value === row.choice)!
                    .key,
                )
              }}
            </td>
            <td>{{ knowledgeDate(row.updateTime) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <PaginationBar :current="current" :total="total" @change="load" />
  </AsyncState>
</template>
