<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { onBeforeRouteLeave } from "vue-router";
import { questionnaireApi } from "../api";
import type { Questionnaire } from "../types";
import { useQuestionnaireList } from "../useQuestionnaireList";
import QuestionnaireEditor from "../components/QuestionnaireEditor.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n();
const {
  rows,
  current,
  total,
  loading,
  error: loadError,
  load,
} = useQuestionnaireList(questionnaireApi.manage);
const busy = ref(false),
  saving = ref(false),
  editing = ref(false),
  selected = ref<Questionnaire>(),
  error = ref(""),
  notice = ref("");
function edit(item?: Questionnaire) {
  selected.value = item;
  error.value = "";
  notice.value = "";
  editing.value = true;
}
async function saved() {
  editing.value = false;
  notice.value = t("questionnaire.saved");
  await load();
}
async function visibility(item: Questionnaire) {
  if (busy.value || loading.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    await questionnaireApi.visibility(item.id, !item.visible, item.version);
    notice.value = t(
      item.visible ? "questionnaire.hiddenNotice" : "questionnaire.shown",
    );
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function remove(item: Questionnaire) {
  if (busy.value || loading.value) return;
  busy.value = true;
  try {
    if (!(await confirmAction(t("questionnaire.deleteConfirm")))) return;
    error.value = "";
    notice.value = "";
    await questionnaireApi.remove(item.id, item.version);
    notice.value = t("questionnaire.deleted");
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onBeforeRouteLeave(
  async () =>
    !busy.value &&
    !saving.value &&
    (!editing.value || (await confirmAction(t("questionnaire.leave")))),
);
</script>
<template>
  <section>
    <p class="eyebrow">
      {{ t("dashboard.mentor") }}
    </p>
    <h1>{{ t("questionnaire.title") }}</h1>
    <p>{{ t("questionnaire.manageIntro") }}</p>
    <p class="muted">
      {{ t("questionnaire.visibilityHelp") }}
    </p>
    <div class="actions">
      <el-button type="primary" :disabled="busy || loading" @click="edit()">
        {{
          t("questionnaire.add")
        }}
      </el-button>
      <el-button :disabled="busy || loading" @click="load()">
        {{
          t("questionnaire.refresh")
        }}
      </el-button>
    </div>
    <p v-if="notice" role="status">
      {{ notice }}
    </p>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <AsyncState :loading="loading" :error="loadError" @retry="load()">
      <p v-if="!rows.length" class="panel">
        {{ t("questionnaire.emptyManage") }}
      </p>
      <ul v-else class="questionnaire-list">
        <li v-for="item in rows" :key="item.id" class="panel">
          <h2>{{ item.title }}</h2>
          <p>{{ item.url }}</p>
          <p class="questionnaire-state">
            {{
              t(item.visible ? "questionnaire.visible" : "questionnaire.hidden")
            }}
          </p>
          <div class="actions">
            <el-button :disabled="busy" @click="edit(item)">
              {{
                t("edit")
              }}
            </el-button>
            <el-button :disabled="busy" @click="visibility(item)">
              {{
                t(item.visible ? "questionnaire.hide" : "questionnaire.show")
              }}
            </el-button>
            <el-button :disabled="busy" @click="remove(item)">
              {{
                t("questionnaire.delete")
              }}
            </el-button>
          </div>
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
      v-model="editing"
      :title="t(selected ? 'questionnaire.edit' : 'questionnaire.add')"
      width="min(620px,95vw)"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <QuestionnaireEditor
        v-if="editing"
        :questionnaire="selected"
        @busy="saving = $event"
        @saved="saved"
        @cancel="editing = false"
      />
    </el-dialog>
  </section>
</template>
<style scoped>
.questionnaire-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}
.questionnaire-list li {
  overflow-wrap: anywhere;
}
.questionnaire-state {
  font-weight: 700;
}
</style>
