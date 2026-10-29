<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { followUpApi } from "../api";
import type { Reminder } from "../api";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const { t } = useI18n(),
  route = useRoute(),
  rows = ref<Reminder[]>([]),
  current = ref(1),
  total = ref(0),
  status = ref("PENDING"),
  loading = ref(false),
  error = ref(""),
  busy = ref(false),
  notice = ref("");
let sequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const sid = route.query.studentId
      ? Number(route.query.studentId)
      : undefined;
    const r = await followUpApi.list(page, status.value, sid);
    if (seq !== sequence) return;
    const last = Math.max(1, Math.ceil(r.total / 10));
    if (page > last) return await load(last);
    rows.value = r.rows;
    current.value = page;
    total.value = r.total;
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
async function review(r: Reminder) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await followUpApi.review(r);
    notice.value = t("followUps.reviewed");
    await load(current.value);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(
  () => route.query.studentId,
  () => load(),
);
onMounted(() => load());
</script>
<template>
  <section>
    <RouterLink to="/mentor/students">
      ← {{ t("followUps.back") }}
    </RouterLink>
    <h1>{{ t("followUps.title") }}</h1>
    <p>{{ t("followUps.intro") }}</p>
    <div class="actions">
      <label for="reminder-filter">{{ t("followUps.filter") }}</label><select
        id="reminder-filter"
        v-model="status"
        :disabled="busy || loading"
        @change="load()"
      >
        <option
          v-for="s in ['PENDING', 'REVIEWED', 'ENDED']"
          :key="s"
          :value="s"
        >
          {{ t("followUps." + s) }}
        </option>
      </select><el-button :disabled="busy || loading" @click="load(current)">
        {{ t("followUps.refresh") }}
      </el-button>
    </div>
    <p v-if="notice" class="notice" role="status">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <p v-if="!rows.length">
        {{ t("followUps.empty") }}
      </p>
      <ul class="reminders">
        <li v-for="r in rows" :key="r.id">
          <article class="panel">
            <h2>{{ t("followUps." + r.kind) }}</h2>
            <p>
              <strong>{{ r.studentName }}</strong> · {{ r.username }}
            </p>
            <p v-if="r.courseTitle">
              {{ r.courseTitle }}
            </p>
            <p>{{ r.reason }}</p>
            <p>{{ t("followUps.date") }}: {{ r.eventDate }}</p>
            <div class="actions">
              <el-button
                v-if="r.active && !r.reviewed"
                type="primary"
                :disabled="busy"
                @click="review(r)"
              >
                {{ t("followUps.review") }}
              </el-button><RouterLink :to="'/mentor/students/' + r.studentId + '/notes'">
                {{ t("followUps.notes") }}
              </RouterLink><RouterLink
                :to="
                  r.kind === 'MOOD'
                    ? '/mentor/mood/' + r.studentId
                    : r.assignmentId
                      ? '/mentor/assignments/' + r.assignmentId
                      : '/manage/courses/' + r.courseId
                "
              >
                {{ t("followUps.record") }}
              </RouterLink>
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
  </section>
</template>
<style scoped>
.reminders {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.reminders article {
  overflow-wrap: anywhere;
}
</style>
