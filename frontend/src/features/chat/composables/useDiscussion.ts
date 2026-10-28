import { ref, onMounted, onBeforeUnmount } from "vue";
import { ApiError } from "../../../shared/http/client";
import type { ChatApi } from "../api";
import type { ChatGroup, ChatMessage, ChatFile } from "../types";
export function mergeMessages(
  existing: ChatMessage[],
  incoming: ChatMessage[],
) {
  return [
    ...new Map([...existing, ...incoming].map((row) => [row.id, row])).values(),
  ].sort((a, b) => a.id - b.id);
}
export function useDiscussion(api: ChatApi, id: number) {
  const group = ref<ChatGroup | null>(null),
    rows = ref<ChatMessage[]>([]),
    drafts = ref<ChatFile[]>([]),
    loading = ref(true),
    error = ref(""),
    connection = ref(false),
    hasEarlier = ref(false),
    denied = ref(false);
  let stopped = false,
    timer: ReturnType<typeof setTimeout> | undefined,
    controller: AbortController | undefined,
    running = false;
  function fail(e: unknown) {
    if (e instanceof ApiError && [401, 403, 404].includes(e.code)) {
      denied.value = true;
      rows.value = [];
      drafts.value = [];
      group.value = null;
      error.value = e.message;
      clearTimeout(timer);
    } else connection.value = true;
  }
  async function load() {
    loading.value = true;
    error.value = "";
    try {
      const [g, b, files] = await Promise.all([
        api.detail(id),
        api.messages(id),
        api.drafts(id),
      ]);
      if (stopped) return;
      group.value = g;
      rows.value = b.rows;
      hasEarlier.value = b.hasMore;
      drafts.value = files;
      connection.value = false;
      denied.value = false;
    } catch (e) {
      if (!stopped) {
        fail(e);
        error.value = (e as Error).message;
      }
    } finally {
      if (!stopped) loading.value = false;
    }
  }
  async function poll() {
    clearTimeout(timer);
    if (stopped || document.hidden || denied.value || running) return;
    running = true;
    controller = new AbortController();
    try {
      const [g, b] = await Promise.all([
        api.detail(id, controller.signal),
        api.messages(
          id,
          { afterId: rows.value.at(-1)?.id || 0 },
          controller.signal,
        ),
      ]);
      if (stopped || controller.signal.aborted) return;
      group.value = g;
      rows.value = mergeMessages(rows.value, b.rows);
      connection.value = false;
    } catch (e) {
      if (!stopped && !controller.signal.aborted) fail(e);
    } finally {
      running = false;
      if (!stopped && !denied.value && !document.hidden)
        timer = setTimeout(poll, connection.value ? 15000 : 5000);
    }
  }
  async function earlier() {
    const first = rows.value[0];
    if (!first) return;
    try {
      const b = await api.messages(id, { beforeId: first.id });
      if (stopped) return;
      rows.value = mergeMessages(rows.value, b.rows);
      hasEarlier.value = b.hasMore;
    } catch (e) {
      if (!stopped) {
        fail(e);
        error.value = (e as Error).message;
      }
    }
  }
  function visibility() {
    clearTimeout(timer);
    if (document.hidden) controller?.abort();
    else void poll();
  }
  onMounted(async () => {
    document.addEventListener("visibilitychange", visibility);
    await load();
    if (!stopped && !denied.value) timer = setTimeout(poll, 5000);
  });
  onBeforeUnmount(() => {
    stopped = true;
    clearTimeout(timer);
    controller?.abort();
    document.removeEventListener("visibilitychange", visibility);
  });
  return {
    group,
    rows,
    drafts,
    loading,
    error,
    connection,
    hasEarlier,
    denied,
    load,
    poll,
    earlier,
  };
}
