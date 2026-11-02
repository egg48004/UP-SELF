import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { mentorEncouragementApi } from "./api";
import type { Recipient, SendRequest, SentMessage } from "./types";
export function useEncouragementComposer(
  deliver: (
    body: SendRequest,
  ) => Promise<SentMessage> = mentorEncouragementApi.send,
) {
  const { t } = useI18n(),
    selected = ref<Recipient[]>([]),
    body = ref(""),
    busy = ref(false),
    error = ref(""),
    notice = ref("");
  const dirty = computed(
    () => !!body.value.trim() || selected.value.length > 0,
  );
  let key = "",
    fingerprint = "",
    disposed = false;
  async function send() {
    if (busy.value || disposed) return false;
    error.value = "";
    notice.value = "";
    const text = body.value.trim(),
      ids = [...new Set(selected.value.map((r) => r.id))].sort((a, b) => a - b);
    if (!ids.length || ids.length > 100) {
      error.value = t("encouragement.limit");
      return false;
    }
    if (!text || text.length > 2000) {
      error.value = t("encouragement.required");
      return false;
    }
    const next = JSON.stringify([text, ids]);
    if (next !== fingerprint) {
      fingerprint = next;
      key = crypto.randomUUID();
    }
    busy.value = true;
    try {
      await deliver({ body: text, studentIds: ids, clientKey: key });
      if (disposed) return false;
      body.value = "";
      selected.value = [];
      fingerprint = "";
      key = "";
      notice.value = t("encouragement.sent");
      return true;
    } catch (e) {
      if (!disposed)
        error.value = (e as Error).message + " " + t("encouragement.kept");
      return false;
    } finally {
      if (!disposed) busy.value = false;
    }
  }
  function reset() {
    body.value = "";
    selected.value = [];
    error.value = "";
    notice.value = "";
    fingerprint = "";
    key = "";
  }
  return {
    selected,
    body,
    busy,
    error,
    notice,
    dirty,
    send,
    reset,
    dispose: () => {
      disposed = true;
    },
  };
}
