import { createRouter, createWebHashHistory } from "vue-router";
import { useAuthStore } from "../features/auth/store";
import { session } from "../shared/auth/session";
import AppLayout from "./layouts/AppLayout.vue";
import { ApiError } from "../shared/http/client";
import { stopSpeech } from "../features/preferences/composables/useSpeech";
const login = () => import("../features/auth/pages/LoginPage.vue");
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [

    { path: "/login", component: login },
    { path: "/internal/login", component: login, meta: { internal: true } },
    {
      path: "/",
      component: AppLayout,
      children: [
        {path:'student/courses/:id/discussions',component:()=>import('../features/chat/pages/CourseDiscussionsPage.vue'),meta:{roles:['student'],permission:'chat:read'}},
        {path:'manage/courses/:id/discussions',component:()=>import('../features/chat/pages/CourseDiscussionsPage.vue'),meta:{roles:['mentor'],permission:'chat:read'}},
        {
          path: "student/knowledge",
          component: () => import("../features/knowledge/pages/StudentKnowledgePage.vue"),
          meta: { roles: ["student"], permission: "knowledge:own:read" },
        },
        {
          path: "mentor/knowledge",
          component: () => import("../features/knowledge/pages/MentorKnowledgePage.vue"),
          meta: { roles: ["mentor"], permission: "knowledge:questions:manage" },
        },
        {
          path: "student/self-efficacy",
          component: () => import("../features/self-efficacy/pages/StudentSelfEfficacyPage.vue"),
          meta: { roles: ["student"], permission: "efficacy:own:read" },
        },
        {
          path: "mentor/self-efficacy/:studentId",
          component: () => import("../features/self-efficacy/pages/MentorSelfEfficacyPage.vue"),
          meta: { roles: ["mentor"], permission: "efficacy:students:read" },
        },
        {
          path: "",
          component: () =>
            import("../features/dashboard/pages/StudentHome.vue"),
        },
        {
          path: "student/home",
          component: () =>
            import("../features/dashboard/pages/StudentHome.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "mentor/home",
          component: () => import("../features/dashboard/pages/MentorHome.vue"),
          meta: { roles: ["mentor"] },
        },
        {
          path: "admin/home",
          component: () => import("../features/dashboard/pages/AdminHome.vue"),
          meta: { roles: ["platform_admin"] },
        },
        {
          path: "internal/home",
          component: () => import("../features/dashboard/pages/AdminHome.vue"),
          meta: { roles: ["admin"] },
        },
        {
          path: "student/goals",
          component: () => import("../features/goals/pages/GoalsPage.vue"),
          meta: { roles: ["student"], permission: "goals:own" },
        },
        {
          path: "student/history",
          component: () =>
            import("../features/learning/pages/LearningHistoryPage.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "mentor/students",
          component: () =>
            import("../features/learning/pages/StudentOverviewPage.vue"),
          meta: { roles: ["mentor"] },
        },
        {
          path: "student/saved-courses",
          component: () => import("../features/learning/pages/SavedCoursesPage.vue"),
          meta: { roles: ["student"], permission: "learning:bookmark:own" },
        },
        {
          path: "student/courses",
          component: () =>
            import("../features/learning/pages/StudentCoursesPage.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "student/courses/:id",
          component: () =>
            import("../features/learning/pages/StudentCoursePage.vue"),
          meta: { roles: ["student"] },
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
          meta: { management: true, permission: "system:user:list" },
        },
        {
          path: "student/assignments",
          component: () =>
            import("../features/assignments/pages/AssignmentsPage.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "student/assignments/:id",
          component: () =>
            import("../features/assignments/pages/StudentAssignmentPage.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "mentor/assignments",
          component: () =>
            import("../features/assignments/pages/AssignmentsPage.vue"),
          meta: { roles: ["mentor"] },
        },
        {
          path: "mentor/assignments/:id",
          component: () =>
            import("../features/assignments/pages/MentorAssignmentPage.vue"),
          meta: { roles: ["mentor"] },
        },
        {
          path: "student/mood",
          component: () => import("../features/mood/pages/StudentMoodPage.vue"),
          meta: { roles: ["student"] },
        },
        {
          path: "mentor/mood",
          component: () => import("../features/mood/pages/MentorMoodList.vue"),
          meta: { roles: ["mentor"] },
        },
        {
          path: "mentor/mood/:studentId",
          component: () => import("../features/mood/pages/MentorMoodPage.vue"),
          meta: { roles: ["mentor"] },
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
  if (to.path === "/login" || to.path === "/internal/login") return true;
  if (!session.token) return "/login";
  try {
    if (!auth.user) await auth.restore();
  } catch (e) {
    if (e instanceof ApiError && e.code === 401) return "/login";
    return false;
  }
  if (to.path === "/") return auth.homePath;
  if (
    to.meta.roles &&
    !(to.meta.roles as string[]).includes(auth.user?.roleKey || "")
  )
    return "/403";
  if (to.meta.management && auth.isStudent) return "/403";
  if (to.meta.permission && !auth.can(String(to.meta.permission)))
    return "/403";
  return true;
});
router.afterEach(() => {
  stopSpeech();
  document.title = "UP-SELF";
  requestAnimationFrame(() =>
    document.querySelector<HTMLElement>("#main")?.focus(),
  );
});
