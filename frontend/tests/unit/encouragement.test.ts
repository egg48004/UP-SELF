import { describe, it, expect, vi } from "vitest";
vi.mock("vue-i18n", () => ({ useI18n: () => ({ t: (key: string) => key }) }));
import { useEncouragementComposer } from "../../src/features/encouragement/useEncouragementComposer";
import type { SentMessage } from "../../src/features/encouragement/types";
const person = (id: number) => ({
  id,
  name: "Student " + id,
  username: "student" + id,
});
const receipt = {
  id: 1,
  body: "Keep learning.",
  recipientCount: 2,
  sentAt: "2026-10-30T10:00:00+08:00",
} as SentMessage;
describe("encouragement delivery state", () => {
  it("does not send an empty message or selection", async () => {
    const send = vi.fn();
    const c = useEncouragementComposer(send);
    expect(await c.send()).toBe(false);
    c.selected.value = [person(1)];
    expect(await c.send()).toBe(false);
    expect(send).not.toHaveBeenCalled();
  });
  it("deduplicates sorted recipients and trims only outer message whitespace", async () => {
    const send = vi.fn().mockResolvedValue(receipt),
      c = useEncouragementComposer(send);
    c.body.value = " Keep learning.\nOne step. ";
    c.selected.value = [person(3), person(1), person(3)];
    await c.send();
    expect(send.mock.calls[0][0]).toMatchObject({
      body: "Keep learning.\nOne step.",
      studentIds: [1, 3],
    });
    expect(c.dirty.value).toBe(false);
    expect(c.notice.value).toBe("encouragement.sent");
  });
  it("preserves the same request key and draft after uncertain delivery", async () => {
    const send = vi
        .fn()
        .mockRejectedValueOnce(new Error("Offline"))
        .mockResolvedValue(receipt),
      c = useEncouragementComposer(send);
    c.body.value = "Keep learning.";
    c.selected.value = [person(1)];
    await c.send();
    expect(c.body.value).toBe("Keep learning.");
    expect(c.selected.value).toHaveLength(1);
    await c.send();
    expect(send.mock.calls[0][0].clientKey).toBe(
      send.mock.calls[1][0].clientKey,
    );
  });
  it("uses a new key when corrected wording changes the message", async () => {
    const send = vi.fn().mockRejectedValue(new Error("Filtered")),
      c = useEncouragementComposer(send);
    c.selected.value = [person(1)];
    c.body.value = "First wording";
    await c.send();
    c.body.value = "Supportive wording";
    await c.send();
    expect(send.mock.calls[0][0].clientKey).not.toBe(
      send.mock.calls[1][0].clientKey,
    );
    expect(c.selected.value).toHaveLength(1);
  });
  it("does not send twice while an earlier click is pending", async () => {
    let resolve!: (r: SentMessage) => void;
    const send = vi
        .fn()
        .mockImplementation(
          () => new Promise<SentMessage>((r) => (resolve = r)),
        ),
      c = useEncouragementComposer(send);
    c.selected.value = [person(1)];
    c.body.value = "Keep learning.";
    const pending = c.send();
    expect(c.busy.value).toBe(true);
    expect(await c.send()).toBe(false);
    resolve(receipt);
    await pending;
    expect(send).toHaveBeenCalledTimes(1);
  });
  it("rejects more than 100 recipients before a request", async () => {
    const send = vi.fn(),
      c = useEncouragementComposer(send);
    c.body.value = "Keep learning.";
    c.selected.value = Array.from({ length: 101 }, (_, id) => person(id + 1));
    expect(await c.send()).toBe(false);
    expect(send).not.toHaveBeenCalled();
  });
  it("ignores late delivery results after the page leaves", async () => {
    let resolve!: (r: SentMessage) => void;
    const c = useEncouragementComposer(
      () => new Promise<SentMessage>((r) => (resolve = r)),
    );
    c.selected.value = [person(1)];
    c.body.value = "Keep learning.";
    const pending = c.send();
    c.dispose();
    resolve(receipt);
    expect(await pending).toBe(false);
    expect(c.notice.value).toBe("");
  });
});
