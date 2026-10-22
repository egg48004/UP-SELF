<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../store";

const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter();
const route = useRoute();
const internal = computed(() => route.meta.internal === true);
const entry = ref("student"),
  username = ref(""),
  password = ref(""),
  busy = ref(false),
  error = ref("");
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    await auth.login(
      !internal.value && entry.value === "student" ? "student" : "admin",
      username.value,
      password.value,
    );
    const expected = internal.value
      ? "admin"
      : (
          {
            student: "student",
            mentor: "mentor",
            admin: "platform_admin",
          } as Record<string, string>
        )[entry.value];
    if (auth.user?.roleKey !== expected) {
      await auth.logout();
      throw new Error(t("dashboard.wrongRole"));
    }
    password.value = "";
    await router.replace(auth.homePath);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <main class="login-layout">
    <div class="login-intro">
      <a class="brand" href="#/login">UP<span>SELF</span><i aria-hidden="true">✦</i></a>
      <p class="eyebrow">
        {{ t("tagline") }}
      </p>
      <h1>{{ t("loginTitle") }}</h1>
      <div class="learning-illustration" aria-hidden="true">
        <span>🌱</span><span>📖</span><span>☀️</span>
      </div>
    </div>
    <section class="login-card">
      <h2>{{ t(internal ? "dashboard.internalLogin" : "welcome") }}</h2>
      <p>{{ t(internal ? "dashboard.internalOnly" : "loginHelp") }}</p>
      <form @submit.prevent="submit">
        <template v-if="!internal">
          <label for="entry">{{ t("dashboard.loginRole") }}</label><select id="entry" v-model="entry">
            <option value="student">
              {{ t("studentEntry") }}
            </option>
            <option value="mentor">
              {{ t("dashboard.mentor") }}
            </option>
            <option value="admin">
              {{ t("dashboard.administrator") }}
            </option>
          </select>
        </template><label for="username">{{ t("username") }}</label><input
          id="username"
          v-model="username"
          required
          maxlength="100"
          autocomplete="username"
        /><label for="password">{{ t("password") }}</label><input
          id="password"
          v-model="password"
          required
          type="password"
          maxlength="128"
          autocomplete="current-password"
        />
        <p v-if="error" class="notice error" role="alert">
          {{ error }}
        </p>
        <el-button
          native-type="submit"
          type="primary"
          :loading="busy"
          class="wide"
        >
          {{ t("signIn") }} <span aria-hidden="true">→</span>
        </el-button>
      </form>
    </section>
  </main>
</template>
