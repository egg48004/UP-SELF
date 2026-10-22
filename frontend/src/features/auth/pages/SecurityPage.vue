<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { authApi } from "../api";
import { useAuthStore } from "../store";
import { session } from "../../../shared/auth/session";
const { t } = useI18n(),
  auth = useAuthStore(),
  router = useRouter(),
  oldPassword = ref(""),
  newPassword = ref(""),
  error = ref(""),
  busy = ref(false);
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    await authApi.password(
      session.channel,
      oldPassword.value,
      newPassword.value,
    );
    auth.clear();
    await router.replace("/login");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="panel narrow">
    <h1>{{ t("changePassword") }}</h1>
    <form @submit.prevent="submit">
      <label for="old-password">{{ t("currentPassword") }}</label><input
        id="old-password"
        v-model="oldPassword"
        type="password"
        required
        autocomplete="current-password"
      /><label for="new-password">{{ t("newPassword") }}</label><input
        id="new-password"
        v-model="newPassword"
        type="password"
        required
        minlength="10"
        maxlength="64"
        autocomplete="new-password"
      />
      <p v-if="error" role="alert" class="notice error">
        {{ error }}
      </p>
      <el-button native-type="submit" type="primary" :loading="busy">
        {{
          t("saveSignOut")
        }}
      </el-button>
    </form>
  </section>
</template>
