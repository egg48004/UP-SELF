<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { goalApi } from "../api";
import type { PersonalGoal } from "../types";

const props = defineProps<{ goal?: PersonalGoal }>(),
  emit = defineEmits<{
    saved: [];
    cancel: [];
    busy: [value: boolean];
  }>(),
  { t } = useI18n(),
  busy = ref(false),
  error = ref("");
const form = reactive({
  title: props.goal?.title || "",
  details: props.goal?.details || "",
});
async function save() {
  if (busy.value) return;
  error.value = "";
  if (!form.title.trim()) {
    error.value = t("goal.required");
    document.getElementById("goal-title")?.focus();
    return;
  }
  busy.value = true;
  emit("busy", true);
  try {
    const body = { title: form.title.trim(), details: form.details.trim() };
    if (props.goal) await goalApi.edit(props.goal.id, body, props.goal.version);
    else await goalApi.create(body);
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
  <form class="stack" :aria-busy="busy" @submit.prevent="save">
    <label for="goal-title">{{ t("goal.goalTitle") }}</label>
    <input
      id="goal-title"
      v-model="form.title"
      required
      maxlength="150"
      :disabled="busy"
      aria-describedby="goal-title-help"
    />
    <p id="goal-title-help" class="muted">
      {{ t("goal.titleHelp") }}
    </p>
    <label for="goal-details">{{ t("goal.details") }}</label>
    <textarea
      id="goal-details"
      v-model="form.details"
      rows="4"
      maxlength="2000"
      :disabled="busy"
    ></textarea>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <div class="actions">
      <el-button native-type="submit" type="primary" :loading="busy">
        {{ t("save") }}
      </el-button>
      <el-button :disabled="busy" @click="emit('cancel')">
        {{
          t("cancel")
        }}
      </el-button>
    </div>
  </form>
</template>
