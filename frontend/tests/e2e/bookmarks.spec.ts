import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import path from "node:path";

test("saved courses: real roundtrip, unavailable entries, pagination and three-role isolation", async ({ browser, request }) => {
  test.skip(!process.env.UPSELF_BOOKMARK_STUDENT, "Use scripts/verify-bookmarks.py --browser for isolated accounts and courses.");
  test.setTimeout(180000);
  const contexts: BrowserContext[] = [], errors: string[] = [];
  const title = process.env.UPSELF_BOOKMARK_COURSE_TITLE!, cid = process.env.UPSELF_BOOKMARK_COURSE!;
  const base = "/api/client/learning/bookmarks";
  async function page() {
    const c = await browser.newContext(); contexts.push(c);
    const p = await c.newPage(); p.on("pageerror", e => errors.push(e.message)); return p;
  }
  async function login(p: Page, role: string, username: string, password: string) {
    await p.goto("/#/login");
    await p.getByLabel("Choose your role", { exact: true }).selectOption(role);
    await p.getByLabel("Username", { exact: true }).fill(username);
    await p.getByLabel("Password", { exact: true }).fill(password);
    await p.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(p).toHaveURL(new RegExp(role + "/home"));
  }
  async function saved(p: Page) {
    const sections = p.getByRole("navigation", { name: "My learning sections", exact: true });
    if (!await sections.isVisible()) await p.getByRole("navigation", { name: "Student navigation", exact: true }).getByRole("link", { name: /My learning/ }).click();
    await sections.getByRole("link", { name: "Saved courses", exact: true }).click();
    await expect(p.getByRole("heading", { name: "Saved courses", exact: true })).toBeVisible();
  }
  async function findCourse(p: Page) {
    await p.getByRole("navigation", { name: "Student navigation", exact: true }).getByRole("link", { name: /My learning/ }).click();
    await p.getByRole("searchbox").count().then(async n => {
      if (n) await p.getByRole("searchbox").fill(title);
      else await p.getByPlaceholder("Search", { exact: true }).fill(title);
    });
    await p.getByRole("button", { name: "Search", exact: true }).click();
    await expect(p.getByRole("article", { name: title, exact: true })).toBeVisible();
  }
  async function token(p: Page) { return p.evaluate(() => sessionStorage.getItem("upself.token")); }
  try {
    const student = await page();
    await login(student, "student", process.env.UPSELF_BOOKMARK_STUDENT!, process.env.UPSELF_BOOKMARK_STUDENT_PASSWORD!);
    await saved(student);
    await expect(student.getByText("No saved courses yet.", { exact: true })).toBeVisible();
    await findCourse(student);
    let card = student.getByRole("article", { name: title, exact: true });
    await student.route("**/api/client/learning/bookmarks/" + cid, route => route.fulfill({ json: { code: 500, msg: "The service is temporarily unavailable. Please try again later." } }), { times: 1 });
    await card.getByRole("button", { name: /^Save course:/ }).click();
    await expect(card.getByRole("alert")).toContainText("temporarily unavailable");
    await expect(card.getByRole("button", { name: /^Save course:/ })).toHaveAttribute("aria-pressed", "false");
    const saveButton = card.getByRole("button", { name: /^Save course:/ });
    await saveButton.focus(); await student.keyboard.press("Enter");
    await expect(card.getByRole("button", { name: /^Remove from saved:/ })).toHaveAttribute("aria-pressed", "true");
    await saved(student);
    card = student.getByRole("article", { name: title, exact: true });
    await expect(card).toBeVisible(); await student.reload(); await expect(card).toBeVisible();
    await student.screenshot({ path: path.join(process.env.UPSELF_BOOKMARK_WORK!, "saved-courses.png"), fullPage: true });
    await student.setViewportSize({ width: 390, height: 844 });
    expect(await student.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBe(true);
    await student.screenshot({ path: path.join(process.env.UPSELF_BOOKMARK_WORK!, "saved-courses-mobile.png"), fullPage: true });
    await student.setViewportSize({ width: 1365, height: 900 });
    await card.getByRole("link", { name: /^Open course/ }).click();
    await expect(student.getByRole("heading", { name: title, exact: true })).toBeVisible();
    await student.getByRole("button", { name: /^Remove from saved:/ }).click();
    await expect(student.getByRole("button", { name: /^Save course:/ })).toBeVisible();
    await saved(student); await expect(student.getByText("No saved courses yet.", { exact: true })).toBeVisible();
    await findCourse(student);
    await student.getByRole("article", { name: title, exact: true }).getByRole("button", { name: /^Save course:/ }).click();
    await expect(student.getByRole("button", { name: /^Remove from saved:/ })).toBeVisible();
    await student.getByRole("button", { name: "Sign out", exact: true }).click();
    await login(student, "student", process.env.UPSELF_BOOKMARK_STUDENT!, process.env.UPSELF_BOOKMARK_STUDENT_PASSWORD!);
    await saved(student); await expect(student.getByRole("article", { name: title, exact: true })).toBeVisible();
    const adminLogin = await request.post("/api/empLogin", { data: { username: "admin01", password: process.env.UPSELF_BOOKMARK_ADMIN_PASSWORD } });
    const adminToken = (await adminLogin.json()).token;
    const headers = { Authorization: "Bearer " + adminToken };
    const enrollment = "/api/learning/courses/" + cid + "/students";
    expect((await (await request.delete(enrollment + "/" + process.env.UPSELF_BOOKMARK_STUDENT_ID, { headers })).json()).code).toBe(200);
    await student.getByRole("button", { name: "Refresh saved courses", exact: true }).click();
    await expect(student.getByRole("heading", { name: "Course unavailable", exact: true })).toBeVisible();
    await expect(student.getByRole("heading", { name: title, exact: true })).toHaveCount(0);
    await expect(student.getByRole("link", { name: /^Open course/ })).toHaveCount(0);
    await student.screenshot({ path: path.join(process.env.UPSELF_BOOKMARK_WORK!, "unavailable-course.png"), fullPage: true });
    await student.getByRole("button", { name: /^Remove from saved:/ }).click();
    await expect(student.getByText("No saved courses yet.", { exact: true })).toBeVisible();
    expect((await (await request.post(enrollment, { headers, data: { studentId: Number(process.env.UPSELF_BOOKMARK_STUDENT_ID) } })).json()).code).toBe(200);
    await request.post("/api/logout", { headers });
    const ownHeaders = { Authorization: "Bearer " + await token(student) };
    const ids: number[] = JSON.parse(process.env.UPSELF_BOOKMARK_COURSES!);
    for (const id of ids.slice(0, 11)) expect((await (await request.put(base + "/" + id, { headers: ownHeaders })).json()).code).toBe(200);
    await student.getByRole("button", { name: "Refresh saved courses", exact: true }).click();
    await expect(student.getByRole("article")).toHaveCount(10);
    await student.getByRole("button", { name: "Next", exact: true }).click();
    await expect(student.getByRole("article", { name: title, exact: true })).toBeVisible();
    await student.getByRole("button", { name: /^Remove from saved:/ }).click();
    await expect(student.getByRole("article")).toHaveCount(10);
    await expect(student.getByRole("button", { name: "Next", exact: true })).toHaveCount(0);
    const other = await page();
    await login(other, "student", process.env.UPSELF_BOOKMARK_OTHER!, process.env.UPSELF_BOOKMARK_OTHER_PASSWORD!);
    await saved(other); await expect(other.getByText("No saved courses yet.", { exact: true })).toBeVisible();
    const otherHeaders = { Authorization: "Bearer " + await token(other) };
    const otherList = await request.get(base + "?studentId=" + process.env.UPSELF_BOOKMARK_STUDENT_ID, { headers: otherHeaders });
    expect((await otherList.json()).total).toBe(0);
    await findCourse(other);
    await other.getByRole("article", { name: title, exact: true }).getByRole("button", { name: /^Save course:/ }).click();
    await saved(other); await expect(other.getByRole("article", { name: title, exact: true })).toBeVisible();
    await other.getByRole("button", { name: /^Remove from saved:/ }).click();
    await expect(other.getByText("No saved courses yet.", { exact: true })).toBeVisible();
    expect((await (await request.get(base, { headers: ownHeaders })).json()).total).toBe(10);
    for (const [role, username, password] of [
      ["mentor", process.env.UPSELF_BOOKMARK_MENTOR!, process.env.UPSELF_BOOKMARK_MENTOR_PASSWORD!],
      ["admin", "admin01", process.env.UPSELF_BOOKMARK_ADMIN_PASSWORD!],
    ]) {
      const p = await page(); await login(p, role!, username!, password!);
      await expect(p.getByRole("navigation").getByRole("link", { name: "Saved courses", exact: true })).toHaveCount(0);
      await p.goto("/#/student/saved-courses"); await expect(p).toHaveURL(/\/403$/);
    }
    expect(errors).toEqual([]);
    console.log("PASS saved-course buttons, keyboard, error recovery, refresh/relogin persistence, detail roundtrip, mobile, unavailable removal, last-page fallback and three-role isolation");
  } finally {
    for (const c of contexts) {
      for (const p of c.pages()) {
        try {
          const auth = await p.evaluate(() => ({ token: sessionStorage.getItem("upself.token"), channel: sessionStorage.getItem("upself.channel") }));
          if (auth.token) await c.request.post((process.env.UPSELF_TEST_UI_URL || "http://127.0.0.1:5173") + (auth.channel === "admin" ? "/api/logout" : "/api/client/logout"), { headers: { Authorization: "Bearer " + auth.token } });
        } catch { /* Cleanup never replaces the original test failure. */ }
      }
      await c.close();
    }
  }
});
