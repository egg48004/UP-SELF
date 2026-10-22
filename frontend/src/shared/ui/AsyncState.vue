<script setup lang="ts">
import { useI18n } from "vue-i18n";
defineProps<{ loading?: boolean; error?: string }>();
defineEmits<{ retry: [] }>();
const { t } = useI18n();
</script>
<template>
  <p v-if="loading" role="status">
    {{ t("loading") }}
  </p>
  <div v-else-if="error" class="notice error" role="alert">
    <p>{{ error }}</p>
    <el-button @click="$emit('retry')">
      {{ t("retry") }}
    </el-button>
  </div>
  <slot v-else />
</template>
