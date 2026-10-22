<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useRoute, onBeforeRouteLeave } from "vue-router";
import { useI18n } from "vue-i18n";
import { learningApi } from "../api";
import type { ChapterBody, Resource, ResourceBody } from "../types";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
import ResourceUpload from "../components/ResourceUpload.vue";
import ResourceFields from "../components/ResourceFields.vue";
import MediaViewer from "../components/MediaViewer.vue";
const { t } = useI18n(),
  route = useRoute(),
  id = computed(() => Number(route.params.id)),
  resources = ref<Resource[]>([]),
  loading = ref(false),
  error = ref(""),
  busy = ref(false),
  notice = ref(""),
  archived = ref(false),
  baseline = ref(""),
  preview = ref<Resource>(),
  edit = ref<Resource>();
const form = reactive<ChapterBody>({
    title: "",
    body: "",
    sortOrder: 0,
    status: "DRAFT",
    languageCode: "en",
  }),
  resourceForm = reactive<ResourceBody>({
    title: "",
    altText: "",
    captionResourceId: null,
    transcript: "",
    status: "DRAFT",
    languageCode: "en",
  });
const dirty = computed(
    () => baseline.value && JSON.stringify(form) !== baseline.value,
  ),
  captions = computed(() =>
    resources.value.filter((r) => r.kind === "CAPTION"),
  );
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [ch, c, r] = await Promise.all([
      learningApi.chapter(id.value),
      learningApi.course(Number(route.params.courseId)),
      learningApi.resources(id.value),
    ]);
    Object.assign(form, {
      title: ch.title,
      body: ch.body,
      sortOrder: ch.sortOrder,
      status: ch.status,
      languageCode: ch.languageCode,
    });
    baseline.value = JSON.stringify(form);
    archived.value = c.status === "ARCHIVED";
    resources.value = r;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function reloadResources() {
  try {
    resources.value = await learningApi.resources(id.value);
  } catch (e) {
    error.value = (e as Error).message;
  }
}
async function save() {
  busy.value = true;
  notice.value = "";
  error.value = "";
  try {
    await learningApi.updateChapter(id.value, form);
    baseline.value = JSON.stringify(form);
    notice.value = t("chapterSaved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function remove(r: Resource) {
  if (!(await confirmAction(t("confirmDelete")))) return;
  busy.value = true;
  try {
    await learningApi.deleteResource(r.id);
    await reloadResources();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
function editing(r: Resource) {
  edit.value = r;
  Object.assign(resourceForm, {
    title: r.title,
    altText: r.altText || "",
    captionResourceId: r.captionResourceId,
    transcript: r.transcript || "",
    status: r.status,
    languageCode: r.languageCode,
  });
}
async function saveResource() {
  if (!edit.value) return;
  busy.value = true;
  error.value = "";
  try {
    await learningApi.updateResource(edit.value.id, resourceForm);
    edit.value = undefined;
    await reloadResources();
    notice.value = t("contentSaved");
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onBeforeRouteLeave(
  async () => !dirty.value || (await confirmAction(t("leaveUnsaved"))),
);
watch(id, load, { immediate: true });
</script>
<template>
  <section>
    <RouterLink
      class="back-link"
      :to="'/manage/courses/' + route.params.courseId"
    >
      ← {{ t("backCourses") }}
    </RouterLink>
    <h1>{{ t("chapterTitle") }}</h1>
    <p v-if="notice" role="status" class="notice">
      {{ notice }}
    </p>
    <p v-if="error && !loading" class="notice error" role="alert">
      {{ error }}
    </p>
    <AsyncState :loading="loading" @retry="load">
      <p v-if="archived" class="notice">
        {{ t("archivedHelp") }}
      </p>
      <section class="panel">
        <form class="stack" @submit.prevent="save">
          <fieldset :disabled="busy || archived">
            <label for="chapter-title">{{ t("chapterTitle") }}</label><input
              id="chapter-title"
              v-model="form.title"
              required
              maxlength="150"
            /><label for="chapter-body">{{ t("body") }}</label><textarea
              id="chapter-body"
              v-model="form.body"
              maxlength="20000"
              rows="8"
            ></textarea>
            <div class="form-grid">
              <div>
                <label for="chapter-order">{{ t("sortOrder") }}</label><input
                  id="chapter-order"
                  v-model.number="form.sortOrder"
                  type="number"
                  required
                  min="0"
                  max="99999"
                />
              </div>
              <div>
                <label for="chapter-status">{{ t("status") }}</label><select id="chapter-status" v-model="form.status">
                  <option value="DRAFT">
                    {{ t("draft") }}
                  </option>
                  <option value="PUBLISHED">
                    {{ t("published") }}
                  </option>
                </select>
              </div>
            </div>
            <label for="chapter-language">{{ t("language") }}</label><input
              id="chapter-language"
              v-model="form.languageCode"
              required
              maxlength="35"
              pattern="[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*"
            /><el-button
              native-type="submit"
              type="primary"
              :loading="busy"
              :disabled="archived"
            >
              {{ t("save") }}
            </el-button>
          </fieldset>
        </form>
      </section>
      <section class="panel">
        <h2>{{ t("resources") }}</h2>
        <ul class="content-list">
          <li v-for="r in resources" :key="r.id">
            <span>{{ r.title }}
              <small>{{ t(r.kind.toLowerCase()) }} ·
                {{ t(r.status.toLowerCase()) }}</small></span>
            <div class="actions">
              <el-button @click="preview = r">
                {{ t("preview") }}
              </el-button><el-button :disabled="busy || archived" @click="editing(r)">
                {{
                  t("edit")
                }}
              </el-button><el-button :disabled="busy || archived" @click="remove(r)">
                {{
                  t("delete")
                }}
              </el-button>
            </div>
          </li>
        </ul>
        <p v-if="!resources.length">
          {{ t("noResources") }}
        </p>
      </section>
      <section v-if="!archived" class="panel">
        <h2>{{ t("upload") }}</h2>
        <ResourceUpload
          :chapter-id="id"
          :resources="resources"
          @saved="reloadResources"
        />
      </section>
    </AsyncState><el-dialog
      :model-value="!!preview"
      :title="t('preview')"
      width="min(850px,95vw)"
      destroy-on-close
      @update:model-value="preview = undefined"
    >
      <MediaViewer v-if="preview" :resource="preview" />
    </el-dialog><el-dialog
      :model-value="!!edit"
      :title="t('editResource')"
      width="min(650px,95vw)"
      destroy-on-close
      :close-on-click-modal="false"
      @update:model-value="edit = undefined"
    >
      <form v-if="edit" class="stack" @submit.prevent="saveResource">
        <ResourceFields
          v-model="resourceForm"
          :kind="edit.kind"
          :captions="captions"
          prefix="edit-resource"
        />
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <div class="actions">
          <el-button native-type="submit" type="primary" :loading="busy">
            {{
              t("save")
            }}
          </el-button><el-button :disabled="busy" @click="edit = undefined">
            {{
              t("cancel")
            }}
          </el-button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>
