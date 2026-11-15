<script setup lang="ts">
import { reactive, ref, watch, computed } from "vue";
import { useI18n } from "vue-i18n";
import { marketApi } from "../api";
import type { TaskContent, MarketTask } from "../types";
import { confirmAction } from "../../../shared/ui/confirm";
const visible = defineModel<boolean>({ required: true });
const props = defineProps<{ courseId: number; task?: MarketTask }>();
const emit = defineEmits<{ saved: [id: number] }>();
const { t } = useI18n();
const blank = (): TaskContent => ({
  courseId: props.courseId,
  title: "",
  objective: "",
  situation: "",
  question: "",
  reflectionPrompt: "What would you try differently?",
  version: 0,
  options: [
    { label: "", outcome: "", explanation: "" },
    { label: "", outcome: "", explanation: "" },
  ],
});
const form = reactive<TaskContent>(blank()),
  busy = ref(false),
  error = ref(""),
  baseline = ref("");
const dirty = computed(() => JSON.stringify(form) !== baseline.value);
watch(
  visible,
  (open) => {
    if (open) {
      Object.assign(
        form,
        blank(),
        props.task ? JSON.parse(JSON.stringify(props.task)) : {},
      );
      error.value = "";
      baseline.value = JSON.stringify(form);
    }
  },
  { immediate: true },
);
async function close(done?: () => void) {
  if (busy.value) return;
  if (dirty.value && !(await confirmAction(t("market.leave")))) return;
  visible.value = false;
  done?.();
}
async function save() {
  if (busy.value) return;
  const keys = [
    "title",
    "objective",
    "situation",
    "question",
    "reflectionPrompt",
  ] as const;
  const invalid = keys.find((k) => !form[k].trim());
  if (
    invalid ||
    form.options.some(
      (o) => !o.label.trim() || !o.outcome?.trim() || !o.explanation?.trim(),
    )
  ) {
    error.value = t("market.required");
    document.getElementById("market-" + (invalid || "option-0"))?.focus();
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    const id = props.task
      ? (await marketApi.update(props.task.id, form), props.task.id)
      : await marketApi.create(form);
    visible.value = false;
    emit("saved", id);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <el-dialog
    v-model="visible"
    :title="t(task ? 'market.edit' : 'market.new')"
    width="720px"
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :before-close="close"
  >
    <form novalidate @submit.prevent="save">
      <fieldset :disabled="busy">
        <label for="market-title">{{ t("market.taskTitle") }}</label><input
          id="market-title"
          v-model="form.title"
          maxlength="150"
          required
        />
        <label for="market-objective">{{ t("market.goal") }}</label><input
          id="market-objective"
          v-model="form.objective"
          maxlength="300"
          required
        />
        <label for="market-situation">{{ t("market.situation") }}</label><textarea
          id="market-situation"
          v-model="form.situation"
          rows="4"
          maxlength="2000"
          required
        />
        <label for="market-question">{{ t("market.question") }}</label><input
          id="market-question"
          v-model="form.question"
          maxlength="300"
          required
        />
        <h3>{{ t("market.options") }}</h3>
        <p>{{ t("market.optionHelp") }}</p>
        <fieldset v-for="(option, i) in form.options" :key="i" class="panel">
          <legend>{{ t("market.option", { number: i + 1 }) }}</legend>
          <label :for="'market-option-' + i">{{ t("market.label") }}</label><input
            :id="'market-option-' + i"
            v-model="option.label"
            required
            maxlength="300"
          />
          <label :for="'market-outcome-' + i">{{ t("market.outcome") }}</label><textarea
            :id="'market-outcome-' + i"
            v-model="option.outcome"
            rows="3"
            required
            maxlength="1000"
          />
          <label :for="'market-why-' + i">{{ t("market.why") }}</label><textarea
            :id="'market-why-' + i"
            v-model="option.explanation"
            rows="3"
            required
            maxlength="1000"
          />
        </fieldset>
        <el-button
          v-if="form.options.length === 2"
          @click="
            form.options.push({ label: '', outcome: '', explanation: '' })
          "
        >
          {{ t("market.add") }}
        </el-button>
        <el-button v-else @click="form.options.pop()">
          {{
            t("market.remove")
          }}
        </el-button>
        <label for="market-reflectionPrompt">{{ t("market.prompt") }}</label><input
          id="market-reflectionPrompt"
          v-model="form.reflectionPrompt"
          required
          maxlength="300"
        />
      </fieldset>
      <p v-if="error" role="alert" class="notice error">
        {{ error }}
      </p>
      <div class="button-row">
        <el-button :disabled="busy" @click="close()">
          {{
            t("market.cancel")
          }}
        </el-button><el-button native-type="submit" type="primary" :loading="busy">
          {{
            t("market.save")
          }}
        </el-button>
      </div>
    </form>
  </el-dialog>
</template>
