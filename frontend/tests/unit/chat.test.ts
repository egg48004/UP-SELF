import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import {
  mergeMessages,
  useDiscussion,
} from "../../src/features/chat/composables/useDiscussion";
import type { ChatApi } from "../../src/features/chat/api";
import type { ChatMessage } from "../../src/features/chat/types";
import { ApiError } from "../../src/shared/http/client";
function message(id: number, body = "Text") {
  return {
    id,
    groupId: 1,
    senderId: 2,
    senderName: "Student",
    body,
    clientKey: String(id),
    createTime: "2026-10-28T00:00:00Z",
    files: [],
  } as ChatMessage;
}
describe("discussion lifecycle", () => {
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
  function render() {
    const api = {
      detail: vi
        .fn()
        .mockResolvedValue({ id: 1, title: "Group", status: "ACTIVE" }),
      messages: vi
        .fn()
        .mockResolvedValue({ rows: [message(1)], hasMore: false }),
      drafts: vi.fn().mockResolvedValue([]),
    };
    let discussion!: ReturnType<typeof useDiscussion>;
    const view = mount(
      defineComponent({
        setup() {
          discussion = useDiscussion(api as unknown as ChatApi, 1);
          return () => null;
        },
      }),
    );
    views.push(view);
    return { api, discussion, view };
  }
  it("merges concurrent send and polling responses without duplicates or reordering", () => {
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
  it("polls from the latest message and stops on unmount", async () => {
    const { api, discussion, view } = render();
    await flushPromises();
    api.messages.mockResolvedValue({ rows: [message(2)], hasMore: false });
    await vi.advanceTimersByTimeAsync(5000);
    expect(api.messages).toHaveBeenLastCalledWith(
      1,
      { afterId: 1 },
      expect.any(AbortSignal),
    );
    expect(discussion.rows.value.map((m) => m.id)).toEqual([1, 2]);
    view.unmount();
    const count = api.messages.mock.calls.length;
    await vi.advanceTimersByTimeAsync(30000);
    expect(api.messages).toHaveBeenCalledTimes(count);
  });
  it("removes cached messages and draft metadata after access is revoked", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.detail.mockRejectedValue(new ApiError(403, "Access removed"));
    await vi.advanceTimersByTimeAsync(5000);
    expect(discussion.denied.value).toBe(true);
    expect(discussion.rows.value).toEqual([]);
    expect(discussion.group.value).toBeNull();
    const count = api.messages.mock.calls.length;
    await vi.advanceTimersByTimeAsync(30000);
    expect(api.messages).toHaveBeenCalledTimes(count);
  });
  it("pauses hidden tabs and catches up when they become visible", async () => {
    const { api } = render();
    await flushPromises();
    const hidden = vi.spyOn(document, "hidden", "get");
    hidden.mockReturnValue(true);
    document.dispatchEvent(new Event("visibilitychange"));
    const count = api.messages.mock.calls.length;
    await vi.advanceTimersByTimeAsync(20000);
    expect(api.messages).toHaveBeenCalledTimes(count);
    hidden.mockReturnValue(false);
    document.dispatchEvent(new Event("visibilitychange"));
    await flushPromises();
    expect(api.messages).toHaveBeenCalledTimes(count + 1);
  });
  it("loads older history by cursor without losing current messages", async () => {
    const { api, discussion } = render();
    await flushPromises();
    api.messages.mockResolvedValue({ rows: [message(0)], hasMore: false });
    await discussion.earlier();
    expect(api.messages).toHaveBeenLastCalledWith(1, { beforeId: 1 });
    expect(discussion.rows.value.map((m) => m.id)).toEqual([0, 1]);
  });
});
