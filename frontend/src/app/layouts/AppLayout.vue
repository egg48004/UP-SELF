<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../features/auth/store";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter(),
  error = ref("");
const role = computed(() =>
  auth.isStudent
    ? "student"
    : auth.isMentor
      ? "mentor"
      : auth.isMaintenance
        ? "internal"
        : "administrator",
);
const navigation = computed(() =>
  auth.isStudent
    ? "dashboard.studentNav"
    : auth.isMentor
      ? "dashboard.mentorNav"
      : auth.isMaintenance
        ? "dashboard.internalNav"
        : "dashboard.adminNav",
);
function focusContent() {
  document.getElementById("main")?.focus();
}
async function logout() {
  const target = auth.isMaintenance ? "/internal/login" : "/login";
  try {
    await auth.logout();
  } catch (e) {
    error.value = (e as Error).message;
  }
  await router.replace(target);
}
</script>
<template>
  <div
    :class="[
      'app-shell',
      auth.isStudent ? 'student-shell' : 'management-shell',
      'role-' + role,
    ]"
  >
    <a class="skip-link" href="#main" @click.prevent="focusContent">{{
      t("skip")
    }}</a>
    <header class="app-header">
      <RouterLink class="brand" :to="auth.homePath">
        UP<span>SELF</span><i aria-hidden="true">✦</i>
      </RouterLink>
      <nav class="main-nav" :aria-label="t(navigation)">
        <RouterLink :to="auth.homePath">
          {{ t("dashboard.home") }}
        </RouterLink>
        <RouterLink
          :to="auth.isStudent ? '/student/courses' : '/manage/courses'"
        >
          <span v-if="auth.isStudent" aria-hidden="true">📚 </span>{{
            t(
              auth.isStudent
                ? "myCourses"
                : auth.isMentor
                  ? "dashboard.teachingCourses"
                  : "dashboard.courseAllocation",
            )
          }}
        </RouterLink>
        <RouterLink
          v-if="auth.isStudent || auth.isMentor"
          :to="auth.isStudent ? '/student/assignments' : '/mentor/assignments'"
        >
          <span v-if="auth.isStudent" aria-hidden="true">📝 </span>{{ t("assignment.title") }}
        </RouterLink>
        <RouterLink v-if="auth.can('system:user:list')" to="/manage/accounts">
          {{
            t("accounts")
          }}
        </RouterLink>
        <RouterLink v-if="auth.isStudent" to="/preferences">
          Aa {{ t("preferences") }}
        </RouterLink>
      </nav>
      <div class="account-nav">
        <span class="role-badge">{{ t("dashboard." + role) }}</span><RouterLink to="/profile/security" :aria-label="t('profile')">
          {{
            auth.user?.nickName
          }}
        </RouterLink><el-button text @click="logout">
          {{ t("signOut") }}
        </el-button>
      </div>
    </header>
    <main id="main" tabindex="-1" class="main-content">
      <p v-if="error" role="alert">
        {{ error }}
      </p>
      <RouterView />
    </main>
    <footer>
      UP-SELF <span aria-hidden="true">✦</span> {{ t("tagline") }}
    </footer>
  </div>
</template>
