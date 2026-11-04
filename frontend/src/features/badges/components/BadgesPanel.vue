<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { badgeApi } from "../api";
import type { Badge } from "../types";
import { badgeIcons, badgeDate } from "../presentation";
import AsyncState from "../../../shared/ui/AsyncState.vue";
const { t } = useI18n();
const rows = ref<Badge[]>([]),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load() {
  const current = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const result = await badgeApi.mine();
    if (current === sequence) rows.value = result;
  } catch (e) {
    if (current === sequence) error.value = (e as Error).message;
  } finally {
    if (current === sequence) loading.value = false;
  }
}
onMounted(load);
onBeforeUnmount(() => {
  sequence++;
});
</script>
<template>
  <section class="badges" aria-labelledby="my-badges-title">
    <h2 id="my-badges-title">
      {{ t("badge.title") }}
    </h2>
    <p>{{ t("badge.intro") }}</p>
    <el-button :disabled="loading" @click="load">
      {{
        t("badge.refresh")
      }}
    </el-button>
    <AsyncState :loading="loading" :error="error" @retry="load">
      <ul class="badge-list">
        <li v-for="badge in rows" :key="badge.code" class="panel">
          <span class="badge-icon" aria-hidden="true">{{
            badgeIcons[badge.code]
          }}</span>
          <h3>{{ t("badge.names." + badge.code) }}</h3>
          <template v-if="badge.awardedAt">
            <p class="badge-state">
              <span aria-hidden="true">✓ </span>{{ t("badge.earned") }}
            </p>
            <p>
              <time :datetime="badge.awardedAt">{{
                t("badge.earnedOn", { date: badgeDate(badge.awardedAt) })
              }}</time>
            </p>
          </template>
          <template v-else>
            <p class="badge-state">
              {{ t("badge.next") }}
            </p>
            <p>{{ t("badge.steps." + badge.code) }}</p>
          </template>
        </li>
      </ul>
    </AsyncState>
  </section>
</template>
<style scoped>
.badges {
  margin-block: 2rem;
}
.badge-list {
  list-style: none;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr));
  gap: 1rem;
}
.badge-list li {
  overflow-wrap: anywhere;
}
.badge-icon {
  font-size: 2rem;
}
.badge-state {
  font-weight: 700;
  color: var(--primary);
}
</style>
