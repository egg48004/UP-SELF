<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { onBeforeRouteLeave, onBeforeRouteUpdate } from "vue-router";
import { useI18n } from "vue-i18n";
import type { BusinessIdea } from "../types";
import { ideaApi } from "../api";
import { confirmAction } from "../../../shared/ui/confirm";
const props = defineProps<{ idea?: BusinessIdea }>(),
  emit = defineEmits<{ saved: [id: number]; changed: [] }>(),
  { t } = useI18n();
const form = reactive({
  title: props.idea?.title || "",
  offer: props.idea?.offer || "",
  audience: props.idea?.audience || "",
  resources: props.idea?.resources || "",
  nextStep: props.idea?.nextStep || "",
});
const fields = ["offer", "audience", "resources", "nextStep"] as const;
const baseline = ref(JSON.stringify(form));
const busy = ref(false),
  error = ref(""),
  dirty = computed(() => JSON.stringify(form) !== baseline.value);
watch(form, () => emit("changed"));
async function save() {
  if (busy.value) return;
  error.value = "";
  if (!form.title.trim()) {
    error.value = t("business.nameRequired");
    document.getElementById("idea-name")?.focus();
    return;
  }
  busy.value = true;
  try {
    const body = { ...form, title: form.title.trim() };
    let id = props.idea?.id;
    if (id) await ideaApi.edit(id, body, props.idea!.version);
    else id = await ideaApi.create(body);
    baseline.value = JSON.stringify(form);
    emit("saved", id!);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
const canLeave = async () =>
  !busy.value && (!dirty.value || (await confirmAction(t("business.leave"))));
onBeforeRouteLeave(canLeave);
onBeforeRouteUpdate(canLeave);
</script>
<template>
  <form class="stack idea-form panel" :aria-busy="busy" @submit.prevent="save">
    <p>{{ t("business.partial") }}</p>
    <label for="idea-name">{{ t("business.name") }}</label><input
      id="idea-name"
      v-model="form.title"
      maxlength="100"
      required
      :disabled="busy"
      aria-describedby="idea-example"
    />
    <p id="idea-example" class="muted">
      {{ t("business.example") }}
    </p>
    <div v-for="field in fields" :key="field" class="stack">
      <label :for="'idea-' + field">{{ t("business." + field) }}</label><textarea
        :id="'idea-' + field"
        v-model="form[field]"
        rows="2"
        maxlength="500"
        :disabled="busy"
        :aria-describedby="'idea-' + field + '-help'"
      ></textarea>
      <p :id="'idea-' + field + '-help'" class="muted">
        {{ t("business.optional") }}
      </p>
    </div>
    <p v-if="error" role="alert" class="notice error">
      {{ error }}
    </p>
    <div class="actions">
      <el-button type="primary" native-type="submit" :loading="busy">
        {{
          t("business.save")
        }}
      </el-button><RouterLink v-if="!busy" to="/student/business/ideas">
        {{
          t("cancel")
        }}
      </RouterLink>
    </div>
  </form>
</template>
<style scoped>
.idea-form {
  max-width: 48rem;
}
</style>
