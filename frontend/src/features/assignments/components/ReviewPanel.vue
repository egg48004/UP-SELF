<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { assignmentApi } from "../api";
import type { Submission } from "../types";
import SubmissionHistory from "./SubmissionHistory.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const props = defineProps<{
    assignmentId: number;
    submissionId: number;
    readonly?: boolean;
  }>(),
  emit = defineEmits<{ saved: [] }>(),
  { t } = useI18n(),
  work = ref<Submission | null>(null),
  loading = ref(false),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  decision = ref<"GRADED" | "RETURNED">("GRADED"),
  score = ref<number | null>(null),
  feedback = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  feedback.value = "";
  score.value = null;
  try {
    work.value = await assignmentApi.reviewDetail(
      props.assignmentId,
      props.submissionId,
    );
  } catch (e) {
    work.value = null;
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
watch(() => [props.assignmentId, props.submissionId], load, {
  immediate: true,
});
async function save() {
  if (!work.value) return;
  busy.value = true;
  error.value = "";
  try {
    work.value = await assignmentApi.review(
      props.assignmentId,
      props.submissionId,
      {
        version: work.value.version,
        decision: decision.value,
        score: decision.value === "GRADED" ? score.value : null,
        feedback: feedback.value,
      },
    );
    notice.value = t("assignment.reviewSaved");
    emit("saved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="review-panel">
    <AsyncState :loading="loading" @retry="load">
      <template v-if="work">
        <h2>
          {{ work.studentName }} <small>({{ work.studentUsername }})</small>
        </h2>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <p v-if="notice" role="status" class="notice">
          {{ notice }}
        </p>
        <SubmissionHistory :attempts="work.attempts" />
        <form
          v-if="work.status === 'SUBMITTED' && !readonly"
          class="panel"
          @submit.prevent="save"
        >
          <h2>{{ t("assignment.review") }}</h2>
          <fieldset :disabled="busy">
            <label for="review-decision">{{
              t("assignment.reviewDecision")
            }}</label
            ><select id="review-decision" v-model="decision">
              <option value="GRADED">
                {{ t("assignment.grade") }}
              </option>
              <option value="RETURNED">
                {{ t("assignment.return") }}
              </option></select
            ><template v-if="decision === 'GRADED'">
              <label for="review-score"
                >{{ t("assignment.score") }} /
                {{ work.attempts[0]?.maxScore }}</label
              ><input
                id="review-score"
                v-model.number="score"
                type="number"
                required
                min="0"
                :max="work.attempts[0]?.maxScore"
              /> </template
            ><label for="review-feedback">{{
              t("assignment.feedbackText")
            }}</label
            ><textarea
              id="review-feedback"
              v-model="feedback"
              required
              maxlength="10000"
              rows="5"
            />
          </fieldset>
          <el-button native-type="submit" type="primary" :loading="busy">
            {{ t("assignment.publishFeedback") }}
          </el-button>
        </form>
        <p v-else class="notice">
          {{
            t(
              readonly
                ? "assignment.archivedCourse"
                : "assignment.reviewReadOnly",
            )
          }}
        </p>
      </template>
      <p v-else-if="error" role="alert">
        {{ error }}
      </p>
    </AsyncState>
  </section>
</template>
