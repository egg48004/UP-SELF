<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Post } from "../types";
defineProps<{ posts: Post[] }>();
const { t } = useI18n();
function date(s: string) {
  return new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kuala_Lumpur",
  }).format(new Date(s));
}
function safe(url: string | null) {
  try {
    const parsed = new URL(url || "");
    return parsed.protocol === "https:" && !parsed.username && !parsed.password
      ? url
      : null;
  } catch {
    return null;
  }
}
</script>
<template>
  <div>
    <p v-if="!posts.length" class="muted">
      {{ t("noPosts") }}
    </p>
    <article v-for="p in posts" :key="p.id" class="update-card">
      <span class="eyebrow"><span aria-hidden="true">{{ p.type === "MEETING" ? "📅" : "📣" }} </span>{{ t(p.type === "MEETING" ? "meeting" : "announcement") }}</span>
      <h3>{{ p.title }}</h3>
      <p class="plain-text">
        {{ p.body }}
      </p>
      <p v-if="p.meetingStartsAt">
        {{ date(p.meetingStartsAt) }} <small>(MYT)</small>
      </p>
      <a
        v-if="safe(p.meetingUrl)"
        :href="p.meetingUrl!"
        target="_blank"
        rel="noopener noreferrer"
      >{{ t("joinMeeting") }} ↗
        <span class="sr-only">{{ t("opensNewTab") }}</span></a><slot name="actions" :post="p" />
    </article>
  </div>
</template>
