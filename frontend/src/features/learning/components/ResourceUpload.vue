<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Resource, ResourceBody } from "../types";
import ResourceFields from "./ResourceFields.vue";
const props = defineProps<{ chapterId: number; resources: Resource[] }>(),
  emit = defineEmits<{ saved: [] }>(),
  { t } = useI18n(),
  kind = ref<"IMAGE" | "VIDEO" | "CAPTION">("IMAGE"),
  file = ref<File | null>(null),
  input = ref<HTMLInputElement | null>(null),
  busy = ref(false),
  progress = ref(0),
  error = ref("");
const form = reactive<ResourceBody>({
    title: "",
    altText: "",
    captionResourceId: null,
    transcript: "",
    languageCode: "en",
    status: "DRAFT",
  }),
  captions = computed(() =>
    props.resources.filter((r) => r.kind === "CAPTION"),
  ),
  accept = computed(() =>
    kind.value === "IMAGE"
      ? ".png,.jpg,.jpeg,.webp"
      : kind.value === "VIDEO"
        ? ".mp4"
        : ".vtt",
  );
let controller: AbortController | null = null;
function selected(e: Event) {
  file.value = (e.target as HTMLInputElement).files?.[0] || null;
}
async function submit() {
  error.value = "";
  if (!file.value) {
    error.value = t("selectFile");
    return;
  }
  const max = { IMAGE: 10, VIDEO: 100, CAPTION: 1 }[kind.value] * 1024 * 1024;
  if (file.value.size > max) {
    error.value = t("fileTooLarge");
    return;
  }
  const allowed =
    kind.value === "IMAGE"
      ? /\.(jpe?g|png|webp)$/i
      : kind.value === "VIDEO"
        ? /\.mp4$/i
        : /\.vtt$/i;
  if (!allowed.test(file.value.name)) {
    error.value = t("invalidFile");
    return;
  }
  busy.value = true;
  progress.value = 0;
  controller = new AbortController();
  try {
    const fd = new FormData();
    fd.append("file", file.value);
    fd.append("kind", kind.value);
    for (const [k, v] of Object.entries(form))
      if (v !== null) fd.append(k, String(v));
    await learningApi.upload(
      props.chapterId,
      fd,
      (n) => (progress.value = n),
      controller.signal,
    );
    file.value = null;
    if (input.value) input.value.value = "";
    form.title = "";
    form.altText = "";
    form.transcript = "";
    form.captionResourceId = null;
    emit("saved");
  } catch (e) {
    error.value = controller.signal.aborted
      ? t("cancel")
      : (e as Error).message;
  } finally {
    busy.value = false;
  }
}
function cancel() {
  controller?.abort();
}
onUnmounted(cancel);
</script>
<template>
  <form class="stack" @submit.prevent="submit">
    <p class="muted">
      {{ t("uploadHelp") }}
    </p>
    <label for="upload-kind">{{ t("kind") }}</label><select id="upload-kind" v-model="kind" :disabled="busy">
      <option value="IMAGE">
        {{ t("image") }}
      </option>
      <option value="VIDEO">
        {{ t("video") }}
      </option>
      <option value="CAPTION">
        {{ t("caption") }}
      </option>
    </select><label for="upload-file">{{ t("file") }}</label><input
      id="upload-file"
      ref="input"
      type="file"
      :accept="accept"
      :disabled="busy"
      required
      @change="selected"
    /><ResourceFields
      v-model="form"
      :kind="kind"
      :captions="captions"
      prefix="upload"
    />
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <p v-if="busy" role="status">
      {{ t("uploading") }} {{ progress }}%
    </p>
    <progress
      v-if="busy"
      :value="progress"
      max="100"
      :aria-label="t('uploading')"
    ></progress>
    <div class="actions">
      <el-button native-type="submit" type="primary" :loading="busy">
        {{
          t("upload")
        }}
      </el-button><el-button v-if="busy" @click="cancel">
        {{ t("cancel") }}
      </el-button>
    </div>
  </form>
</template>
