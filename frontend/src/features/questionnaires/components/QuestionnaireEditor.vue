<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { Questionnaire } from "../types";
import { questionnaireApi } from "../api";
import { safeQuestionnaireUrl } from "../links";
const props = defineProps<{ questionnaire?: Questionnaire }>();
const emit = defineEmits<{ saved: []; cancel: []; busy: [value: boolean] }>();
const { t } = useI18n();
const form = reactive({
  title: props.questionnaire?.title || "",
  url: props.questionnaire?.url || "",
  visible: props.questionnaire?.visible ?? false,
});
const busy = ref(false),
  error = ref("");
async function save() {
  if (busy.value) return;
  error.value = "";
  const title = form.title.trim(),
    url = form.url.trim();
  if (!title || title.length > 150) {
    error.value = t("questionnaire.requiredTitle");
    document.getElementById("questionnaire-title")?.focus();
    return;
  }
  if (!safeQuestionnaireUrl(url)) {
    error.value = t("questionnaire.invalidUrl");
    document.getElementById("questionnaire-url")?.focus();
    return;
  }
  busy.value = true;
  emit("busy", true);
  try {
    const body = { title, url, visible: form.visible };
    if (props.questionnaire)
      await questionnaireApi.update(
        props.questionnaire.id,
        body,
        props.questionnaire.version,
      );
    else await questionnaireApi.create(body);
    emit("saved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
    emit("busy", false);
  }
}
</script>
<template>
  <form class="stack" novalidate :aria-busy="busy" @submit.prevent="save">
    <label for="questionnaire-title">{{ t("questionnaire.titleLabel") }}</label>
    <input
      id="questionnaire-title"
      v-model="form.title"
      required
      maxlength="150"
      :disabled="busy"
    />
    <label for="questionnaire-url">{{ t("questionnaire.linkLabel") }}</label>
    <input
      id="questionnaire-url"
      v-model="form.url"
      type="url"
      required
      maxlength="2000"
      :disabled="busy"
      aria-describedby="questionnaire-url-help"
    />
    <p id="questionnaire-url-help" class="muted">
      {{ t("questionnaire.linkHelp") }}
    </p>
    <label class="visibility-option"><input v-model="form.visible" type="checkbox" :disabled="busy" />{{
      t("questionnaire.visibleLabel")
    }}</label>
    <p class="muted">
      {{ t("questionnaire.defaultHelp") }}
    </p>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <div class="actions">
      <el-button native-type="submit" type="primary" :loading="busy">
        {{
          t("save")
        }}
      </el-button>
      <el-button :disabled="busy" @click="emit('cancel')">
        {{
          t("cancel")
        }}
      </el-button>
    </div>
  </form>
</template>
<style scoped>
.visibility-option {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-height: 44px;
}
.visibility-option input {
  width: auto;
}
</style>
