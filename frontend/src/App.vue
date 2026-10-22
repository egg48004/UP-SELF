<script setup lang="ts">
import en from "element-plus/es/locale/lang/en";
import { usePreferences } from "./features/preferences/store";
usePreferences();
</script>
<template>
  <el-config-provider :locale="en">
    <RouterView />
  </el-config-provider>
</template>
