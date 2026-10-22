<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { Resource, ResourceBody } from "../types";
defineProps<{ kind: string; captions: Resource[]; prefix: string }>();
const modelValue = defineModel<ResourceBody>({ required: true });
const { t } = useI18n();
</script>
<template>
  <label :for="prefix + '-title'">{{ t("resourceTitle") }}</label><input
    :id="prefix + '-title'"
    v-model="modelValue.title"
    required
    maxlength="150"
  /><template v-if="kind === 'IMAGE'">
    <label :for="prefix + '-alt'">{{ t("altText") }}</label>
    <p :id="prefix + '-alt-help'" class="muted">
      {{ t("altHelp") }}
    </p>
    <textarea
      :id="prefix + '-alt'"
      v-model="modelValue.altText"
      required
      maxlength="500"
      :aria-describedby="prefix + '-alt-help'"
    ></textarea>
  </template><template v-if="kind === 'VIDEO'">
    <label :for="prefix + '-caption'">{{ t("captionLink") }}</label><select :id="prefix + '-caption'" v-model="modelValue.captionResourceId">
      <option :value="null">
        {{ t("none") }}
      </option>
      <option v-for="c in captions" :key="c.id" :value="c.id">
        {{ c.title }} · {{ t(c.status.toLowerCase()) }}
      </option>
    </select><label :for="prefix + '-transcript'">{{ t("transcript") }}</label><textarea
      :id="prefix + '-transcript'"
      v-model="modelValue.transcript"
      maxlength="20000"
      rows="4"
    ></textarea>
  </template><label :for="prefix + '-status'">{{ t("status") }}</label><select :id="prefix + '-status'" v-model="modelValue.status">
    <option value="DRAFT">
      {{ t("draft") }}
    </option>
    <option value="PUBLISHED">
      {{ t("published") }}
    </option>
  </select><label :for="prefix + '-language'">{{ t("language") }}</label><input
    :id="prefix + '-language'"
    v-model="modelValue.languageCode"
    required
    maxlength="35"
    pattern="[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*"
  />
</template>
