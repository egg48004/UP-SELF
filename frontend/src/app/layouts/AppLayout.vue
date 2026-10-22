<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../features/auth/store";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter(),
  error = ref("");
const student = computed(() => auth.isStudent);
function focusContent() {
  document.getElementById('main')?.focus();
}
async function logout() {
  try {
    await auth.logout();
    await router.replace("/login");
  } catch (e) {
    error.value = (e as Error).message;
    await router.replace("/login");
  }
}
</script>
<template>
  <div :class="['app-shell', student ? 'student-shell' : 'management-shell']">
    <a class="skip-link" href="#main" @click.prevent="focusContent">{{ t("skip") }}</a>
    <header class="app-header">
      <RouterLink
        class="brand"
        :to="student ? '/student/courses' : '/manage/courses'"
      >
        UP<span>SELF</span><i aria-hidden="true">✦</i>
      </RouterLink>
      <nav class="main-nav" :aria-label="t('mainNavigation')">
        <RouterLink :to="student ? '/student/courses' : '/manage/courses'">
          <span aria-hidden="true">📚 </span>{{ t(student ? "myCourses" : "courses") }}
        </RouterLink><RouterLink
          v-if="auth.can('system:user:list')"
          to="/manage/accounts"
        >
          {{ t("accounts") }}
        </RouterLink><RouterLink to="/preferences">
          <span aria-hidden="true">Aa </span>{{ t("preferences") }}
        </RouterLink>
      </nav>
      <div class="account-nav">
        <RouterLink to="/profile/security" :aria-label="t('profile')">
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
