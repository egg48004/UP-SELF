<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { goalApi } from "../api";
import type { GoalFilter, PersonalGoal } from "../types";
import GoalEditor from "../components/GoalEditor.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";

const { t } = useI18n(),
  rows = ref<PersonalGoal[]>([]),
  current = ref(1),
  total = ref(0),
  status = ref<GoalFilter>(""),
  loading = ref(false),
  error = ref(""),
  actionError = ref(""),
  notice = ref(""),
  editorOpen = ref(false),
  editing = ref<PersonalGoal>(),
  saving = ref(false),
  acting = ref(false),
  deleting = ref<PersonalGoal | null>(null);
let sequence = 0;
async function load(page = 1) {
  const active = ++sequence;
  loading.value = true;
  error.value = "";
  current.value = page;
  try {
    const result = await goalApi.list(page, status.value);
    if (active !== sequence) return;
    // Deleting the last goal on a page returns to the last remaining page.
    const last = Math.max(1, Math.ceil(result.total / 10));
    if (page > last) return await load(last);
    rows.value = result.rows;
    total.value = result.total;
  } catch (e) {
    if (active === sequence) error.value = (e as Error).message;
  } finally {
    if (active === sequence) loading.value = false;
  }
}
function open(goal?: PersonalGoal) {
  editing.value = goal;
  saving.value = false;
  editorOpen.value = true;
  actionError.value = "";
  notice.value = "";
}
async function saved() {
  editorOpen.value = false;
  notice.value = t("goal.saved");
  if (!editing.value) status.value = "";
  await load(editing.value ? current.value : 1);
}
async function complete(goal: PersonalGoal) {
  if (acting.value) return;
  acting.value = true;
  actionError.value = "";
  notice.value = "";
  const completed = goal.status === "ACTIVE";
  try {
    await goalApi.complete(goal.id, completed, goal.version);
    notice.value = t(completed ? "goal.completed" : "goal.reopened");
    await load(current.value);
  } catch (e) {
    actionError.value = (e as Error).message;
  } finally {
    acting.value = false;
  }
}
async function remove() {
  if (!deleting.value || acting.value) return;
  acting.value = true;
  actionError.value = "";
  notice.value = "";
  try {
    await goalApi.remove(deleting.value.id, deleting.value.version);
    deleting.value = null;
    notice.value = t("goal.deleted");
    await load(current.value);
  } catch (e) {
    actionError.value = (e as Error).message;
    deleting.value = null;
  } finally {
    acting.value = false;
  }
}
function closeDelete(value: boolean) {
  if (!value && !acting.value) deleting.value = null;
}
onMounted(() => load());
</script>

<template>
  <section class="goals-page">
    <h1><span aria-hidden="true">🌱 </span>{{ t("goal.title") }}</h1>
    <p>{{ t("goal.intro") }}</p>
    <p class="muted">
      {{ t("goal.privacy") }}
    </p>
    <div class="actions goal-toolbar">
      <el-button type="primary" :disabled="loading || acting" @click="open()">
        {{ t("goal.create") }}
      </el-button>
      <label for="goal-filter">{{ t("goal.filter") }}</label>
      <select
        id="goal-filter"
        v-model="status"
        :disabled="acting || loading"
        @change="load()"
      >
        <option value="">
          {{ t("goal.all") }}
        </option>
        <option value="ACTIVE">
          {{ t("goal.ACTIVE") }}
        </option>
        <option value="COMPLETED">
          {{ t("goal.COMPLETED") }}
        </option>
      </select>
      <el-button
        :disabled="loading || acting"
        @click="
          actionError = '';
          load(current);
        "
      >
        {{ t("goal.refresh") }}
      </el-button>
    </div>
    <p class="notice" role="status" aria-live="polite">
      {{ notice }}
    </p>
    <p v-if="actionError" class="notice error" role="alert">
      {{ actionError }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div v-if="!rows.length" class="panel">
        <h2>{{ t("goal.empty") }}</h2>
        <p>{{ t("goal.emptyHelp") }}</p>
      </div>
      <ul v-else class="goal-list">
        <li v-for="goal in rows" :key="goal.id">
          <article class="panel" :aria-labelledby="'goal-heading-' + goal.id">
            <h2 :id="'goal-heading-' + goal.id">
              {{ goal.title }}
            </h2>
            <p class="goal-status">
              <span aria-hidden="true">{{ goal.status === "COMPLETED" ? "✓" : "○" }}
              </span>
              {{ t("goal." + goal.status) }}
            </p>
            <p v-if="goal.details" class="goal-details">
              {{ goal.details }}
            </p>
            <div class="actions">
              <el-button
                type="primary"
                :disabled="acting || loading"
                @click="complete(goal)"
              >
                {{
                  t(goal.status === "ACTIVE" ? "goal.complete" : "goal.reopen")
                }}
              </el-button>
              <el-button :disabled="acting || loading" @click="open(goal)">
                {{
                  t("goal.edit")
                }}
              </el-button>
              <el-button
                :disabled="acting || loading"
                @click="
                  deleting = goal;
                  actionError = '';
                "
              >
                {{ t("goal.remove") }}
              </el-button>
            </div>
          </article>
        </li>
      </ul>
      <PaginationBar
        v-if="total > 10"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
    <el-dialog
      v-model="editorOpen"
      :title="t(editing ? 'goal.edit' : 'goal.create')"
      width="min(560px,95vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      destroy-on-close
    >
      <GoalEditor
        v-if="editorOpen"
        :goal="editing"
        @busy="saving = $event"
        @saved="saved"
        @cancel="editorOpen = false"
      />
    </el-dialog>
    <el-dialog
      :model-value="!!deleting"
      :title="t('goal.remove')"
      width="min(480px,95vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!acting"
      :show-close="!acting"
      @update:model-value="closeDelete"
    >
      <p class="goal-details">
        {{ deleting?.title }}
      </p>
      <p>{{ t("goal.deleteHelp") }}</p>
      <div class="actions">
        <el-button :disabled="acting" @click="deleting = null">
          {{
            t("cancel")
          }}
        </el-button>
        <el-button type="danger" :loading="acting" @click="remove">
          {{
            t("goal.remove")
          }}
        </el-button>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.goals-page {
  max-width: 900px;
  margin-inline: auto;
}
.goal-toolbar {
  flex-wrap: wrap;
  margin-block: 1.5rem;
}
.goal-toolbar select {
  width: auto;
}
.goal-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.goal-list h2,
.goal-details {
  overflow-wrap: anywhere;
}
.goal-details {
  white-space: pre-wrap;
}
.goal-status {
  font-weight: 600;
}
</style>
