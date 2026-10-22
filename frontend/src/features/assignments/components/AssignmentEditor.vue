<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import { useI18n } from "vue-i18n";
import { assignmentApi } from "../api";
import type { Assignment, AssignmentBody } from "../types";
import { learningApi } from "../../learning/api";
import type { Course } from "../../learning/types";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
const visible = defineModel<boolean>({ required: true });
const props = defineProps<{ assignment?: Assignment; courseId?: number }>();
const emit = defineEmits<{ saved: [id: number] }>();
const { t } = useI18n();
const form = reactive<AssignmentBody>({
    courseId: 0,
    title: "",
    instructions: "",
    status: "DRAFT",
    dueAt: null,
    maxScore: 100,
    version: 0,
    languageCode: "en",
  }),
  due = ref(""),
  busy = ref(false),
  error = ref(""),
  courses = ref<Course[]>([]),
  selected = ref<Course | null>(null),
  keyword = ref(""),
  current = ref(1),
  total = ref(0),
  loading = ref(false);
async function search(reset = true) {
  if (reset) current.value = 1;
  loading.value = true;
  error.value = "";
  try {
    const r = await learningApi.courses({
      current: current.value,
      pageSize: 10,
      keyword: keyword.value,
    });
    courses.value = r.rows;
    total.value = r.total;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function choose(c: Course) {
  selected.value = c;
  form.courseId = c.id;
}
watch(
  visible,
  async (open) => {
    if (!open) return;
    error.value = "";
    selected.value = null;
    Object.assign(
      form,
      {
        courseId: 0,
        title: "",
        instructions: "",
        status: "DRAFT",
        dueAt: null,
        maxScore: 100,
        version: 0,
        languageCode: "en",
      },
      props.assignment || {},
    );
    due.value = form.dueAt
      ? new Date(new Date(form.dueAt).getTime() + 8 * 3600000)
          .toISOString()
          .slice(0, 16)
      : "";
    const cid = props.assignment?.courseId || props.courseId;
    if (cid) {
      try {
        choose(await learningApi.course(cid));
      } catch (e) {
        error.value = (e as Error).message;
      }
    } else await search();
  },
  { immediate: true },
);
async function save() {
  if (!form.courseId) {
    error.value = t("assignment.chooseCourse");
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    form.dueAt = due.value ? due.value + ":00+08:00" : null;
    const id = props.assignment
      ? (await assignmentApi.update(props.assignment.id, form),
        props.assignment.id)
      : await assignmentApi.create(form);
    visible.value = false;
    emit("saved", id);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
function beforeClose(done: () => void) {
  if (!busy.value) done();
}
</script>
<template>
  <el-dialog
    v-model="visible"
    :title="t(assignment ? 'assignment.edit' : 'assignment.new')"
    width="680px"
    :close-on-click-modal="!busy"
    :close-on-press-escape="!busy"
    :before-close="beforeClose"
  >
    <form @submit.prevent="save">
      <fieldset :disabled="busy">
        <div v-if="selected" class="notice">
          <strong>{{ t("assignment.course") }}: {{ selected.title }}</strong
          ><el-button
            v-if="!assignment"
            @click="
              selected = null;
              form.courseId = 0;
              search();
            "
          >
            {{ t("assignment.chooseCourse") }}
          </el-button>
        </div>
        <section v-else>
          <label for="course-keyword">{{ t("assignment.chooseCourse") }}</label>
          <div class="search-row">
            <input
              id="course-keyword"
              v-model="keyword"
              :placeholder="t('search')"
            /><el-button :disabled="loading" @click="search()">
              {{ t("search") }}
            </el-button>
          </div>
          <ul class="candidate-list">
            <li v-for="c in courses" :key="c.id">
              <el-button
                :disabled="c.status === 'ARCHIVED'"
                :aria-label="'Choose course: ' + c.title"
                @click="choose(c)"
              >
                {{ c.title }}
              </el-button>
            </li>
          </ul>
          <PaginationBar
            :current="current"
            :total="total"
            @change="
              (n) => {
                current = n;
                search(false);
              }
            "
          />
        </section>
        <label for="assignment-title">{{
          t("assignment.assignmentTitle")
        }}</label
        ><input
          id="assignment-title"
          v-model="form.title"
          required
          maxlength="150"
        /><label for="assignment-instructions">{{
          t("assignment.instructions")
        }}</label
        ><textarea
          id="assignment-instructions"
          v-model="form.instructions"
          required
          maxlength="20000"
          rows="5"
        /><label for="assignment-due">{{ t("assignment.due") }}</label
        ><input id="assignment-due" v-model="due" type="datetime-local" />
        <p class="muted">
          {{ t("assignment.lateHelp") }}
        </p>
        <label for="assignment-max">{{ t("assignment.maxScore") }}</label
        ><input
          id="assignment-max"
          v-model.number="form.maxScore"
          type="number"
          min="1"
          max="1000"
          required
        /><label for="assignment-status">{{ t("assignment.status") }}</label
        ><select id="assignment-status" v-model="form.status">
          <option
            v-for="s in ['DRAFT', 'PUBLISHED', 'ARCHIVED']"
            :key="s"
            :value="s"
          >
            {{ t("assignment." + s) }}
          </option></select
        ><label for="assignment-language">{{ t("assignment.language") }}</label
        ><input
          id="assignment-language"
          v-model="form.languageCode"
          required
          maxlength="35"
        />
        <p class="muted">
          {{ t("assignment.publishedNotice") }}
        </p>
      </fieldset>
      <p v-if="error" class="notice error" role="alert">
        {{ error }}
      </p>
      <div class="button-row">
        <el-button :disabled="busy" @click="visible = false">
          {{ t("cancel") }} </el-button
        ><el-button native-type="submit" type="primary" :loading="busy">
          {{ t("assignment.save") }}
        </el-button>
      </div>
    </form>
  </el-dialog>
</template>
