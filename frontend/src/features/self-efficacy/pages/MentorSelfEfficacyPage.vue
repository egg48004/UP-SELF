<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { efficacyApi } from "../api";
import type { EfficacyPerson } from "../types";
import SelfEfficacyTimeline from "../components/SelfEfficacyTimeline.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const route = useRoute(), { t } = useI18n(), id = computed(() => Number(route.params.studentId)), person = ref<EfficacyPerson | null>(null), loading = ref(false), error = ref("");
let sequence = 0;
async function load() {
  const active = ++sequence; person.value = null; loading.value = true; error.value = "";
  try { const result = await efficacyApi.student(id.value); if (active === sequence) person.value = result; }
  catch (e) { if (active === sequence) error.value = (e as Error).message; }
  finally { if (active === sequence) loading.value = false; }
}
watch(id, load, { immediate: true });
</script>
<template>
  <section>
    <RouterLink class="back-link" to="/mentor/mood">
      ← {{ t("mood.back") }}
    </RouterLink>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="person">
        <p class="eyebrow">
          {{ t("mood.readOnly") }} · {{ t("efficacy.title") }}
        </p>
        <h1>{{ person.displayName }} <small>({{ person.username }})</small></h1><p>{{ t("efficacy.mentorAnswers") }}</p><p>{{ t("efficacy.reflection") }}</p>
        <SelfEfficacyTimeline :key="person.id" :enabled="true" :student-id="person.id" />
      </template>
    </AsyncState>
  </section>
</template>
