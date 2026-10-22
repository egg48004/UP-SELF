import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { authApi } from "./api";
import type { User } from "./types";
import { session } from "../../shared/auth/session";
import type { Channel } from "../../shared/auth/session";
import { hasPermission } from "../../shared/auth/permissions";
export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null),
    permissions = ref<string[]>([]),
    loading = ref(false),
    totpEnabled = ref(false);
  let pending: Promise<void> | null = null;
  const isStudent = computed(() => user.value?.roleKey === "student");
  const isMentor = computed(() => user.value?.roleKey === "mentor");
  const isMaintenance = computed(() => user.value?.roleKey === "admin");
  const homePath = computed(
    () =>
      (
        ({
          student: "/student/home",
          mentor: "/mentor/home",
          platform_admin: "/admin/home",
          admin: "/internal/home",
        }) as Record<string, string>
      )[user.value?.roleKey || ""] || "/login",
  );
  const can = (p: string) => hasPermission(permissions.value, p);
  function clear() {
    session.clear();
    user.value = null;
    permissions.value = [];
    totpEnabled.value = false;
  }
  async function restore() {
    if (!session.token) return;
    if (pending) return pending;
    pending = (async () => {
      loading.value = true;
      try {
        const r = await authApi.info(session.channel);
        user.value = r.user;
        permissions.value = r.permissions;
        totpEnabled.value = r.totpEnabled === true;
      } finally {
        loading.value = false;
        pending = null;
      }
    })();
    return pending;
  }
  async function login(channel: Channel, username: string, password: string) {
    const r = await authApi.login(channel, username, password);
    session.save(r.token, channel);
    try {
      await restore();
    } catch (e) {
      clear();
      throw e;
    }
  }
  async function logout() {
    try {
      await authApi.logout(session.channel);
    } finally {
      clear();
    }
  }
  return {
    user,
    permissions,
    totpEnabled,
    loading,
    isStudent,
    isMentor,
    isMaintenance,
    homePath,
    can,
    clear,
    restore,
    login,
    logout,
  };
});
