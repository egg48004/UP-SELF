<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { ideaApi } from "../api";
import type { BusinessIdea } from "../types";
import IdeaForm from "../components/IdeaForm.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(),
  router = useRouter(),
  { t } = useI18n(),
  idea = ref<BusinessIdea>(),
  loading = ref(true),
  error = ref(""),
  notice = ref("");
let sequence = 0;
async function load() {
  const active = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const next = route.params.id
      ? await ideaApi.detail(Number(route.params.id))
      : undefined;
    if (active === sequence) idea.value = next;
  } catch (e) {
    if (active === sequence) error.value = (e as Error).message;
  } finally {
    if (active === sequence) loading.value = false;
  }
}
async function saved(id: number) {
  notice.value = t("business.saved");
  if (!route.params.id) await router.replace("/student/business/ideas/" + id);
  else await load();
}
watch(() => route.params.id, () => load());
onMounted(load);
</script>
<template>
  <section>
    <RouterLink to="/student/business/ideas">
      ← {{ t("business.ideas") }}
    </RouterLink>
    <h1>{{ t(route.params.id ? "business.edit" : "business.create") }}</h1>
    <p v-if="notice" class="notice" role="status">
      {{ notice }}
    </p>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <IdeaForm
        :key="idea ? idea.id + ':' + idea.version : 'new'"
        :idea="idea"
        @saved="saved"
        @changed="notice = ''"
      />
    </AsyncState>
  </section>
</template>
