<script setup lang="ts">
import { useRoute } from "vue-router";
import { useAuthStore } from "../features/auth/store";
import { useI18n } from "vue-i18n";
const { t } = useI18n(),
  route = useRoute(),
  auth = useAuthStore();
</script>
<template>
  <section class="panel">
    <h1>{{ t(route.path === "/403" ? "noPermission" : "notFound") }}</h1>
    <RouterLink :to="auth.isStudent ? '/student/courses' : '/manage/courses'">
      {{
        t("goHome")
      }}
    </RouterLink>
  </section>
</template>
