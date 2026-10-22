import { describe, it, expect, vi } from "vitest";
import {
  assertSuccess,
  ApiError,
  mediaUrl,
} from "../../src/shared/http/client";
import { hasAll, hasPermission } from "../../src/shared/auth/permissions";
import { session } from "../../src/shared/auth/session";
describe("backend response contracts", () => {
  it("keeps top-level paged data", () => {
    const r = { code: 200, msg: "Success", rows: [{ id: 1 }], total: 25 };
    assertSuccess(r);
    expect(r.total).toBe(25);
    expect(r.rows).toHaveLength(1);
  });
  it("rejects business 401 even when transport succeeded", () =>
    expect(() => assertSuccess({ code: 401, msg: "Sign in" })).toThrow(
      ApiError,
    ));
  it("rejects unreadable JSON envelopes", () =>
    expect(() => assertSuccess("<html>")).toThrow());
  it("does not turn business failure into success", () =>
    expect(() => assertSuccess({ code: 403, msg: "Denied" })).toThrow(
      "Denied",
    ));
});
describe("permission semantics", () => {
  it("requires every permission for account updates", () =>
    expect(
      hasAll(
        ["system:user:edit"],
        ["system:user:edit", "system:user:changeStatus"],
      ),
    ).toBe(false));
  it("accepts explicit permissions", () =>
    expect(hasPermission(["learning:course:add"], "learning:course:add")).toBe(
      true,
    ));
  it("retains existing maintenance wildcard", () =>
    expect(hasAll(["*:*:*"], ["a", "b"])).toBe(true));
  it("does not interpret management identity as wildcard", () =>
    expect(hasPermission(["admin"], "learning:course:add")).toBe(false));
});
describe("private media URL", () => {
  it("adds same-origin proxy for student media", () =>
    expect(mediaUrl("/client/learning/media/" + "a".repeat(32))).toBe(
      "/api/client/learning/media/" + "a".repeat(32),
    ));
  it("rejects external or unexpected paths", () =>
    expect(() => mediaUrl("https://evil.example/a")).toThrow());
});
describe("session", () => {
  it("keeps channel and token together and clears both", () => {
    session.save("test-only", "admin");
    expect(session.channel).toBe("admin");
    session.clear();
    expect(session.token).toBeNull();
  });
});
vi.stubGlobal("matchMedia", () => ({ matches: false }));
