<script setup lang="ts">
import { useI18n } from "vue-i18n";
defineProps<{ current: number; total: number; pageSize?: number }>();
defineEmits<{ change: [page: number] }>();
const { t } = useI18n();
</script>
<template>
  <nav class="pagination" :aria-label="t('page', { current })">
    <el-button :disabled="current <= 1" @click="$emit('change', current - 1)">
      {{
        t("previous")
      }}
    </el-button><span>{{ t("page", { current }) }} · {{ t("total", { count: total }) }}</span><el-button
      :disabled="current * (pageSize || 10) >= total"
      @click="$emit('change', current + 1)"
    >
      {{ t("next") }}
    </el-button>
  </nav>
</template>
