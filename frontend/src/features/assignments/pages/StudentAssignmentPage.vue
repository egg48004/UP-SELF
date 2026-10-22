<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, onBeforeRouteLeave, onBeforeRouteUpdate } from "vue-router";
import { useI18n } from "vue-i18n";
import { useStudentWork } from "../composables/useStudentWork";
import { malaysiaDate } from "../api";
import WorkFiles from "../components/WorkFiles.vue";
import SubmissionHistory from "../components/SubmissionHistory.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const route = useRoute(),
  { t } = useI18n(),
  id = computed(() => Number(route.params.id));
const {
  assignment,
  work,
  answer,
  loading,
  busy,
  error,
  notice,
  progress,
  file,
  uploadControl,
  dirty,
  editable,
  refresh,
  save,
  upload,
  remove,
  submit,
} = useStudentWork(id);
const fileInput = ref<HTMLInputElement | null>(null);
watch(file, (value) => {
  if (!value && fileInput.value) fileInput.value.value = "";
});
async function guard() {
  if (busy.value) return false;
  return !dirty.value || (await confirmAction(t("assignment.leaveConfirm")));
}
onBeforeRouteLeave(guard);
onBeforeRouteUpdate(guard);
function selectFile(e: Event) {
  file.value = (e.target as HTMLInputElement).files?.[0] || null;
}
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/student/assignments">
      ← {{ t("assignment.back") }} </RouterLink
    ><AsyncState :loading="loading" @retry="refresh">
      <template v-if="assignment && work">
        <div class="course-banner">
          <span aria-hidden="true">📝</span>
          <div>
            <p class="eyebrow">
              {{ assignment.courseTitle }}
            </p>
            <h1>{{ assignment.title }}</h1>
            <p>
              {{
                assignment.dueAt
                  ? malaysiaDate(assignment.dueAt) + " MYT"
                  : t("assignment.noDue")
              }}
            </p>
          </div>
        </div>
        <p v-if="error" class="notice error" role="alert">
          {{ error }}
          <el-button :disabled="!!busy" @click="refresh">
            {{ t("retry") }}
          </el-button>
        </p>
        <p v-if="notice" class="notice" role="status">
          {{ notice }}
        </p>
        <section class="panel">
          <h2>{{ t("assignment.instructions") }}</h2>
          <p class="plain-text">
            {{ assignment.instructions }}
          </p>
          <p class="muted">
            {{ t("assignment.lateHelp") }}
          </p>
        </section>
        <section v-if="editable" class="panel">
          <h2>{{ t("assignment.yourAnswer") }}</h2>
          <p>{{ t("assignment.answerHelp") }}</p>
          <p v-if="work.status === 'RETURNED'" class="notice">
            {{ t("assignment.pastWork") }}
          </p>
          <label class="sr-only" for="work-answer">{{
            t("assignment.yourAnswer")
          }}</label
          ><textarea
            id="work-answer"
            v-model="answer"
            rows="6"
            maxlength="20000"
            :disabled="!!busy && busy !== 'save'"
          />
          <p class="muted">
            {{ t("assignment.draftPrivate") }} {{ t("assignment.dirtyHelp") }}
          </p>
          <p role="status">
            {{
              busy === "save"
                ? t("assignment.saving")
                : dirty
                  ? t("assignment.unsaved")
                  : ""
            }}
          </p>
          <el-button
            :disabled="!!busy || !dirty"
            :loading="busy === 'save'"
            @click="save"
          >
            {{ t("assignment.saveDraft") }}
          </el-button>
          <hr />
          <h3>{{ t("assignment.files") }}</h3>
          <p>{{ t("assignment.fileHelp") }}</p>
          <WorkFiles
            :files="work.files"
            removable
            :disabled="!!busy"
            @remove="remove"
          /><label for="work-file">{{ t("assignment.chooseFile") }}</label
          ><input
            id="work-file"
            ref="fileInput"
            :key="work.version"
            type="file"
            accept=".pdf,.docx,.png,.jpg,.jpeg,.txt"
            :disabled="!!busy || work.files.length >= 5"
            @change="selectFile"
          />
          <div class="button-row">
            <el-button
              :disabled="!file || !!busy || work.files.length >= 5"
              @click="upload"
            >
              {{ t("assignment.upload") }} </el-button
            ><el-button
              v-if="busy === 'upload'"
              @click="uploadControl?.abort()"
            >
              {{ t("assignment.cancelUpload") }}
            </el-button>
          </div>
          <p v-if="busy === 'upload'" role="status">
            {{ t("assignment.uploading", { progress }) }}
          </p>
          <hr />
          <el-button
            type="primary"
            :disabled="!!busy"
            :loading="busy === 'submit'"
            @click="submit"
          >
            {{ t("assignment.submit") }}
          </el-button>
        </section>
        <section v-else class="panel">
          <p>
            {{
              t(
                work.status === "GRADED" || work.status === "RETURNED"
                  ? "assignment.feedbackPrompt"
                  : "assignment.waiting",
              )
            }}
          </p>
          <el-button @click="refresh">
            {{ t("assignment.refresh") }}
          </el-button>
        </section>
        <SubmissionHistory :attempts="work.attempts" />
      </template>
      <div v-else-if="error">
        <p role="alert" class="notice error">
          {{ error }}
        </p>
        <el-button @click="refresh">
          {{ t("retry") }}
        </el-button>
      </div>
    </AsyncState>
  </section>
</template>
