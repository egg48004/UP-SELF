<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { accessibilityApi } from "../api";
import type { PlatformDefaults } from "../api";
import { usePreferences } from "../store";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n(),
  preferences = usePreferences(),
  form = ref<PlatformDefaults>(),
  loading = ref(false),
  saving = ref(false),
  loadError = ref(""),
  error = ref(""),
  notice = ref("");
let sequence = 0;
async function load() {
  const seq = ++sequence;
  loading.value = true;
  loadError.value = "";
  error.value = "";
  notice.value = "";
  try {
    const r = await accessibilityApi.read();
    if (seq !== sequence) return;
    preferences.acceptDefaults(r);
    form.value = { ...r };
  } catch (e) {
    if (seq === sequence) loadError.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
async function save() {
  if (saving.value || !form.value) return;
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    const r = await accessibilityApi.save({ ...form.value });
    form.value = { ...r };
    preferences.acceptDefaults(r);
    notice.value = t("readingDefaults.saved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="panel narrow">
    <RouterLink to="/admin/home">
      ← {{ t("readingDefaults.back") }}
    </RouterLink>
    <h1>{{ t("readingDefaults.title") }}</h1>
    <p>{{ t("readingDefaults.intro") }}</p>
    <p>{{ t("readingDefaults.personalPriority") }}</p>
    <AsyncState :loading="loading" :error="loadError" @retry="load">
      <form v-if="form" @submit.prevent="save" @change="notice = ''">
        <label for="default-size">{{ t("textSize") }}</label><select id="default-size" v-model="form.size" :disabled="saving">
          <option
            v-for="s in ['standard', 'large', 'extraLarge']"
            :key="s"
            :value="s"
          >
            {{ t(s) }}
          </option>
        </select>
        <label for="default-contrast">{{ t("contrast") }}</label><select
          id="default-contrast"
          v-model="form.contrast"
          :disabled="saving"
        >
          <option value="standard">
            {{ t("standard") }}
          </option>
          <option value="highContrast">
            {{ t("highContrast") }}
          </option>
        </select>
        <label class="check"><input v-model="form.motion" type="checkbox" :disabled="saving" />{{
          t("readingDefaults.motion")
        }}</label>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <p v-if="notice" role="status" class="notice">
          {{ notice }}
        </p>
        <div class="actions">
          <el-button type="primary" native-type="submit" :disabled="saving">
            {{ t("readingDefaults.save") }}
          </el-button><el-button :disabled="saving" @click="load">
            {{ t("readingDefaults.reload") }}
          </el-button>
        </div>
      </form>
    </AsyncState>
  </section>
</template>
