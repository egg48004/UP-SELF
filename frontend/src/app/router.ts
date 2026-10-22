import { createRouter, createWebHashHistory } from "vue-router";
import { useAuthStore } from "../features/auth/store";
import { session } from "../shared/auth/session";
import AppLayout from "./layouts/AppLayout.vue";
import { ApiError } from "../shared/http/client";
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/login",
      component: () => import("../features/auth/pages/LoginPage.vue"),
    },
    {
      path: "/",
      component: AppLayout,
      children: [
        { path: "", redirect: "/student/courses" },
        {
          path: "student/courses",
          component: () =>
            import("../features/learning/pages/StudentCoursesPage.vue"),
          meta: { student: true },
        },
        {
          path: "student/courses/:id",
          component: () =>
            import("../features/learning/pages/StudentCoursePage.vue"),
          meta: { student: true },
        },
        {
          path: "manage/courses",
          component: () =>
            import("../features/learning/pages/ManageCoursesPage.vue"),
          meta: { management: true },
        },
        {
          path: "manage/courses/:id",
          component: () =>
            import("../features/learning/pages/CourseWorkspacePage.vue"),
          meta: { management: true },
        },
        {
          path: "manage/courses/:courseId/chapters/:id",
          component: () =>
            import("../features/learning/pages/ChapterEditPage.vue"),
          meta: { management: true, permission: "learning:chapter:edit" },
        },
        {
          path: "manage/accounts",
          component: () =>
            import("../features/accounts/pages/AccountListPage.vue"),
          meta: { permission: "system:user:list", management: true },
        },
        {
          path: "preferences",
          component: () =>
            import("../features/preferences/pages/PreferencesPage.vue"),
        },
        {
          path: "profile/security",
          component: () => import("../features/auth/pages/SecurityPage.vue"),
        },
        { path: "403", component: () => import("../pages/ErrorPage.vue") },
        {
          path: ":pathMatch(.*)*",
          component: () => import("../pages/ErrorPage.vue"),
        },
      ],
    },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.path === "/login") return true;
  if (!session.token) return "/login";
  try {
    if (!auth.user) await auth.restore();
  } catch (e) {
    if (e instanceof ApiError && e.code === 401) return "/login";
    return false;
  }
  if (to.meta.student && !auth.isStudent) return "/manage/courses";
  if (to.meta.management && auth.isStudent) return "/403";
  if (to.meta.permission && !auth.can(String(to.meta.permission)))
    return "/403";
  return true;
});
router.afterEach(() => {
  if ("speechSynthesis" in window) window.speechSynthesis.cancel();
  document.title = "UP-SELF";
  requestAnimationFrame(() =>
    document.querySelector<HTMLElement>("#main")?.focus(),
  );
});
