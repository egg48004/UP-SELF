<script setup lang="ts">
import { useI18n } from "vue-i18n";
const { t } = useI18n();
</script>
<template>
  <section>
    <RouterLink to="/student/courses">
      ← {{ t("navigation.myLearning") }}
    </RouterLink>
    <h1><span aria-hidden="true">💡 </span>{{ t("business.title") }}</h1>
    <p>{{ t("business.intro") }}</p>
    <div class="dashboard-grid">
      <section class="panel">
        <h2><span aria-hidden="true">🌱 </span>{{ t("business.ideas") }}</h2>
        <p>{{ t("business.ideasHelp") }}</p>
        <RouterLink class="button-link" to="/student/business/ideas">
          {{ t("business.ideas") }} <span aria-hidden="true">→</span>
        </RouterLink>
      </section>
      <section class="panel">
        <h2>
          <span aria-hidden="true">🧮 </span>{{ t("business.calculator") }}
        </h2>
        <p>{{ t("business.calculatorHelp") }}</p>
        <RouterLink class="button-link" to="/student/business/costs">
          {{ t("business.calculator") }} <span aria-hidden="true">→</span>
        </RouterLink>
      </section>
    </div>
    <p class="muted">
      {{ t("business.private") }}
    </p>
  </section>
</template>
