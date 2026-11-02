<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { mentorEncouragementApi as api } from "../api";
import type { Recipient } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const props = defineProps<{ modelValue: Recipient[]; disabled: boolean }>(),
  emit = defineEmits<{ "update:modelValue": [value: Recipient[]] }>(),
  { t } = useI18n();
const rows = ref<Recipient[]>([]),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const result = await api.students(page, keyword.value);
    if (seq !== sequence) return;
    rows.value = result.rows;
    total.value = result.total;
    current.value = page;
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
function checked(person: Recipient) {
  return props.modelValue.some((p) => p.id === person.id);
}
function toggle(person: Recipient) {
  if (props.disabled) return;
  if (checked(person))
    emit(
      "update:modelValue",
      props.modelValue.filter((p) => p.id !== person.id),
    );
  else if (props.modelValue.length < 100)
    emit("update:modelValue", [...props.modelValue, person]);
}
onMounted(() => load());
onBeforeUnmount(() => {
  sequence++;
});
</script>
<template>
  <section class="panel" :aria-label="t('encouragement.pick')">
    <h2>{{ t("encouragement.pick") }}</h2>
    <p>{{ t("encouragement.recipientHelp") }}</p>
    <fieldset :disabled="disabled" class="recipient-fields">
      <form class="search-bar" @submit.prevent="load()">
        <label class="sr-only" for="recipient-search">{{
          t("encouragement.search")
        }}</label>
        <input
          id="recipient-search"
          v-model="keyword"
          maxlength="100"
          :placeholder="t('encouragement.search')"
        />
        <el-button native-type="submit">
          {{ t("search") }}
        </el-button>
        <el-button
          @click="
            keyword = '';
            load();
          "
        >
          {{ t("clear") }}
        </el-button>
      </form>
      <p role="status">
        {{ t("encouragement.selected", { count: modelValue.length }) }}
      </p>
      <ul v-if="modelValue.length" class="selected-list">
        <li v-for="person in modelValue" :key="person.id">
          <span>{{ person.name }}</span><button
            type="button"
            :aria-label="t('encouragement.remove', { name: person.name })"
            @click="toggle(person)"
          >
            ×
          </button>
        </li>
      </ul>
      <el-button
        v-if="modelValue.length"
        @click="emit('update:modelValue', [])"
      >
        {{ t("encouragement.clearSelection") }}
      </el-button>
      <AsyncState :loading="loading" :error="error" @retry="load(current)">
        <p v-if="!rows.length">
          {{ t("encouragement.noStudents") }}
        </p>
        <div class="recipient-options">
          <label
            v-for="person in rows"
            :key="person.id"
            class="recipient-choice"
          >
            <input
              type="checkbox"
              :checked="checked(person)"
              :disabled="
                disabled || (!checked(person) && modelValue.length >= 100)
              "
              :aria-label="t('encouragement.select', { name: person.name })"
              @change="toggle(person)"
            />
            <span>{{ person.name }} <small>{{ person.username }}</small></span>
          </label>
        </div>
        <PaginationBar
          v-if="total > 10"
          :current="current"
          :total="total"
          @change="load"
        />
      </AsyncState>
    </fieldset>
  </section>
</template>
<style scoped>
.recipient-fields {
  border: 0;
  margin: 0;
  padding: 0;
  min-width: 0;
}
.recipient-options {
  display: grid;
  gap: 0.4rem;
}
.recipient-choice {
  display: flex;
  gap: 0.7rem;
  align-items: center;
  min-height: 48px;
  padding: 0.6rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  cursor: pointer;
  margin: 0.3rem 0;
}
.recipient-choice small {
  display: block;
  font-weight: 400;
  overflow-wrap: anywhere;
}
.selected-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.selected-list li {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  background: var(--tint);
  padding: 0.3rem 0.6rem;
  border-radius: 0.5rem;
  overflow-wrap: anywhere;
}
.selected-list button {
  min-width: 44px;
  min-height: 44px;
  color: var(--text);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 0.4rem;
  cursor: pointer;
}
</style>
