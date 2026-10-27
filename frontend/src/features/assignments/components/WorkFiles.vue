<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { assignmentApi } from "../api";
import type { WorkFile } from "../types";
import AudioPlayer from "../../recordings/components/AudioPlayer.vue";
defineProps<{ files: WorkFile[]; removable?: boolean; disabled?: boolean }>();
defineEmits<{ remove: [id: number] }>();
const { t } = useI18n(),
  busy = ref<number | null>(null),
  error = ref("");
async function download(file: WorkFile) {
  busy.value = file.id;
  error.value = "";
  let url = "";
  try {
    const blob = await assignmentApi.download(file.id);
    url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = file.originalName;
    link.click();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = null;
    if (url) setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
function audioLoader(id: number) { return () => assignmentApi.download(id); }
</script>
<template>
  <div>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <p v-if="!files.length" class="muted">
      {{ t("assignment.noFiles") }}
    </p>
    <ul class="work-files">
      <li v-for="f in files" :key="f.id">
        <span>📎 {{ f.originalName }}
          <small>({{ Math.ceil(f.fileSize / 1024) }} KB)</small></span>
        <div class="button-row">
          <AudioPlayer v-if="f.mimeType.startsWith('audio/')" :source-key="'assignment-' + f.id" :label="f.originalName" :load="audioLoader(f.id)" />
          <el-button
            :loading="busy === f.id"
            :aria-label="t('assignment.download') + ': ' + f.originalName"
            @click="download(f)"
          >
            {{ t("assignment.download") }}
          </el-button><el-button
            v-if="removable"
            :disabled="disabled"
            :aria-label="t('assignment.remove') + ': ' + f.originalName"
            @click="$emit('remove', f.id)"
          >
            {{ t("assignment.remove") }}
          </el-button>
        </div>
      </li>
    </ul>
  </div>
</template>
<style scoped>
.work-files .button-row { width: 100%; min-width: 0; flex-wrap: wrap; }
.work-files li > span { overflow-wrap: anywhere; max-width: 100%; }
</style>
