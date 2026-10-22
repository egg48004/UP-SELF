<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { accountApi } from "../../accounts/api";
import type { Candidate } from "../../accounts/api";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ role: "mentor" | "student" }>(),
  emit = defineEmits<{ selected: [candidate: Candidate] }>(),
  { t } = useI18n(),
  rows = ref<Candidate[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  error = ref(""),
  loading = ref(false);
let seq = 0;
async function load() {
  const n = ++seq;
  loading.value = true;
  error.value = "";
  try {
    const r = await accountApi.candidates(
      props.role,
      current.value,
      keyword.value,
    );
    if (n === seq) {
      rows.value = r.rows;
      total.value = r.total;
    }
  } catch (e) {
    if (n === seq) error.value = (e as Error).message;
  } finally {
    if (n === seq) loading.value = false;
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
onMounted(load);
</script>
<template>
  <section>
    <form class="search-bar" @submit.prevent="search">
      <label class="sr-only" for="candidate-search">{{
        t("findAccount")
      }}</label><input
        id="candidate-search"
        v-model="keyword"
        :placeholder="t('findAccount')"
      /><el-button native-type="submit" :loading="loading">
        {{
          t("search")
        }}
      </el-button>
    </form>
    <p v-if="error" class="notice error" role="alert">
      {{ error }}
    </p>
    <ul class="picker-list">
      <li v-for="u in rows" :key="u.id">
        <span>{{ u.nickName }} <small>{{ u.userName }}</small></span><el-button
          :aria-label="t('chooseAccount') + ': ' + u.userName"
          @click="emit('selected', u)"
        >
          {{ t("chooseAccount") }}
        </el-button>
      </li>
    </ul>
    <p v-if="!rows.length && !loading">
      {{ t("noAccounts") }}
    </p>
    <PaginationBar :current="current" :total="total" @change="page" />
  </section>
</template>
