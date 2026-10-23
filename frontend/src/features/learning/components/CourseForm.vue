<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Course } from "../types";
const props = defineProps<{ course?: Course }>(),
  emit = defineEmits<{ saved: [id: number]; cancel: [] }>(),
  { t } = useI18n(),
  busy = ref(false),
  error = ref("");
const form = reactive({
  title: props.course?.title || "",
  description: props.course?.description || "",
  difficulty: props.course?.difficulty || "BEGINNER",
  estimatedMinutes: props.course?.estimatedMinutes || 30,
  languageCode: props.course?.languageCode || "en",
});
async function submit() {
  error.value = "";
  busy.value = true;
  try {
    let id = props.course?.id;
    if (id) await learningApi.updateCourse(id, form);
    else id = await learningApi.createCourse(form);
    emit("saved", id);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <form class="stack" @submit.prevent="submit">
    <label for="course-title">{{ t("courseTitle") }}</label><input
      id="course-title"
      v-model="form.title"
      required
      maxlength="150"
    /><label for="course-description">{{ t("description") }}</label><textarea
      id="course-description"
      v-model="form.description"
      maxlength="20000"
      rows="5"
    ></textarea><label for="course-language">{{ t("language") }}</label><input
      id="course-language"
      v-model="form.languageCode"
      required
      maxlength="35"
      pattern="[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*"
    />
    <label for="course-difficulty">{{ t("progress.difficulty") }}</label>
    <select id="course-difficulty" v-model="form.difficulty">
      <option
        v-for="d in ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']"
        :key="d"
        :value="d"
      >
        {{ t("progress." + d) }}
      </option>
    </select>
    <label for="course-minutes">{{ t("progress.estimatedMinutes") }}</label><input
      id="course-minutes"
      v-model.number="form.estimatedMinutes"
      type="number"
      min="1"
      max="10080"
      required
    />
    <p class="muted">
      {{ t("progress.timeHelp") }}
    </p>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <div class="actions">
      <el-button native-type="submit" type="primary" :loading="busy">
        {{ t("save") }}
      </el-button><el-button :disabled="busy" @click="emit('cancel')">
        {{ t("cancel") }}
      </el-button>
    </div>
  </form>
</template>
