<script setup lang="ts">
import { onMounted } from "vue";
import en from "element-plus/es/locale/lang/en";
import { usePreferences } from "./features/preferences/store";
const settings = usePreferences();
onMounted(() => {
  void settings.loadDefaults();
});
</script>
<template>
  <el-config-provider :locale="en">
    <RouterView />
  </el-config-provider>
</template>
