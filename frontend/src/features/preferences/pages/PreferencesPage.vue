<script setup lang="ts">
import { usePreferences } from "../store";
import { useI18n } from "vue-i18n";
const { t } = useI18n(),
  settings = usePreferences();
</script>
<template>
  <section class="panel narrow">
    <h1><span aria-hidden="true">Aa </span>{{ t("preferences") }}</h1>
    <p>{{ t("settingsHelp") }}</p>
    <label for="text-size">{{ t("textSize") }}</label><select id="text-size" v-model="settings.size">
      <option
        v-for="s in ['standard', 'large', 'extraLarge']"
        :key="s"
        :value="s"
      >
        {{ t(s) }}
      </option>
    </select><label for="contrast">{{ t("contrast") }}</label><select id="contrast" v-model="settings.contrast">
      <option value="standard">
        {{ t("standard") }}
      </option>
      <option value="highContrast">
        {{ t("highContrast") }}
      </option>
    </select><label class="check"><input v-model="settings.motion" type="checkbox" />{{
      t("reduceMotion")
    }}</label><el-button @click="settings.reset">
      {{ t("resetSettings") }}
    </el-button>
  </section>
</template>
