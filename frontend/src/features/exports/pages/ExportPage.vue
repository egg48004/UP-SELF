<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { download } from "../../../shared/http/client";
import { learningApi } from "../../learning/api";
import type { Course } from "../../learning/types";
const { t } = useI18n(),
  type = ref("users"),
  keyword = ref(""),
  role = ref(""),
  status = ref(""),
  courseId = ref(""),
  courses = ref<Course[]>([]),
  busy = ref(false),
  error = ref(""),
  notice = ref("");
watch(type, () => {
  role.value = "";
  status.value = "";
  courseId.value = "";
  notice.value = "";
  error.value = "";
});
async function loadCourses() {
  try {
    courses.value = (
      await learningApi.courses({ current: 1, pageSize: 100 })
    ).rows.filter((c) => c.status === "PUBLISHED");
  } catch (e) {
    error.value = (e as Error).message;
  }
}
async function exportCsv() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    const params = new URLSearchParams({
      keyword: keyword.value,
      role: role.value,
      status: status.value,
      ...(courseId.value ? { courseId: courseId.value } : {}),
    });
    const blob = await download("/exports/" + type.value + "?" + params);
    const url = URL.createObjectURL(blob),
      link = document.createElement("a");
    link.href = url;
    link.download = "up-self-" + type.value + ".csv";
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    notice.value = t("exports.done");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onMounted(loadCourses);
</script>
<template>
  <section class="panel narrow">
    <h1>{{ t("exports.title") }}</h1>
    <p>{{ t("exports.intro") }}</p>
    <form class="stack" :aria-busy="busy" @submit.prevent="exportCsv">
      <label for="export-type">{{ t("exports.type") }}</label><select id="export-type" v-model="type" :disabled="busy">
        <option
          v-for="v in ['users', 'courses', 'learning']"
          :key="v"
          :value="v"
        >
          {{ t("exports." + v) }}
        </option>
      </select><label for="export-keyword">{{ t("exports.keyword") }}</label><input
        id="export-keyword"
        v-model="keyword"
        maxlength="100"
        :disabled="busy"
      />
      <template v-if="type === 'users'">
        <label for="export-role">{{ t("exports.role") }}</label><select id="export-role" v-model="role" :disabled="busy">
          <option value="">
            {{ t("exports.all") }}
          </option>
          <option
            v-for="r in ['student', 'mentor', 'platform_admin']"
            :key="r"
            :value="r"
          >
            {{ t("exports." + r) }}
          </option>
        </select>
      </template>
      <template v-if="type !== 'learning'">
        <label for="export-status">{{ t("exports.status") }}</label><select id="export-status" v-model="status" :disabled="busy">
          <option value="">
            {{ t("exports.all") }}
          </option>
          <template v-if="type === 'users'">
            <option value="0">
              {{ t("exports.active") }}
            </option>
            <option value="1">
              {{ t("exports.disabled") }}
            </option>
          </template><template v-else>
            <option
              v-for="s in ['DRAFT', 'PUBLISHED', 'ARCHIVED']"
              :key="s"
              :value="s"
            >
              {{ t("exports." + s) }}
            </option>
          </template>
        </select>
      </template>
      <template v-else>
        <label for="export-course">{{ t("exports.course") }}</label><select id="export-course" v-model="courseId" :disabled="busy">
          <option value="">
            {{ t("exports.all") }}
          </option>
          <option v-for="c in courses" :key="c.id" :value="String(c.id)">
            {{ c.title }}
          </option>
        </select>
      </template>
      <p class="muted">
        {{ t("exports.privacy") }}
      </p>
      <p v-if="error" role="alert" class="notice error">
        {{ error }}
      </p>
      <p v-if="notice" role="status" class="notice">
        {{ notice }}
      </p>
      <el-button type="primary" native-type="submit" :loading="busy">
        {{ t("exports.download") }}
      </el-button>
    </form>
  </section>
</template>
