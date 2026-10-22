import { computed, onBeforeUnmount, ref, watch } from "vue";
import type { Ref } from "vue";
import { useI18n } from "vue-i18n";
import axios from "axios";
import { assignmentApi } from "../api";
import type { Assignment, Submission } from "../types";
import { confirmAction } from "../../../shared/ui/confirm";
export function useStudentWork(id: Ref<number>) {
  const { t } = useI18n(),
    assignment = ref<Assignment | null>(null),
    work = ref<Submission | null>(null),
    answer = ref(""),
    savedAnswer = ref(""),
    loading = ref(false),
    busy = ref(""),
    error = ref(""),
    notice = ref(""),
    progress = ref(0),
    file = ref<File | null>(null),
    uploadControl = ref<AbortController | null>(null);
  let sequence = 0,
    requestKey = crypto.randomUUID();
  const dirty = computed(() => answer.value !== savedAnswer.value),
    editable = computed(
      () => work.value?.status === "DRAFT" || work.value?.status === "RETURNED",
    );
  async function load() {
    const token = ++sequence;
    loading.value = true;
    error.value = "";
    try {
      const [a, s] = await Promise.all([
        assignmentApi.detail(id.value),
        assignmentApi.mine(id.value),
      ]);
      if (token !== sequence) return;
      assignment.value = a;
      work.value = s;
      answer.value = s.answerText;
      savedAnswer.value = s.answerText;
      requestKey = crypto.randomUUID();
      file.value = null;
    } catch (e) {
      if (token === sequence) error.value = (e as Error).message;
    } finally {
      if (token === sequence) loading.value = false;
    }
  }
  async function refresh() {
    if (dirty.value && !(await confirmAction(t("assignment.leaveConfirm"))))
      return;
    notice.value = "";
    await load();
  }
  async function save() {
    if (!work.value || busy.value || !editable.value) return;
    busy.value = "save";
    error.value = "";
    const text = answer.value;
    try {
      work.value = await assignmentApi.draft(
        id.value,
        work.value.version,
        text,
      );
      savedAnswer.value = text;
      notice.value = t("assignment.saved");
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      busy.value = "";
    }
  }
  async function upload() {
    if (!work.value || !file.value || busy.value) return;
    const selected = file.value;
    if (
      selected.size > 20 * 1024 * 1024 ||
      selected.size === 0 ||
      !/\.(pdf|docx|png|jpe?g|txt)$/i.test(selected.name)
    ) {
      error.value = t("assignment.invalidFile");
      return;
    }
    busy.value = "upload";
    progress.value = 0;
    error.value = "";
    const controller = new AbortController();
    uploadControl.value = controller;
    try {
      work.value = await assignmentApi.upload(
        id.value,
        work.value.version,
        selected,
        controller.signal,
        (n) => {
          progress.value = n;
        },
      );
      file.value = null;
      notice.value = t("saved");
    } catch (e) {
      error.value = axios.isCancel(e)
        ? t("assignment.uploadCancelled")
        : (e as Error).message;
    } finally {
      uploadControl.value = null;
      busy.value = "";
    }
  }
  async function remove(fileId: number) {
    if (
      !work.value ||
      busy.value ||
      !(await confirmAction(t("assignment.removeConfirm")))
    )
      return;
    busy.value = "remove";
    error.value = "";
    try {
      work.value = await assignmentApi.removeFile(
        id.value,
        fileId,
        work.value.version,
      );
      notice.value = t("assignment.fileRemoved");
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      busy.value = "";
    }
  }
  async function submit() {
    if (!work.value || !assignment.value || busy.value) return;
    if (file.value) {
      error.value = t("assignment.uploadFirst");
      return;
    }
    busy.value = "confirm";
    if (!(await confirmAction(t("assignment.submitConfirm")))) {
      busy.value = "";
      return;
    }
    busy.value = "submit";
    error.value = "";
    try {
      work.value = await assignmentApi.submit(
        id.value,
        work.value.version,
        answer.value,
        assignment.value.version,
        requestKey,
      );
      savedAnswer.value = answer.value;
      notice.value = t("assignment.savedWork");
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      busy.value = "";
    }
  }
  function beforeUnload(e: BeforeUnloadEvent) {
    if (dirty.value || busy.value) {
      e.preventDefault();
      e.returnValue = "";
    }
  }
  window.addEventListener("beforeunload", beforeUnload);
  const timer = setInterval(() => {
    if (dirty.value && !busy.value && editable.value && !loading.value)
      void save();
  }, 30000);
  onBeforeUnmount(() => {
    sequence++;
    window.removeEventListener("beforeunload", beforeUnload);
    clearInterval(timer);
    uploadControl.value?.abort();
  });
  watch(id, load, { immediate: true });
  return {
    assignment,
    work,
    answer,
    loading,
    busy,
    error,
    notice,
    progress,
    file,
    uploadControl,
    dirty,
    editable,
    load,
    refresh,
    save,
    upload,
    remove,
    submit,
  };
}
