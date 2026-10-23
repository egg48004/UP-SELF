<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../features/auth/store";
import NavigationLinks from "../../shared/ui/NavigationLinks.vue";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter(),
  route = useRoute(),
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
const learningPaths = ["/student/courses", "/student/saved-courses", "/student/history"];
const studentPaths = ["/mentor/students", "/mentor/mood"];
function within(path: string) {
  return route.path === path || route.path.startsWith(path + "/");
}
function link(to: string, key: string, icon = "", paths = [to]) {
  return { to, label: t(key), icon, active: paths.some(within) };
}
const mainLinks = computed(() => {
  const home = link(auth.homePath, "dashboard.home", auth.isStudent ? "🏠" : "");
  if (auth.isStudent) {
    return [
      home,
      link("/student/courses", "navigation.myLearning", "📚", learningPaths),
      link("/student/assignments", "assignment.title", "📝"),
      link("/student/mood", "mood.myMood", "🌤️"),
      ...(auth.can("goals:own") ? [link("/student/goals", "goal.title", "🌱")] : []),
    ];
  }
  if (auth.isMentor) {
    return [
      home,
      link("/manage/courses", "dashboard.teachingCourses"),
      link("/mentor/assignments", "assignment.title"),
      link("/mentor/students", "navigation.students", "", studentPaths),
    ];
  }
  return [
    home,
    ...(auth.can("system:user:list") ? [link("/manage/accounts", "accounts")] : []),
    link("/manage/courses", "dashboard.courseAllocation"),
  ];
});
const sectionLinks = computed(() => {
  if (auth.isStudent && learningPaths.some(within)) {
    return {
      label: t("navigation.learningSections"),
      items: [
        link("/student/courses", "myCourses"),
        ...(auth.can("learning:bookmark:own") ? [link("/student/saved-courses", "bookmark.title")] : []),
        link("/student/history", "progress.history"),
      ],
    };
  }
  if (auth.isMentor && studentPaths.some(within)) {
    return {
      label: t("navigation.studentSections"),
      items: [
        link("/mentor/students", "progress.overview"),
        link("/mentor/mood", "mood.studentMood"),
      ],
    };
  }
  return null;
});
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
      <div class="account-nav">
        <span class="role-badge">{{ t("dashboard." + role) }}</span><RouterLink to="/profile/security" :aria-label="t('profile')">
          {{ auth.user?.nickName }}
        </RouterLink><el-button text @click="logout">
          {{ t("signOut") }}
        </el-button>
      </div>
      <RouterLink
        v-if="auth.isStudent"
        class="reading-settings-link"
        to="/preferences"
        :aria-label="t('preferences')"
        :title="t('preferences')"
      >
        <span aria-hidden="true">Aa</span>
      </RouterLink>
      <NavigationLinks class="main-nav" :label="t(navigation)" :items="mainLinks" />
    </header>
    <main id="main" tabindex="-1" class="main-content">
      <p v-if="error" role="alert">
        {{ error }}
      </p>
      <NavigationLinks
        v-if="sectionLinks"
        class="section-nav"
        :label="sectionLinks.label"
        :items="sectionLinks.items"
      />
      <RouterView />
    </main>
    <footer>
      UP-SELF <span aria-hidden="true">✦</span> {{ t("tagline") }}
    </footer>
  </div>
</template>
