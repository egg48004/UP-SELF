<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import type { ChatApi } from "../api";
import type { ChatMember } from "../types";
import { confirmAction } from "../../../shared/ui/confirm";
const props = defineProps<{
  id: number;
  api: ChatApi;
  manager: boolean;
  closed: boolean;
}>();
const { t } = useI18n();
const members = ref<ChatMember[]>([]),
  candidates = ref<ChatMember[]>([]),
  selected = ref(""),
  busy = ref(false),
  error = ref("");
const available = computed(() =>
  candidates.value.filter(
    (c) => !members.value.some((m) => m.studentId === c.studentId),
  ),
);
async function load() {
  try {
    members.value = await props.api.members(props.id);
    if (props.manager) candidates.value = await props.api.candidates(props.id);
  } catch (e) {
    error.value = (e as Error).message;
  }
}
async function add() {
  if (!selected.value) return;
  busy.value = true;
  error.value = "";
  try {
    await props.api.add(props.id, Number(selected.value));
    selected.value = "";
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function remove(id: number) {
  if (!(await confirmAction(t("chat.removeConfirm")))) return;
  busy.value = true;
  error.value = "";
  try {
    await props.api.remove(props.id, id);
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section>
    <p>{{ t("chat.mentorIncluded") }}</p>
    <p v-if="manager">
      {{ t("chat.memberHelp") }}
    </p>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <form v-if="manager && !closed" class="stack" @submit.prevent="add">
      <label for="chat-student">{{ t("chat.choose") }}</label><select id="chat-student" v-model="selected" :disabled="busy">
        <option value="">
          {{ t("chat.choose") }}
        </option>
        <option
          v-for="s in available"
          :key="s.studentId"
          :value="String(s.studentId)"
        >
          {{ s.nickName }} ({{ s.userName }})
        </option>
      </select><el-button native-type="submit" :disabled="!selected" :loading="busy">
        {{
          t("chat.add")
        }}
      </el-button>
    </form>
    <ul class="content-list">
      <li v-for="m in members" :key="m.studentId">
        <span>{{ m.nickName }} <small>{{ m.userName }}</small></span><el-button
          v-if="manager"
          :disabled="busy"
          :aria-label="t('chat.remove') + ': ' + m.nickName"
          @click="remove(m.studentId)"
        >
          {{ t("chat.remove") }}
        </el-button>
      </li>
    </ul>
    <p v-if="!members.length">
      {{ t("chat.noMembers") }}
    </p>
  </section>
</template>
