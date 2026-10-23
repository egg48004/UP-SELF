<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { efficacyApi } from "../api";
import type { EfficacyCurrent } from "../types";
import SelfEfficacyForm from "../components/SelfEfficacyForm.vue";
import SelfEfficacyTimeline from "../components/SelfEfficacyTimeline.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n(), current = ref<EfficacyCurrent | null>(null), loading = ref(true), loadError = ref(""), error = ref(""), busy = ref(false), accepted = ref(false), notice = ref(""), form = ref<InstanceType<typeof SelfEfficacyForm> | null>(null), timeline = ref<InstanceType<typeof SelfEfficacyTimeline> | null>(null);
async function load() {
  loading.value = true; loadError.value = "";
  try { current.value = await efficacyApi.current(); }
  catch (e) { loadError.value = (e as Error).message; }
  finally { loading.value = false; }
}
async function refresh() {
  if (busy.value || (form.value && !await form.value.canLeave())) return;
  form.value?.reset(); notice.value = ""; error.value = ""; await load();
}
async function acknowledge() {
  if (!current.value || !accepted.value || busy.value) return;
  busy.value = true; error.value = "";
  try { current.value = await efficacyApi.acknowledge(current.value.noticeVersion); }
  catch (e) { error.value = (e as Error).message; }
  finally { busy.value = false; }
}
async function saved(result: EfficacyCurrent) {
  const existingTimeline = timeline.value;
  current.value = result; notice.value = t("efficacy.saved");
  await nextTick(); await existingTimeline?.reload();
}
onMounted(load);
</script>
<template>
  <section>
    <div class="course-banner">
      <span aria-hidden="true">🧭</span><div>
        <p class="eyebrow">
          {{ t("efficacy.checkIn") }}
        </p><h1>{{ t("efficacy.title") }}</h1><p>{{ t("efficacy.intro") }}</p>
      </div>
    </div>
    <AsyncState :loading="loading" :error="loadError" @retry="load">
      <template v-if="current">
        <p v-if="error" class="notice error" role="alert">
          {{ error }}
        </p>
        <p v-if="notice" class="notice" role="status">
          {{ notice }}
        </p>
        <section v-if="!current.acknowledged" class="panel">
          <h2>{{ t("efficacy.noticeTitle") }}</h2><p>{{ t("efficacy.noticeIntro") }}</p><p>{{ t("efficacy.noticeAnswers") }}</p>
          <details>
            <summary>{{ t("efficacy.viewers") }}</summary><ul v-if="current.viewers.length">
              <li v-for="person in current.viewers" :key="person.id">
                {{ person.displayName }} ({{ person.username }})
              </li>
            </ul><p v-else>
              {{ t("efficacy.viewersOnly") }}
            </p><p>{{ t("efficacy.viewersFuture") }}</p>
          </details>
          <label class="check"><input v-model="accepted" type="checkbox" :disabled="busy" />{{ t("efficacy.noticeAgree") }}</label>
          <div class="actions">
            <el-button type="primary" :disabled="!accepted || busy" :loading="busy" @click="acknowledge">
              {{ t("efficacy.continue") }}
            </el-button><RouterLink to="/student/home">
              {{ t("mood.notNow") }}
            </RouterLink>
          </div>
        </section>
        <template v-else>
          <div class="page-heading">
            <p>{{ t("efficacy.cadence") }}</p><el-button :disabled="busy" @click="refresh">
              {{ t("efficacy.refresh") }}
            </el-button>
          </div>
          <details class="panel">
            <summary>{{ t("efficacy.viewers") }}</summary><p>{{ t("efficacy.noticeIntro") }}</p><p>{{ t("efficacy.noticeAnswers") }}</p><ul>
              <li>{{ t("mood.self") }}</li><li v-for="person in current.viewers" :key="person.id">
                {{ person.displayName }} ({{ person.username }})
              </li>
            </ul><p>{{ t("efficacy.viewersFuture") }}</p>
          </details>
          <section v-if="current.latest" class="panel">
            <h2>{{ t("efficacy.score", { score: Number(current.latest.score).toFixed(2) }) }}</h2><p>{{ current.latest.assessmentDate }}</p><p>{{ t("efficacy.reflection") }}</p><p v-if="!current.available">
              {{ t("efficacy.nextDate", { date: current.nextAvailableDate }) }}
            </p>
          </section>
          <SelfEfficacyForm v-if="current.available" ref="form" :current="current" @saved="saved" @busy="busy = $event" />
          <SelfEfficacyTimeline v-if="current.latest" ref="timeline" :enabled="current.acknowledged" :instrument="current.instrument" />
          <p v-else class="muted">
            {{ t("efficacy.empty") }}
          </p>
        </template>
        <details class="panel">
          <summary>{{ t("efficacy.about") }}</summary><p>{{ current.instrument.name }}</p><p>{{ current.instrument.attribution }}</p><p>{{ t("efficacy.presentation") }}</p><p>{{ t("efficacy.reflection") }}</p><div class="actions">
            <a :href="current.instrument.sourceUrl" target="_blank" rel="noopener noreferrer">{{ t("efficacy.source") }} ({{ t("opensNewTab") }})</a><a :href="current.instrument.licenseUrl" target="_blank" rel="noopener noreferrer">{{ current.instrument.license }}</a>
          </div>
        </details>
      </template>
    </AsyncState>
  </section>
</template>
