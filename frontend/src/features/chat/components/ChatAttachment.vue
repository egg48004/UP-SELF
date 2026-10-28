<script setup lang="ts">
import { ref, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import type { ChatFile } from "../types";
import type { ChatApi } from "../api";
import AudioPlayer from "../../recordings/components/AudioPlayer.vue";
const props = defineProps<{ file: ChatFile; api: ChatApi }>();
const { t } = useI18n();
const image = ref(""),
  busy = ref(false),
  error = ref("");
let disposed = false;
const downloads = new Set<string>();
function loadAudio() {
  return props.api.file(props.file.id);
}
function clear() {
  if (image.value) URL.revokeObjectURL(image.value);
  image.value = "";
}
async function show() {
  busy.value = true;
  error.value = "";
  try {
    const b = await props.api.file(props.file.id);
    if (!disposed) image.value = URL.createObjectURL(b);
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    if (!disposed) busy.value = false;
  }
}
async function download() {
  busy.value = true;
  error.value = "";
  try {
    const b = await props.api.file(props.file.id);
    if (disposed) return;
    const url = URL.createObjectURL(b);
    downloads.add(url);
    const a = document.createElement("a");
    a.href = url;
    a.download = props.file.originalName;
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(url);
      downloads.delete(url);
    }, 10000);
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    if (!disposed) busy.value = false;
  }
}
onBeforeUnmount(() => {
  disposed = true;
  clear();
  downloads.forEach((url) => URL.revokeObjectURL(url));
});
</script>
<template>
  <div class="chat-file">
    <span class="filename">{{ file.originalName }}</span>
    <AudioPlayer
      v-if="file.mimeType.startsWith('audio/')"
      :source-key="'chat-' + file.id"
      :label="file.originalName"
      :load="loadAudio"
    />
    <template v-if="file.mimeType.startsWith('image/')">
      <el-button v-if="!image" :loading="busy" @click="show">
        {{
          t("chat.viewImage")
        }}
      </el-button>
      <template v-else>
        <img :src="image" :alt="file.altText" /><el-button @click="clear">
          {{
            t("chat.hideImage")
          }}
        </el-button>
      </template>
    </template>
    <el-button
      :loading="busy"
      :aria-label="t('chat.download') + ': ' + file.originalName"
      @click="download"
    >
      {{ t("chat.download") }}
    </el-button>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
  </div>
</template>
<style scoped>
.chat-file {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
  padding: 0.6rem 0;
  min-width: 0;
}
.filename {
  overflow-wrap: anywhere;
  flex: 1 1 100%;
}
img {
  display: block;
  max-width: 100%;
  max-height: 24rem;
  object-fit: contain;
  flex-basis: 100%;
}
</style>
