<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "../../auth/store";
import { accountApi } from "../api";
import type { User } from "../../auth/types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { hasAll } from "../../../shared/auth/permissions";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  auth = useAuthStore(),
  rows = ref<User[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  creating = ref(false),
  selected = ref<User>(),
  resetPassword = ref("");
const form = reactive({ userName: "", nickName: "", password: "", roleId: 10 });
const canEdit = computed(() =>
  hasAll(auth.permissions, ["system:user:edit", "system:user:changeStatus"]),
);
function protectedUser(u: User) {
  return (
    u.roleKey === "platform_admin" &&
    u.id !== auth.user?.id &&
    auth.user?.roleKey !== "admin"
  );
}
function role(key: string) {
  return t(
    (
      {
        student: "student",
        mentor: "mentor",
        platform_admin: "platformAdmin",
        admin: "maintenance",
      } as Record<string, string>
    )[key] || "role",
  );
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await accountApi.list(current.value, keyword.value);
    rows.value = r.rows;
    total.value = r.total;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function search() {
  current.value = 1;
  void load();
}
function page(n: number) {
  current.value = n;
  void load();
}
async function create() {
  busy.value = true;
  error.value = "";
  try {
    await accountApi.create(form);
    creating.value = false;
    form.password = "";
    form.userName = "";
    form.nickName = "";
    notice.value = t("saved");
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (!selected.value) return;
  busy.value = true;
  error.value = "";
  try {
    const mine = selected.value.id === auth.user?.id;
    await accountApi.update(selected.value);
    selected.value = undefined;
    if (mine) {
      auth.clear();
      location.hash = "/login";
      return;
    }
    notice.value = t("saved");
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function reset() {
  if (!selected.value) return;
  busy.value = true;
  error.value = "";
  try {
    await accountApi.reset(selected.value.id, resetPassword.value);
    resetPassword.value = "";
    notice.value = t("saved");
    selected.value = undefined;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function totp() {
  if (selected.value && (await confirmAction(t("resetAuthenticator")))) {
    try {
      await accountApi.resetTotp(selected.value.id);
      notice.value = t("saved");
    } catch (e) {
      error.value = (e as Error).message;
    }
  }
}
function edit(u: User) {
  selected.value = { ...u };
  resetPassword.value = "";
  error.value = "";
}
onMounted(load);
</script>
<template>
  <section>
    <div class="page-heading">
      <h1>{{ t("accounts") }}</h1>
      <el-button
        v-if="auth.can('system:user:add')"
        type="primary"
        @click="creating = true"
      >
        {{ t("createAccount") }}
      </el-button>
    </div>
    <p>{{ t("accountHelp") }}</p>
    <p v-if="notice" role="status" class="notice">
      {{ notice }}
    </p>
    <form class="search-bar" @submit.prevent="search">
      <label class="sr-only" for="account-search">{{ t("username") }}</label><input
        id="account-search"
        v-model="keyword"
        :placeholder="t('username')"
      /><el-button native-type="submit">
        {{ t("search") }}
      </el-button><el-button
        :disabled="!keyword"
        @click="
          keyword = '';
          search();
        "
      >
        {{ t("clear") }}
      </el-button>
    </form>
    <AsyncState
      :loading="loading"
      :error="!selected && !creating ? error : ''"
      @retry="load"
    >
      <div class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("username") }}</th>
              <th>{{ t("name") }}</th>
              <th>{{ t("role") }}</th>
              <th>{{ t("status") }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in rows" :key="u.id">
              <td>{{ u.userName }}</td>
              <td>{{ u.nickName }}</td>
              <td>{{ role(u.roleKey) }}</td>
              <td>{{ t(u.status === "0" ? "active" : "disabled") }}</td>
              <td>
                <el-button
                  v-if="canEdit && !protectedUser(u)"
                  @click="edit(u)"
                >
                  {{ t("edit") }}
                </el-button><span v-else>{{ t("viewOnly") }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!rows.length">
          {{ t("noAccounts") }}
        </p>
      </div>
      <PaginationBar
        :current="current"
        :total="total"
        @change="page"
      />
    </AsyncState><el-dialog
      v-model="creating"
      :title="t('createAccount')"
      width="min(580px,95vw)"
      :close-on-click-modal="false"
    >
      <form class="stack" @submit.prevent="create">
        <label for="account-username">{{ t("username") }}</label><input
          id="account-username"
          v-model="form.userName"
          required
          pattern="[A-Za-z0-9_.@-]{3,100}"
        /><label for="account-name">{{ t("name") }}</label><input
          id="account-name"
          v-model="form.nickName"
          required
          maxlength="60"
        /><label for="account-password">{{ t("initialPassword") }}</label><input
          id="account-password"
          v-model="form.password"
          type="password"
          required
          minlength="10"
          maxlength="64"
          autocomplete="new-password"
        /><label for="account-role">{{ t("role") }}</label><select id="account-role" v-model.number="form.roleId">
          <option :value="10">
            {{ t("student") }}
          </option>
          <option :value="20">
            {{ t("mentor") }}
          </option>
          <option :value="30">
            {{ t("platformAdmin") }}
          </option>
        </select>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <div class="actions">
          <el-button native-type="submit" type="primary" :loading="busy">
            {{
              t("createAccount")
            }}
          </el-button><el-button
            @click="
              creating = false;
              form.password = '';
            "
          >
            {{ t("cancel") }}
          </el-button>
        </div>
      </form>
    </el-dialog><el-dialog
      :model-value="!!selected"
      :title="t('edit')"
      width="min(620px,95vw)"
      :close-on-click-modal="false"
      @update:model-value="
        selected = undefined;
        resetPassword = '';
      "
    >
      <template v-if="selected">
        <form class="stack" @submit.prevent="save">
          <label for="edit-name">{{ t("name") }}</label><input
            id="edit-name"
            v-model="selected.nickName"
            required
            maxlength="60"
          /><label for="edit-role">{{ t("role") }}</label><select
            id="edit-role"
            v-model.number="selected.roleId"
            :disabled="selected.id === auth.user?.id"
          >
            <option :value="10">
              {{ t("student") }}
            </option>
            <option :value="20">
              {{ t("mentor") }}
            </option>
            <option :value="30">
              {{ t("platformAdmin") }}
            </option>
          </select><label for="edit-status">{{ t("status") }}</label><select
            id="edit-status"
            v-model="selected.status"
            :disabled="selected.id === auth.user?.id"
          >
            <option value="0">
              {{ t("active") }}
            </option>
            <option value="1">
              {{ t("disabled") }}
            </option>
          </select><el-button native-type="submit" type="primary" :loading="busy">
            {{
              t("saveAccount")
            }}
          </el-button>
        </form>
        <form
          v-if="auth.can('system:user:resetPwd')"
          class="stack"
          @submit.prevent="reset"
        >
          <label for="reset-password">{{ t("newPassword") }}</label><input
            id="reset-password"
            v-model="resetPassword"
            type="password"
            required
            minlength="10"
            maxlength="64"
            autocomplete="new-password"
          /><el-button native-type="submit" :loading="busy">
            {{
              t("resetPassword")
            }}
          </el-button>
        </form>
        <el-button v-if="auth.user?.roleKey === 'admin' && auth.totpEnabled" @click="totp">
          {{
            t("resetAuthenticator")
          }}
        </el-button>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <el-button
          @click="
            selected = undefined;
            resetPassword = '';
          "
        >
          {{ t("close") }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
