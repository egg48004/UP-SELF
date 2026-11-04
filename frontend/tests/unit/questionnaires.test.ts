import { describe, it, expect } from "vitest";
import { safeQuestionnaireUrl } from "../../src/features/questionnaires/links";
describe("external questionnaire links", () => {
  it("keeps a normal HTTPS form URL and its query/fragment", () => {
    expect(
      safeQuestionnaireUrl("https://forms.example.org/form?id=1#page"),
    ).toBe("https://forms.example.org/form?id=1#page");
  });
  it("accepts upper-case HTTPS with normalized host", () => {
    expect(safeQuestionnaireUrl("HTTPS://EXAMPLE.ORG/form")).toBe(
      "https://example.org/form",
    );
  });
  it("rejects executable, relative, HTTP and credential-bearing links", () => {
    for (const value of [
      "javascript:alert(1)",
      "data:text/html,test",
      "//example.org/form",
      "/local",
      "http://example.org",
      "https://user:pass@example.org",
      "https://@example.org",
      "https:example.org",
    ])
      expect(safeQuestionnaireUrl(value), value).toBeNull();
  });
  it("rejects malformed, whitespace, control-character and invalid-port URLs", () => {
    for (const value of [
      "",
      "https://",
      "https://example.org:0/form",
      "https://example.org:70000/form",
      "https://example.org/has space",
      "https://example.org/line\nfeed",
      "https://example.org/\\path",
    ])
      expect(safeQuestionnaireUrl(value), value).toBeNull();
  });
  it("rejects oversized URLs", () => {
    expect(
      safeQuestionnaireUrl("https://example.org/" + "a".repeat(2000)),
    ).toBeNull();
  });
});
