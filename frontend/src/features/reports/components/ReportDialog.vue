<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { reportApi } from "../api";
import type { ChatMessage } from "../../chat/types";
const props = defineProps<{ message: ChatMessage }>(),
  emit = defineEmits<{ close: []; submitted: [] }>(),
  { t } = useI18n();
const reason = ref(""),
  details = ref(""),
  busy = ref(false),
  error = ref("");
function close() {
  if (!busy.value) emit("close");
}
async function submit() {
  if (busy.value) return;
  error.value = "";
  if (!reason.value) {
    error.value = t("reports.required");
    return;
  }
  busy.value = true;
  try {
    await reportApi.create(props.message.id, reason.value, details.value);
    emit("submitted");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <el-dialog
    :model-value="true"
    :title="t('reports.report')"
    width="min(32rem, 94vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    @update:model-value="close"
  >
    <p>{{ t("reports.reporting", { name: message.senderName }) }}</p>
    <p>{{ t("reports.privacy") }}</p>
    <form class="stack" :aria-busy="busy" @submit.prevent="submit">
      <fieldset :disabled="busy" class="reason-options">
        <legend>{{ t("reports.reason") }}</legend>
        <label
          v-for="r in ['HURTFUL', 'SPAM', 'INAPPROPRIATE', 'OTHER']"
          :key="r"
        ><input
           v-model="reason"
           type="radio"
           name="report-reason"
           :value="r"
         />
          {{ t("reports." + r) }}</label>
      </fieldset>
      <label for="report-details">{{ t("reports.details") }}</label><textarea
        id="report-details"
        v-model="details"
        rows="3"
        maxlength="500"
        :disabled="busy"
      ></textarea>
      <p v-if="error" role="alert" class="notice error">
        {{ error }}
      </p>
      <RouterLink v-if="error" to="/profile/reports">
        {{ t("reports.mine") }}
      </RouterLink>
      <div class="actions">
        <el-button type="primary" native-type="submit" :loading="busy">
          {{ t("reports.submit") }}
        </el-button><el-button :disabled="busy" @click="close">
          {{ t("cancel") }}
        </el-button>
      </div>
    </form>
  </el-dialog>
</template>
<style scoped>
.reason-options {
  border: 0;
  padding: 0;
  display: grid;
  gap: 0.4rem;
}
.reason-options label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 44px;
}
.reason-options input {
  width: auto;
}
</style>
