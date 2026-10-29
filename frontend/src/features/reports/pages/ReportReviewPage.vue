<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { reportApi } from "../api";
import type { Report } from "../api";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { learningDate } from "../../learning/progress";
const route = useRoute(),
  { t } = useI18n(),
  report = ref<Report>(),
  loading = ref(false),
  error = ref(""),
  busy = ref(false),
  processing = ref(""),
  actionError = ref(""),
  notice = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    report.value = await reportApi.detail(Number(route.params.id));
    processing.value = "";
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function review(status: string) {
  if (busy.value || !report.value) return;
  actionError.value = "";
  if (!processing.value.trim()) {
    actionError.value = t("reports.resolutionRequired");
    document.getElementById("processing-note")?.focus();
    return;
  }
  busy.value = true;
  try {
    await reportApi.review(
      report.value.id,
      status,
      processing.value.trim(),
      report.value.version,
    );
    notice.value = t("reports.processed");
    await load();
  } catch (e) {
    actionError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
watch(() => route.params.id, load);
onMounted(load);
</script>
<template>
  <section>
    <RouterLink to="/manage/reports">
      ← {{ t("reports.back") }}
    </RouterLink>
    <h1>{{ t("reports.open") }}</h1>
    <p v-if="notice" role="status" class="notice">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="report">
        <section class="panel">
          <p>{{ t("reports.reporter") }}: {{ report.reporterName }}</p>
          <p>{{ t("reports.accused") }}: {{ report.accusedName }}</p>
          <p>{{ report.courseTitle }} · {{ report.groupTitle }}</p>
          <p>{{ t("reports.reason") }}: {{ t("reports." + report.reason) }}</p>
          <p>{{ t("reports.status") }}: {{ t("reports." + report.status) }}</p>
          <p>{{ learningDate(report.createTime) }}</p>
          <p v-if="report.details" class="plain-text">
            {{ report.details }}
          </p>
        </section>
        <section class="panel">
          <h2>{{ t("reports.evidence") }}</h2>
          <p class="plain-text">
            {{ report.messageBody || t("reports.noText") }}
          </p>
          <h3>{{ t("reports.attachments") }}</h3>
          <p class="plain-text">
            {{ report.attachmentSummary || t("reports.noFiles") }}
          </p>
        </section>
        <section v-if="report.status === 'PENDING'" class="panel stack">
          <label for="processing-note">{{ t("reports.processing") }}</label><textarea
            id="processing-note"
            v-model="processing"
            maxlength="1000"
            rows="3"
            :disabled="busy"
          ></textarea>
          <p v-if="actionError" role="alert" class="notice error">
            {{ actionError }}
          </p>
          <div class="actions">
            <el-button
              type="primary"
              :disabled="busy"
              @click="review('RESOLVED')"
            >
              {{ t("reports.resolve") }}
            </el-button><el-button :disabled="busy" @click="review('DISMISSED')">
              {{ t("reports.dismiss") }}
            </el-button>
          </div>
        </section>
        <section v-else class="panel">
          <h2>{{ t("reports.resolution") }}</h2>
          <p class="plain-text">
            {{ report.resolution }}
          </p>
          <p>{{ learningDate(report.reviewedAt || null) }}</p>
        </section>
      </template>
    </AsyncState>
  </section>
</template>
