<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../store";
import type { Channel } from "../../../shared/auth/session";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter();
const channel = ref<Channel>("student"),
  username = ref(""),
  password = ref(""),
  busy = ref(false),
  error = ref("");
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    await auth.login(channel.value, username.value, password.value);
    password.value = "";
    await router.replace(
      auth.isStudent ? "/student/courses" : "/manage/courses",
    );
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
      <h2>{{ t("welcome") }}</h2>
      <p>{{ t("loginHelp") }}</p>
      <form @submit.prevent="submit">
        <label for="entry">{{ t("signIn") }}</label><select id="entry" v-model="channel">
          <option value="student">
            {{ t("studentEntry") }}
          </option>
          <option value="admin">
            {{ t("managementEntry") }}
          </option>
        </select><label for="username">{{ t("username") }}</label><input
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
