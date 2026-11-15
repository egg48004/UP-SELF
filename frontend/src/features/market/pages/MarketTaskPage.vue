<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from "vue";
import {
  useRoute,
  useRouter,
  onBeforeRouteLeave,
  onBeforeRouteUpdate,
} from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { marketApi } from "../api";
import type { MarketTask, MarketAttempt } from "../types";
import MarketTaskEditor from "../components/MarketTaskEditor.vue";
import MarketParticipants from "../components/MarketParticipants.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
import { malaysiaDate } from "../../assignments/api";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  { t } = useI18n(),
  id = computed(() => Number(route.params.id));
const task = ref<MarketTask | null>(null),
  attempts = ref<MarketAttempt[]>([]),
  total = ref(0),
  current = ref(1),
  result = ref<MarketAttempt | null>(null),
  choice = ref<number | null>(null),
  reflection = ref(""),
  busy = ref(false),
  loading = ref(false),
  error = ref(""),
  notice = ref(""),
  editing = ref(false),
  readonly = computed(() => task.value?.courseStatus === "ARCHIVED");
let sequence = 0,
  requestKey = crypto.randomUUID();
const base = computed(() =>
    auth.isStudent ? "/student/market" : "/mentor/market",
  ),
  dirty = computed(() => reflection.value !== (result.value?.reflection || ""));
