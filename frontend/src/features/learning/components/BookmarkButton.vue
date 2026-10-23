<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { bookmarkApi } from "../bookmarks";

const props = defineProps<{ courseId: number; saved: boolean; title?: string }>(),
  emit = defineEmits<{ changed: [saved: boolean] }>(),
  { t } = useI18n(),
  auth = useAuthStore(),
  busy = ref(false),
  error = ref(""),
  notice = ref("");
async function toggle() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  const next = !props.saved;
  try {
    if (next) await bookmarkApi.save(props.courseId);
    else await bookmarkApi.remove(props.courseId);
    emit("changed", next);
    notice.value = t(next ? "bookmark.saved" : "bookmark.removed");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div v-if="auth.isStudent && auth.can('learning:bookmark:own')" class="bookmark-control">
    <el-button
      :aria-pressed="saved"
      :aria-label="t(saved ? 'bookmark.remove' : 'bookmark.save') + ': ' + (title || t('bookmark.unavailable'))"
      :loading="busy"
      :disabled="busy"
      @click="toggle"
    >
      <span aria-hidden="true">{{ saved ? "★" : "☆" }} </span>
      {{ t(saved ? "bookmark.remove" : "bookmark.save") }}
    </el-button>
    <p v-if="notice" class="muted" role="status" aria-live="polite">
      {{ notice }}
    </p>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
  </div>
</template>
<style scoped>
.bookmark-control { margin-top: .75rem; }
.bookmark-control p { margin-bottom: 0; }
.bookmark-control span[aria-hidden="true"] { margin-right: .3rem; }
</style>
