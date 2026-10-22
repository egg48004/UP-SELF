<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { assignmentApi, malaysiaDate } from "../api";
import type { Assignment, Submission } from "../types";
import AssignmentEditor from "../components/AssignmentEditor.vue";
import ReviewPanel from "../components/ReviewPanel.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const route = useRoute(),
  { t } = useI18n(),
  id = computed(() => Number(route.params.id)),
  assignment = ref<Assignment | null>(null),
  rows = ref<Submission[]>([]),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  editing = ref(false),
  selected = ref<number | null>(null);
async function list() {
  const r = await assignmentApi.submissions(id.value, current.value);
  rows.value = r.rows;
  total.value = r.total;
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    assignment.value = await assignmentApi.detail(id.value);
    await list();
  } catch (e) {
    assignment.value = null;
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function page(n: number) {
  current.value = n;
  try {
    await list();
  } catch (e) {
    error.value = (e as Error).message;
  }
}
watch(
  id,
  () => {
    selected.value = null;
    current.value = 1;
    void load();
  },
  { immediate: true },
);
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/mentor/assignments">
      ← {{ t("assignment.back") }} </RouterLink
    ><AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="assignment">
        <div class="page-heading">
          <div>
            <p class="eyebrow">
              {{ assignment.courseTitle }}
            </p>
            <h1>{{ assignment.title }}</h1>
            <span class="status">{{
              t("assignment." + assignment.status)
            }}</span>
          </div>
          <el-button
            :disabled="assignment.courseStatus === 'ARCHIVED'"
            @click="editing = true"
          >
            {{ t("assignment.edit") }}
          </el-button>
        </div>
        <p v-if="assignment.courseStatus === 'ARCHIVED'" class="notice">
          {{ t("assignment.archivedCourse") }}
        </p>
        <details class="panel">
          <summary>{{ t("assignment.details") }}</summary>
          <p class="plain-text">
            {{ assignment.instructions }}
          </p>
          <p>
            {{
              assignment.dueAt
                ? malaysiaDate(assignment.dueAt) + " MYT"
                : t("assignment.noDue")
            }}
          </p>
        </details>
        <section class="panel">
          <h2>{{ t("assignment.submissions") }}</h2>
          <p v-if="!rows.length">
            {{ t("assignment.noSubmissions") }}
          </p>
          <div class="table-scroll">
            <table v-if="rows.length">
              <thead>
                <tr>
                  <th>{{ t("student") }}</th>
                  <th>{{ t("assignment.status") }}</th>
                  <th>{{ t("actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in rows" :key="s.id!">
                  <td>
                    {{ s.studentName }} <small>({{ s.studentUsername }})</small>
                  </td>
                  <td>{{ t("assignment." + s.status) }}</td>
                  <td>
                    <el-button
                      :aria-label="
                        t('assignment.review') + ': ' + s.studentUsername
                      "
                      @click="selected = s.id"
                    >
                      {{ t("assignment.review") }}
                    </el-button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <PaginationBar :current="current" :total="total" @change="page" />
        </section>
        <ReviewPanel
          v-if="selected"
          :key="selected"
          :assignment-id="id"
          :submission-id="selected"
          :readonly="assignment.courseStatus === 'ARCHIVED'"
          @saved="page(current)"
        /><AssignmentEditor
          v-model="editing"
          :assignment="assignment"
          @saved="load"
        />
      </template>
    </AsyncState>
  </section>
</template>
