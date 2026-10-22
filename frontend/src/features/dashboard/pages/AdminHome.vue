<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
const { t } = useI18n(),
  auth = useAuthStore();
</script>
<template>
  <section>
    <p class="eyebrow">
      {{
        t(auth.isMaintenance ? "dashboard.internal" : "dashboard.administrator")
      }}
    </p>
    <h1>
      {{
        t(
          auth.isMaintenance
            ? "dashboard.internalTitle"
            : "dashboard.adminTitle",
        )
      }}
    </h1>
    <p>
      {{
        t(
          auth.isMaintenance
            ? "dashboard.internalIntro"
            : "dashboard.adminIntro",
        )
      }}
    </p>
    <div class="dashboard-grid">
      <section class="panel">
        <h2>{{ t("accounts") }}</h2>
        <p>{{ t("dashboard.accountsHelp") }}</p>
        <RouterLink class="button-link" to="/manage/accounts">
          {{ t("accounts") }} →
        </RouterLink>
      </section>
      <section class="panel">
        <h2>{{ t("dashboard.courseAllocation") }}</h2>
        <p>{{ t("dashboard.coursesHelp") }}</p>
        <RouterLink class="button-link" to="/manage/courses">
          {{ t("dashboard.courseAllocation") }} →
        </RouterLink>
      </section>
    </div>
  </section>
</template>