async function guard() {
  return (
    !busy.value && (!dirty.value || (await confirmAction(t("market.leave"))))
  );
}
onBeforeRouteLeave(guard);
onBeforeRouteUpdate(guard);
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
async function history() {
  const token = sequence,
    taskId = id.value,
    page = current.value;
  const r = await marketApi.history(taskId, page);
  if (token !== sequence || taskId !== id.value || page !== current.value)
    return;
  attempts.value = r.rows;
  total.value = r.total;
}
async function load() {
  const n = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const value = await marketApi.detail(id.value);
    if (n !== sequence) return;
    task.value = value;
    if (auth.isStudent) await history();
  } catch (e) {
    if (n === sequence) {
      task.value = null;
      error.value = (e as Error).message;
    }
  } finally {
    if (n === sequence) loading.value = false;
  }
}
async function refresh() {
  if (!(await guard())) return;
  await load();
}
async function state(value: string) {
  if (busy.value || !task.value) return;
  busy.value = true;
  error.value = "";
  try {
    await marketApi.status(id.value, value, task.value.version);
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function copy() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const value = await marketApi.copy(id.value);
    busy.value = false;
    await router.push(base.value + "/" + value);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function choose() {
  if (busy.value || !task.value) return;
  if (choice.value === null) {
    error.value = t("market.chooseFirst");
    document.getElementById("market-choice-0")?.focus();
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    result.value = await marketApi.choose(
      id.value,
      choice.value,
      task.value.version,
      requestKey,
    );
    reflection.value = result.value.reflection;
    notice.value = "";
    current.value = 1;
    await history();
    requestKey = crypto.randomUUID();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function retry() {
  if (!(await guard())) return;
  result.value = null;
  reflection.value = "";
  choice.value = null;
  notice.value = "";
  requestKey = crypto.randomUUID();
}
async function open(a: MarketAttempt) {
  if (!(await guard())) return;
  result.value = a;
  reflection.value = a.reflection;
  notice.value = "";
}
async function saveReflection() {
  if (busy.value || !result.value) return;
  busy.value = true;
  error.value = "";
  try {
    await marketApi.reflect(
      id.value,
      result.value.id,
      result.value.version,
      reflection.value,
    );
    result.value = {
      ...result.value,
      reflection: reflection.value.trim(),
      version: result.value.version + 1,
    };
    reflection.value = result.value.reflection;
    notice.value = t("market.reflectionSaved");
    await history();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function page(n: number) {
  if (busy.value) return;
  current.value = n;
  try {
    await history();
  } catch (e) {
    error.value = (e as Error).message;
  }
}
watch(
  id,
  () => {
    result.value = null;
    reflection.value = "";
    choice.value = null;
    current.value = 1;
    requestKey = crypto.randomUUID();
    void load();
  },
  { immediate: true },
);
</script>
<template>
  <section class="market-task-page">
    <RouterLink
      class="back-link"
      :to="{ path: base, query: task ? { courseId: task.courseId } : {} }"
    >
      ← {{ t("market.back") }}
    </RouterLink>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <p v-if="notice" role="status">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" @retry="refresh">
      <template v-if="task">
        <div class="page-heading">
          <div>
            <p class="eyebrow">
              {{ task.courseTitle }}
            </p>
            <h1>{{ task.title }}</h1>
            <p>{{ task.objective }}</p>
            <p class="status">
              {{ t("market." + task.status.toLowerCase()) }}
            </p>
          </div>
        </div>
        <el-button :disabled="busy" @click="refresh">
          {{ t("market.refresh") }}
        </el-button>
        <template v-if="auth.isMentor">
          <p v-if="readonly" class="notice">
            {{ t("assignment.archivedCourse") }}
          </p>
          <p v-if="task.locked" class="notice">
            {{ t("market.locked") }}
          </p>
          <div class="button-row">
            <el-button
              :disabled="busy || task.locked || readonly"
              @click="editing = true"
            >
              {{ t("market.edit") }}
            </el-button><el-button :disabled="busy || readonly" @click="copy">
              {{ t("market.copy") }}
            </el-button><el-button
              v-if="task.status !== 'PUBLISHED'"
              :disabled="busy || readonly"
              @click="state('PUBLISHED')"
            >
              {{ t("market.publish") }}
            </el-button><el-button
              v-if="task.status === 'PUBLISHED'"
              :disabled="busy || readonly"
              @click="state('CLOSED')"
            >
              {{ t("market.close") }}
            </el-button><el-button
              v-if="!task.locked && task.status !== 'DRAFT'"
              :disabled="busy || readonly"
              @click="state('DRAFT')"
            >
              {{ t("market.toDraft") }}
            </el-button>
          </div>
        </template>
        <section class="panel">
          <h2>{{ t("market.situation") }}</h2>
          <p class="plain-text">
            {{ task.situation }}
          </p>
          <p class="muted">
            {{ t("market.practiceOnly") }}
          </p>
        </section>
        <template v-if="auth.isStudent">
          <p class="muted">
            {{ t("market.shared") }}
          </p>
          <p v-if="total && !result">
            {{ t("market.completeHelp") }}
          </p>
          <p v-if="task.status === 'CLOSED'" class="notice">
            {{ t("market.closedHelp") }}
          </p>
          <form
            v-if="!result && task.status === 'PUBLISHED'"
            class="panel"
            @submit.prevent="choose"
          >
            <fieldset :disabled="busy">
              <legend>{{ task.question }}</legend>
              <p>{{ t("market.choiceHelp") }}</p>
              <label
                v-for="(option, i) in task.options"
                :key="option.id"
                class="market-choice"
                :for="'market-choice-' + i"
              ><input
                :id="'market-choice-' + i"
                v-model="choice"
                type="radio"
                name="market-response"
                :value="option.id"
              />{{ option.label }}</label><el-button native-type="submit" type="primary" :loading="busy">
                {{ t("market.choose") }}
              </el-button>
            </fieldset>
          </form>
          <section v-if="result" class="panel market-result" aria-live="polite">
            <p>
              {{
                t(
                  result.firstAttempt
                    ? "market.firstAttempt"
                    : "market.laterAttempt",
                )
              }}
            </p>
            <h2>{{ result.label }}</h2>
            <h3>{{ t("market.outcome") }}</h3>
            <p class="plain-text">
              {{ result.outcome }}
            </p>
            <h3>{{ t("market.why") }}</h3>
            <p class="plain-text">
              {{ result.explanation }}
            </p>
            <p>{{ t("market.completeHelp") }}</p>
            <form
              v-if="task.status === 'PUBLISHED'"
              @submit.prevent="saveReflection"
            >
              <label for="market-reflection">{{ t("market.reflect") }}</label>
              <p>{{ task.reflectionPrompt }}</p>
              <textarea
                id="market-reflection"
                v-model="reflection"
                rows="3"
                maxlength="1000"
                :disabled="busy"
              /><el-button native-type="submit" :disabled="busy || !dirty">
                {{ t("market.saveReflection") }}
              </el-button>
            </form>
            <p v-else class="plain-text">
              {{ result.reflection || t("market.noReflection") }}
            </p>
            <el-button
              v-if="task.status === 'PUBLISHED'"
              :disabled="busy"
              @click="retry"
            >
              {{ t("market.retry") }}
            </el-button>
          </section>
          <details class="panel market-history">
            <summary>{{ t("market.history") }} · {{ total }}</summary>
            <p>{{ t("market.historyHelp") }}</p>
            <p v-if="!attempts.length">
              {{ t("market.noHistory") }}
            </p>
            <article v-for="a in attempts" :key="a.id" class="panel">
              <strong>{{
                t(
                  a.firstAttempt
                    ? "market.firstAttempt"
                    : "market.laterAttempt",
                )
              }}</strong>
              <p>{{ a.label }}</p>
              <p>{{ malaysiaDate(a.createTime) }} MYT</p>
              <el-button
                :disabled="busy"
                :aria-label="t('market.view') + ': ' + a.id"
                @click="open(a)"
              >
                {{ t("market.view") }}
              </el-button>
            </article>
            <PaginationBar :current="current" :total="total" @change="page" />
          </details>
        </template>
        <template v-else>
          <section class="panel">
            <h2>{{ task.question }}</h2>
            <article
              v-for="option in task.options"
              :key="option.id"
              class="panel"
            >
              <h3>{{ option.label }}</h3>
              <h4>{{ t("market.outcome") }}</h4>
              <p class="plain-text">
                {{ option.outcome }}
              </p>
              <h4>{{ t("market.why") }}</h4>
              <p class="plain-text">
                {{ option.explanation }}
              </p>
            </article>
          </section>
          <MarketParticipants :key="id" :task-id="id" /><MarketTaskEditor
            v-model="editing"
            :course-id="task.courseId"
            :task="task"
            @saved="load"
          />
        </template>
      </template><el-button v-else :disabled="loading" @click="refresh">
        {{ t("market.refresh") }}
      </el-button>
    </AsyncState>
  </section>
</template>
<style scoped>
.market-task-page {
  overflow-wrap: anywhere;
}
.market-choice {
  display: flex;
  gap: 0.8rem;
  align-items: flex-start;
  border: 1px solid var(--border-color, #ccd6cf);
  border-radius: 0.8rem;
  padding: 1rem;
  margin-block: 0.8rem;
  cursor: pointer;
}
.market-choice input {
  width: auto;
  flex: none;
  margin-top: 0.25rem;
}
.market-task-page fieldset {
  min-width: 0;
}
</style>
