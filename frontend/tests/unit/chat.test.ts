import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import {
  mergeMessages,
  useDiscussion,
  MESSAGE_WINDOW,
} from "../../src/features/chat/composables/useDiscussion";
import type { ChatApi } from "../../src/features/chat/api";
import type { ChatMessage, ChatSync } from "../../src/features/chat/types";
import { ApiError } from "../../src/shared/http/client";

function message(id: number, body = "Text"): ChatMessage {
  return {
    id,
    groupId: 1,
    senderId: 2,
    senderName: "Student",
    senderRole: "student",
    hideAllowed: true,
    body,
    clientKey: String(id),
    createTime: "2026-10-30T00:00:00Z",
    files: [],
  };
}
function snapshot(
  rows = [message(1)],
  cursor = 1,
  hasMore = false,
  hiddenStudentIds: number[] = [],
): ChatSync {
  return {
    group: { id: 1, courseId: 1, title: "Group", status: "ACTIVE", version: 0 },
    messages: { rows, hasMore, nextCursor: cursor },
    hiddenStudentIds,
  };
}
describe("bounded discussion lifecycle", () => {
  const views: ReturnType<typeof mount>[] = [];
  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(document, "hidden", "get").mockReturnValue(false);
  });
  afterEach(() => {
    views.splice(0).forEach((v) => v.unmount());
    vi.useRealTimers();
    vi.restoreAllMocks();
  });
  function render(atLatest = () => true, initialError?: Error) {
    const api = {
      sync: vi.fn().mockResolvedValue(snapshot()),
      detail: vi.fn(),
      messages: vi
        .fn()
        .mockResolvedValue({ rows: [], hasMore: false, nextCursor: 1 }),
      drafts: vi.fn().mockResolvedValue([]),
    };
    if (initialError) api.sync.mockRejectedValueOnce(initialError);
    let discussion!: ReturnType<typeof useDiscussion>;
    const view = mount(
      defineComponent({
        setup() {
          discussion = useDiscussion(api as unknown as ChatApi, 1, atLatest);
          return () => null;
        },
      }),
    );
    views.push(view);
    return { api, discussion, view };
  }
  it("deduplicates concurrent responses and keeps chronological order", () => {
    expect(
      mergeMessages(
        [message(2), message(1)],
        [message(2, "Updated"), message(3)],
      ).map((m) => [m.id, m.body]),
    ).toEqual([
      [1, "Text"],
      [2, "Updated"],
      [3, "Text"],
    ]);
  });
  it("recovers an initial connection failure and resumes updates after retry", async () => {
    const { api, discussion } = render(undefined, new Error("Connection lost"));
    await flushPromises();
    expect(discussion.group.value).toBeNull();
    expect(discussion.error.value).toBe("Connection lost");
    expect(discussion.loading.value).toBe(false);
    await discussion.load();
    expect(discussion.group.value?.id).toBe(1);
    expect(discussion.error.value).toBe("");
    await vi.advanceTimersByTimeAsync(5000);
    expect(api.sync).toHaveBeenLastCalledWith(1, 1, expect.any(AbortSignal));
  });
  it("uses one combined polling request and stops on unmount", async () => {
    const { api, discussion, view } = render();
    await flushPromises();
    api.sync.mockResolvedValue(snapshot([message(2)], 2));
    await vi.advanceTimersByTimeAsync(5000);
    expect(api.sync).toHaveBeenLastCalledWith(1, 1, expect.any(AbortSignal));
    expect(api.detail).not.toHaveBeenCalled();
    expect(api.messages).not.toHaveBeenCalled();
    expect(discussion.rows.value.map((m) => m.id)).toEqual([1, 2]);
    view.unmount();
    const calls = api.sync.mock.calls.length;
    await vi.advanceTimersByTimeAsync(30000);
    expect(api.sync).toHaveBeenCalledTimes(calls);
  });
  it("advances across hidden-only message tails using the server cursor", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.sync.mockResolvedValue(snapshot([], 99));
    await vi.advanceTimersByTimeAsync(5000);
    expect(discussion.rows.value.map((m) => m.id)).toEqual([1]);
    await vi.advanceTimersByTimeAsync(5000);
    expect(api.sync).toHaveBeenLastCalledWith(1, 99, expect.any(AbortSignal));
  });
  it("drains a paged backlog promptly without skipping rows", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.sync
      .mockResolvedValueOnce(snapshot([message(2)], 2, true))
      .mockResolvedValueOnce(snapshot([message(3)], 3));
    await vi.advanceTimersByTimeAsync(5000);
    await vi.advanceTimersByTimeAsync(100);
    expect(api.sync).toHaveBeenLastCalledWith(1, 2, expect.any(AbortSignal));
    expect(discussion.rows.value.map((m) => m.id)).toEqual([1, 2, 3]);
  });
  it("clears loaded content and stops polling after access is revoked", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.sync.mockRejectedValue(new ApiError(403, "Access removed"));
    await vi.advanceTimersByTimeAsync(5000);
    expect(discussion.denied.value).toBe(true);
    expect(discussion.rows.value).toEqual([]);
    expect(discussion.group.value).toBeNull();
    const count = api.sync.mock.calls.length;
    await vi.advanceTimersByTimeAsync(30000);
    expect(api.sync).toHaveBeenCalledTimes(count);
  });
  it("pauses hidden tabs and resumes when visible", async () => {
    const { api } = render();
    await flushPromises();
    const hidden = vi.spyOn(document, "hidden", "get");
    hidden.mockReturnValue(true);
    document.dispatchEvent(new Event("visibilitychange"));
    const count = api.sync.mock.calls.length;
    await vi.advanceTimersByTimeAsync(20000);
    expect(api.sync).toHaveBeenCalledTimes(count);
    hidden.mockReturnValue(false);
    document.dispatchEvent(new Event("visibilitychange"));
    await flushPromises();
    expect(api.sync).toHaveBeenCalledTimes(count + 1);
  });
  it("bounds the live window while retaining earlier-history navigation", async () => {
    const { discussion } = render();
    await flushPromises();
    discussion.append(Array.from({ length: 250 }, (_, i) => message(i + 2)));
    expect(discussion.rows.value).toHaveLength(MESSAGE_WINDOW);
    expect(discussion.rows.value[0].id).toBe(52);
    expect(discussion.hasEarlier.value).toBe(true);
  });
  it("keeps an older reading window stable and offers a return to latest", async () => {
    const { discussion, api } = render(() => false);
    await flushPromises();
    discussion.rows.value = Array.from({ length: 200 }, (_, i) =>
      message(i + 100),
    );
    api.messages.mockResolvedValue({
      rows: Array.from({ length: 30 }, (_, i) => message(i + 70)),
      hasMore: true,
      nextCursor: 299,
    });
    await discussion.earlier();
    expect(discussion.rows.value[0].id).toBe(70);
    expect(discussion.rows.value).toHaveLength(MESSAGE_WINDOW);
    expect(discussion.hasLater.value).toBe(true);
    discussion.append([message(300)]);
    expect(discussion.rows.value[0].id).toBe(70);
    expect(discussion.rows.value.at(-1)!.id).toBe(269);
    api.sync.mockResolvedValue(snapshot([message(300)], 300));
    await discussion.latest();
    expect(discussion.hasLater.value).toBe(false);
    expect(discussion.rows.value[0].id).toBe(300);
  });
  it("reloads visibility changes from another tab without resurrecting peer messages", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.sync.mockResolvedValue(snapshot([], 9, false, [2]));
    await vi.advanceTimersByTimeAsync(5000);
    expect(discussion.rows.value).toEqual([]);
    expect(discussion.hiddenStudentIds.value).toEqual([2]);
    expect(api.sync).toHaveBeenLastCalledWith(1);
  });
  it("drops in-flight old responses after a local hide and preserves legacy teaching", async () => {
    const { api, discussion } = render();
    await flushPromises();
    discussion.rows.value = [
      message(1),
      { ...message(2), senderRole: "mentor" },
      { ...message(3), senderRole: "LEGACY" },
    ];
    let resolve!: (s: ChatSync) => void;
    api.sync.mockReturnValueOnce(
      new Promise<ChatSync>((r) => {
        resolve = r;
      }),
    );
    const pending = discussion.poll();
    discussion.forgetPeer(2);
    resolve(snapshot([message(4)], 4));
    await pending;
    expect(discussion.rows.value.map((m) => m.id)).toEqual([2, 3]);
  });
  it("refreshes only history so pending attachments survive", async () => {
    const { api, discussion } = render();
    await flushPromises();
    const pending = { id: 5 } as never;
    discussion.drafts.value = [pending];
    api.sync.mockResolvedValue(snapshot([message(4)], 4));
    await discussion.latest();
    expect(discussion.drafts.value).toEqual([pending]);
    expect(api.drafts).toHaveBeenCalledTimes(1);
  });
  it("keeps loaded history on network failure and retries with backoff", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.sync.mockRejectedValue(new Error("Offline"));
    await vi.advanceTimersByTimeAsync(5000);
    expect(discussion.connection.value).toBe(true);
    expect(discussion.rows.value).toHaveLength(1);
    const count = api.sync.mock.calls.length;
    await vi.advanceTimersByTimeAsync(5000);
    expect(api.sync).toHaveBeenCalledTimes(count);
    await vi.advanceTimersByTimeAsync(10000);
    expect(api.sync).toHaveBeenCalledTimes(count + 1);
  });
});
