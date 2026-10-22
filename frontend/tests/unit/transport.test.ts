import { describe, it, expect, vi, beforeEach } from "vitest";
const mocks = vi.hoisted(() => ({ send: vi.fn(), expired: vi.fn() }));
vi.mock("axios", () => ({
  default: { create: () => ({ request: mocks.send }), isCancel: () => false },
}));
import {
  request,
  onUnauthorized,
  table,
  download,
} from "../../src/shared/http/client";
import { session } from "../../src/shared/auth/session";
describe("actual shared transport decisions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    session.clear();
    onUnauthorized(mocks.expired);
  });
  it("recognizes the existing HTTP 500 / business 401 response and invalidates once per request", async () => {
    mocks.send.mockResolvedValue({
      status: 500,
      data: { code: 401, msg: "Sign in again." },
    });
    await expect(request("/getInfo")).rejects.toMatchObject({ code: 401 });
    expect(mocks.expired).toHaveBeenCalledOnce();
  });
  it("does not invalidate a valid session for HTTP 200 / business 403", async () => {
    mocks.send.mockResolvedValue({
      status: 200,
      data: { code: 403, msg: "Not allowed." },
    });
    await expect(request("/learning/courses/1")).rejects.toMatchObject({
      code: 403,
    });
    expect(mocks.expired).not.toHaveBeenCalled();
  });
  it("keeps the server total and sends an authenticated current/pageSize request", async () => {
    session.save("test-only-token", "admin");
    mocks.send.mockResolvedValue({
      status: 200,
      data: { code: 200, rows: [1], total: 41 },
    });
    const result = await table("/learning/courses", {
      params: { current: 2, pageSize: 10 },
    });
    expect(result.total).toBe(41);
    expect(mocks.send.mock.calls[0]?.[0]).toMatchObject({
      params: { current: 2 },
      headers: { Authorization: "Bearer test-only-token" },
    });
  });
  it("preserves authentication on network failures and gives a retryable message", async () => {
    session.save("test-only-token", "student");
    mocks.send.mockRejectedValue(new Error("offline"));
    await expect(request("/client/getInfo")).rejects.toMatchObject({ code: 0 });
    expect(session.token).toBe("test-only-token");
    expect(mocks.expired).not.toHaveBeenCalled();
  });
});

describe("private assignment downloads", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    session.save("test-only-token", "student");
    onUnauthorized(mocks.expired);
  });
  it("downloads bytes using the existing authorization header", async () => {
    const blob = new Blob(["answer"], { type: "text/plain" });
    mocks.send.mockResolvedValue({
      status: 200,
      data: blob,
      headers: { "content-type": "text/plain" },
    });
    expect(await download("/client/assignments/files/1/content")).toBe(blob);
    expect(mocks.send.mock.calls[0]?.[0].headers.Authorization).toBe(
      "Bearer test-only-token",
    );
  });
  it.each([401, 403])(
    "handles a binary JSON business error %s",
    async (code) => {
      mocks.send.mockResolvedValue({
        status: 200,
        headers: { "content-type": "application/json" },
        data: {
          text: async () => JSON.stringify({ code, msg: "Access denied." }),
        },
      });
      await expect(
        download("/client/assignments/files/1/content"),
      ).rejects.toMatchObject({ code });
      expect(mocks.expired).toHaveBeenCalledTimes(code === 401 ? 1 : 0);
    },
  );
});
