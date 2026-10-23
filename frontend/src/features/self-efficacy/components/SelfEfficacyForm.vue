<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import type { EfficacyCurrent } from "../types";
import { efficacyApi } from "../api";
import { confirmAction } from "../../../shared/ui/confirm";

const props = defineProps<{ current: EfficacyCurrent }>();
const emit = defineEmits<{ saved: [current: EfficacyCurrent]; busy: [value: boolean] }>();
const { t } = useI18n(), router = useRouter(), step = ref(0), answers = ref<(number | null)[]>([null, null, null]), busy = ref(false), error = ref(""), heading = ref<HTMLElement | null>(null);
const dirty = computed(() => answers.value.some(value => value !== null));
const complete = computed(() => answers.value.every(value => value !== null));
async function canLeave() { return !busy.value && (!dirty.value || await confirmAction(t("efficacy.leave"))); }
function reset() { answers.value = [null, null, null]; step.value = 0; error.value = ""; }
defineExpose({ canLeave, reset });
onBeforeRouteLeave(canLeave);
function beforeUnload(event: BeforeUnloadEvent) { if (dirty.value || busy.value) { event.preventDefault(); event.returnValue = ""; } }
onMounted(() => window.addEventListener("beforeunload", beforeUnload));
onBeforeUnmount(() => window.removeEventListener("beforeunload", beforeUnload));
async function move(direction: number) {
  if (busy.value || (direction > 0 && answers.value[step.value] === null)) return;
  step.value = Math.max(0, Math.min(2, step.value + direction));
  await nextTick(); heading.value?.focus();
}
async function submit() {
  if (step.value < 2) return move(1);
  if (!complete.value || busy.value) return;
  busy.value = true; emit("busy", true); error.value = "";
  try {
    if (!await confirmAction(t("efficacy.confirm"))) return;
    const result = await efficacyApi.record(props.current.today, props.current.instrument.version, answers.value as number[]);
    reset(); emit("saved", result);
  } catch (e) { error.value = (e as Error).message; }
  finally { busy.value = false; emit("busy", false); }
}
</script>
<template>
  <form class="efficacy-form panel" @submit.prevent="submit">
    <p v-if="step === 0">
      {{ current.instrument.instruction }}
    </p>
    <p role="status" aria-live="polite">
      {{ t("efficacy.statement", { number: step + 1 }) }}
    </p>
    <fieldset :disabled="busy">
      <legend ref="heading" tabindex="-1" class="efficacy-question">
        {{ current.instrument.questions[step] }}
      </legend>
      <p>{{ t("efficacy.choose") }}</p>
      <div class="efficacy-choices">
        <label v-for="(response, index) in current.instrument.responses" :key="index" :class="['efficacy-choice', { chosen: answers[step] === index + 1 }]">
          <input v-model="answers[step]" type="radio" :name="'efficacy-answer-' + step" :value="index + 1" required />
          <span>{{ response }}</span>
        </label>
      </div>
    </fieldset>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <div class="actions">
      <el-button :disabled="busy || step === 0" @click="move(-1)">
        {{ t("previous") }}
      </el-button>
      <el-button v-if="step < 2" type="primary" :disabled="busy || answers[step] === null" @click="move(1)">
        {{ t("next") }}
      </el-button>
      <el-button v-else native-type="submit" type="primary" :disabled="busy || !complete" :loading="busy">
        {{ t("efficacy.save") }}
      </el-button>
      <el-button :disabled="busy" @click="router.push('/student/home')">
        {{ t("mood.notNow") }}
      </el-button>
    </div>
  </form>
</template>
