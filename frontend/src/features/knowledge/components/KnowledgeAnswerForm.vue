<script setup lang="ts">
import { computed, onUnmounted, ref, useId, watch } from "vue";
import { useI18n } from "vue-i18n";
import { knowledgeApi } from "../api";
import type {
  KnowledgeAnswer,
  KnowledgeChoice,
  KnowledgeQuestion,
} from "../types";
import { knowledgeChoices } from "../presentation";
const props = defineProps<{ question: KnowledgeQuestion }>();
const emit = defineEmits<{ saved: [answer: KnowledgeAnswer] }>();
const { t } = useI18n(),
  uid = useId(),
  choice = ref<KnowledgeChoice | null>(null),
  busy = ref(false),
  error = ref(""),
  notice = ref("");
let disposed = false;
const savedChoice = computed(() =>
  knowledgeChoices.find((item) => item.value === props.question.choice),
);
watch(
  () => [props.question.id, props.question.version],
  () => {
    choice.value = props.question.choice || null;
    error.value = "";
    notice.value = "";
  },
  { immediate: true },
);
async function save() {
  if (!choice.value || busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    const result = await knowledgeApi.answer(
      props.question.id,
      choice.value,
      props.question.version,
    );
    if (!disposed) {
      emit("saved", result);
      notice.value = t("knowledge.saved");
    }
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onUnmounted(() => {
  disposed = true;
});
</script>
<template>
  <form class="panel knowledge-answer" @submit.prevent="save">
    <p class="eyebrow">
      {{ question.courseTitle }}
    </p>
    <fieldset :disabled="busy">
      <legend>{{ question.questionText }}</legend>
      <div class="knowledge-choices">
        <label
          v-for="item in knowledgeChoices"
          :key="item.value"
          :class="['knowledge-choice', { chosen: choice === item.value }]"
        >
          <input
            v-model="choice"
            type="radio"
            :name="uid"
            :value="item.value"
            required
          />
          <span aria-hidden="true" class="choice-icon">{{ item.icon }}</span><span>{{ t(item.key) }}</span>
        </label>
      </div>
    </fieldset>
    <p v-if="savedChoice" class="muted">
      {{ t("knowledge.previousAnswer", { answer: t(savedChoice.key) }) }}
    </p>
    <el-button
      native-type="submit"
      type="primary"
      :loading="busy"
      :disabled="!choice || busy || choice === question.choice"
    >
      {{ t("knowledge.save") }}
    </el-button>
    <p v-if="notice" role="status">
      {{ notice }}
    </p>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
  </form>
</template>
<style scoped>
fieldset {
  border: 0;
  padding: 0;
  margin: 0;
  min-width: 0;
}
legend {
  font-size: 1.3rem;
  font-weight: 650;
  overflow-wrap: anywhere;
}
.knowledge-choices {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-block: 1rem;
}
.knowledge-choice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 2px solid var(--border);
  border-radius: 0.65rem;
  padding: 0.75rem 1rem;
  min-height: 48px;
  cursor: pointer;
}
.knowledge-choice.chosen {
  background: var(--tint);
  border-color: var(--primary);
}
.knowledge-choice:focus-within {
  outline: 3px solid var(--focus);
  outline-offset: 3px;
}
.knowledge-choice input {
  width: 20px;
  height: 20px;
}
.choice-icon {
  font-size: 1.3rem;
  font-weight: 700;
}
</style>
