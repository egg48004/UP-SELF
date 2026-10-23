<script setup lang="ts">
defineProps<{
  label: string;
  items: { to: string; label: string; icon?: string; active: boolean }[];
}>();
</script>
<template>
  <nav :aria-label="label">
    <RouterLink
      v-for="item in items"
      :key="item.to"
      v-slot="{ href, navigate }"
      :to="item.to"
      custom
    >
      <a :href="href" :aria-current="item.active ? 'page' : undefined" @click="navigate">
        <span v-if="item.icon" aria-hidden="true">{{ item.icon }}</span>
        {{ item.label }}
      </a>
    </RouterLink>
  </nav>
</template>
