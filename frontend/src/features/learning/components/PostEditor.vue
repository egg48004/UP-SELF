<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { Post, PostBody } from "../types";
const props = defineProps<{ courseId: number; post?: Post }>(),
  emit = defineEmits<{ saved: []; cancel: [] }>(),
  { t } = useI18n(),
  busy = ref(false),
  error = ref("");
const form = reactive<PostBody>({
  title: props.post?.title || "",
  body: props.post?.body || "",
  type: props.post?.type || "ANNOUNCEMENT",
  meetingUrl: props.post?.meetingUrl || "",
  meetingStartsAt: props.post?.meetingStartsAt || null,
  status: props.post?.status || "DRAFT",
  languageCode: props.post?.languageCode || "en",
});
const time = ref(
  form.meetingStartsAt
    ? new Date(new Date(form.meetingStartsAt).getTime() + 8 * 3600000)
        .toISOString()
        .slice(0, 16)
    : "",
);
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    const b = {
      ...form,
      meetingStartsAt: time.value ? time.value + ":00+08:00" : null,
      meetingUrl: form.meetingUrl || null,
    };
    if (props.post) await learningApi.updatePost(props.post.id, b);
    else await learningApi.createPost(props.courseId, b);
    emit("saved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <form class="stack" @submit.prevent="submit">
    <label for="post-title">{{ t("postTitle") }}</label><input
      id="post-title"
      v-model="form.title"
      required
      maxlength="150"
    /><label for="post-body">{{ t("body") }}</label><textarea
      id="post-body"
      v-model="form.body"
      required
      maxlength="20000"
      rows="5"
    ></textarea><label for="post-type">{{ t("postType") }}</label><select id="post-type" v-model="form.type">
      <option value="ANNOUNCEMENT">
        {{ t("announcement") }}
      </option>
      <option value="MEETING">
        {{ t("meeting") }}
      </option>
    </select><label for="meeting-url">{{ t("meetingUrl") }}</label><input
      id="meeting-url"
      v-model="form.meetingUrl"
      type="url"
      maxlength="2000"
      pattern="https://.*"
      :required="form.type === 'MEETING'"
    /><label for="meeting-time">{{ t("meetingTime") }}</label><input id="meeting-time" v-model="time" type="datetime-local" /><label
      for="post-status"
    >{{ t("status") }}</label><select id="post-status" v-model="form.status">
      <option value="DRAFT">
        {{ t("draft") }}
      </option>
      <option value="PUBLISHED">
        {{ t("published") }}
      </option>
    </select><label for="post-language">{{ t("language") }}</label><input
      id="post-language"
      v-model="form.languageCode"
      required
      maxlength="35"
      pattern="[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*"
    />
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <div class="actions">
      <el-button native-type="submit" type="primary" :loading="busy">
        {{
          t("save")
        }}
      </el-button><el-button :disabled="busy" @click="emit('cancel')">
        {{
          t("cancel")
        }}
      </el-button>
    </div>
  </form>
</template>
