import { ref, onMounted, onBeforeUnmount } from "vue";
import { ApiError } from "../../../shared/http/client";
import type { ChatApi } from "../api";
import type { ChatGroup, ChatMessage, ChatFile, ChatSync } from "../types";

export const MESSAGE_WINDOW = 200;
export function mergeMessages(
  existing: ChatMessage[],
  incoming: ChatMessage[],
) {
  return [
    ...new Map([...existing, ...incoming].map((row) => [row.id, row])).values(),
  ].sort((a, b) => a.id - b.id);
}
export function useDiscussion(
  api: ChatApi,
  id: number,
  atLatest: () => boolean = () => true,
) {
  const group = ref<ChatGroup | null>(null),
    rows = ref<ChatMessage[]>([]),
    drafts = ref<ChatFile[]>([]),
    hiddenStudentIds = ref<number[]>([]),
    loading = ref(true),
    error = ref(""),
    connection = ref(false),
    hasEarlier = ref(false),
    hasLater = ref(false),
    denied = ref(false),
    historyBusy = ref(false);
  let stopped = false,
    timer: ReturnType<typeof setTimeout> | undefined,
    controller: AbortController | undefined,
    running = false,
    epoch = 0,
    cursor = 0;
  function fail(e: unknown) {
    if (e instanceof ApiError && [401, 403, 404].includes(e.code)) {
      denied.value = true;
      rows.value = [];
      drafts.value = [];
      group.value = null;
      hiddenStudentIds.value = [];
      error.value = e.message;
      clearTimeout(timer);
    } else connection.value = true;
  }
  function applySnapshot(snapshot: ChatSync) {
    group.value = snapshot.group;
    rows.value = snapshot.messages.rows;
    hiddenStudentIds.value = snapshot.hiddenStudentIds;
    hasEarlier.value = snapshot.messages.hasMore;
    hasLater.value = false;
    cursor = snapshot.messages.nextCursor;
    denied.value = false;
    connection.value = false;
  }
  async function load() {
    const seq = ++epoch;
    controller?.abort();
    clearTimeout(timer);
    loading.value = true;
    error.value = "";
    try {
      const [snapshot, files] = await Promise.all([
        api.sync(id),
        api.drafts(id),
      ]);
      if (stopped || seq !== epoch) return;
      applySnapshot(snapshot);
      drafts.value = files;
    } catch (e) {
      if (!stopped && seq === epoch) {
        fail(e);
        error.value = (e as Error).message;
      }
    } finally {
      if (!stopped && seq === epoch) loading.value = false;
      schedule();
    }
  }
  // Refresh history separately so unsent text, recording and uploaded drafts survive.
  async function latest() {
    const seq = ++epoch;
    controller?.abort();
    clearTimeout(timer);
    historyBusy.value = true;
    error.value = "";
    try {
      const snapshot = await api.sync(id);
      if (!stopped && seq === epoch) applySnapshot(snapshot);
    } catch (e) {
      if (!stopped && seq === epoch) {
        fail(e);
        error.value = (e as Error).message;
      }
    } finally {
      if (!stopped && seq === epoch) historyBusy.value = false;
      schedule();
    }
  }
  function schedule(delay = 5000) {
    clearTimeout(timer);
    if (!stopped && !denied.value && !document.hidden)
      timer = setTimeout(poll, connection.value ? 15000 : delay);
  }
  function append(incoming: ChatMessage[]) {
    if (!incoming.length) return;
    if (hasLater.value) {
      return;
    }
    const merged = mergeMessages(rows.value, incoming);
    if (merged.length > MESSAGE_WINDOW) {
      if (!atLatest()) {
        rows.value = merged.slice(0, MESSAGE_WINDOW);
        hasLater.value = true;
      } else {
        rows.value = merged.slice(-MESSAGE_WINDOW);
        hasEarlier.value = true;
      }
    } else rows.value = merged;
  }
  function forgetPeer(studentId: number) {
    ++epoch;
    controller?.abort();
    clearTimeout(timer);
    rows.value = rows.value.filter(
      (m) => m.senderId !== studentId || m.senderRole !== "student",
    );
  }
  async function poll() {
    clearTimeout(timer);
    if (
      stopped ||
      document.hidden ||
      denied.value ||
      running ||
      historyBusy.value
    )
      return;
    running = true;
    const seq = epoch;
    const request = new AbortController();
    controller = request;
    let more = false;
    try {
      const snapshot = await api.sync(id, cursor, request.signal);
      if (stopped || request.signal.aborted || seq !== epoch) return;
      if (
        snapshot.hiddenStudentIds.join(",") !== hiddenStudentIds.value.join(",")
      ) {
        // Another tab changed visibility. Drop loaded media before requesting the new window.
        rows.value = [];
        await latest();
        return;
      }
      group.value = snapshot.group;
      append(snapshot.messages.rows);
      cursor = snapshot.messages.nextCursor;
      more = snapshot.messages.hasMore;
      connection.value = false;
    } catch (e) {
      if (!stopped && !request.signal.aborted && seq === epoch) fail(e);
    } finally {
      running = false;
      schedule(more ? 100 : 5000);
    }
  }
  async function earlier() {
    const first = rows.value[0];
    if (!first || historyBusy.value) return;
    const seq = epoch;
    historyBusy.value = true;
    try {
      const b = await api.messages(id, { beforeId: first.id });
      if (stopped || seq !== epoch) return;
      const merged = mergeMessages(rows.value, b.rows);
      if (merged.length > MESSAGE_WINDOW) {
        rows.value = merged.slice(0, MESSAGE_WINDOW);
        hasLater.value = true;
      } else rows.value = merged;
      hasEarlier.value = b.hasMore;
    } catch (e) {
      if (!stopped && seq === epoch) {
        fail(e);
        error.value = (e as Error).message;
      }
    } finally {
      if (!stopped && seq === epoch) historyBusy.value = false;
      schedule();
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
    schedule();
  });
  onBeforeUnmount(() => {
    stopped = true;
    ++epoch;
    clearTimeout(timer);
    controller?.abort();
    document.removeEventListener("visibilitychange", visibility);
  });
  return {
    group,
    rows,
    drafts,
    hiddenStudentIds,
    loading,
    error,
    connection,
    hasEarlier,
    hasLater,
    denied,
    historyBusy,
    load,
    poll,
    earlier,
    latest,
    append,
    forgetPeer,
  };
}
