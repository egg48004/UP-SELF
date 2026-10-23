import { test, expect } from "@playwright/test";
import type { BrowserContext, Page } from "@playwright/test";
import path from "node:path";

test("self-efficacy: independent consent, three statements, persistence, trends and live mentor scope", async ({ browser, request }) => {
  test.skip(!process.env.UPSELF_EFFICACY_STUDENT, "Use scripts/verify-self-efficacy.py --browser for disposable accounts.");
  test.setTimeout(180000);
  const contexts: BrowserContext[] = [], errors: string[] = [];
  const work = process.env.UPSELF_EFFICACY_WORK!, sid = process.env.UPSELF_EFFICACY_STUDENT_ID!, hid = process.env.UPSELF_EFFICACY_HISTORY_ID!;
  async function login(role: string, actor: string) {
    const c = await browser.newContext(); contexts.push(c);
    const p = await c.newPage(); p.on("pageerror", error => errors.push(error.message));
    await p.goto("/#/login"); await p.getByLabel("Choose your role", { exact: true }).selectOption(role);
    const username = actor === "ADMIN" ? "admin01" : process.env["UPSELF_EFFICACY_" + actor]!;
    await p.getByLabel("Username", { exact: true }).fill(username);
    await p.getByLabel("Password", { exact: true }).fill(process.env["UPSELF_EFFICACY_" + actor + "_PASSWORD"]!);
    await p.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(p).toHaveURL(new RegExp("/" + role + "/home")); return p;
  }
  async function own(p: Page) {
    await p.getByRole("navigation", { name: "Student navigation", exact: true }).getByRole("link", { name: "My check-in", exact: true }).click();
    await p.getByRole("navigation", { name: "My check-in sections", exact: true }).getByRole("link", { name: "Self-belief", exact: true }).click();
    await expect(p.getByRole("heading", { name: "Self-belief", exact: true })).toBeVisible();
  }
  const savedText = "Your self-belief check-in is saved.";
  async function confirm(p: Page, answer = "Confirm") { await p.getByRole("dialog").getByRole("button", { name: answer, exact: true }).click(); }
  async function auth(p: Page) { return { Authorization: "Bearer " + await p.evaluate(() => sessionStorage.getItem("upself.token")) }; }
  try {
    const p = await login("student", "STUDENT");
    await own(p);
    const notice = p.getByRole("heading", { name: "Before your first self-belief check-in", exact: true });
    await expect(notice).toBeVisible(); await expect(p.getByRole("radio")).toHaveCount(0);
    await p.screenshot({ path: path.join(work, "self-belief-sharing-notice.png"), fullPage: true });
    await expect(p.getByRole("button", { name: "Continue to self-belief", exact: true })).toBeDisabled();
    await p.getByRole("link", { name: "Not now", exact: true }).click(); await expect(p).toHaveURL(/\/student\/home$/);
    await own(p); await expect(notice).toBeVisible();
    await p.getByLabel("I understand who can see my self-belief check-ins.", { exact: true }).check();
    await p.getByRole("button", { name: "Continue to self-belief", exact: true }).click();
    await expect(p.locator(".efficacy-form")).toBeVisible(); await expect(p.getByRole("radio")).toHaveCount(5);
    await expect(p.getByRole("radio", { checked: true })).toHaveCount(0);
    await p.screenshot({ path: path.join(work, "self-belief-question.png"), fullPage: true });
    for (const width of [320, 390]) {
      await p.setViewportSize({ width, height: 844 });
      expect(await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
      for (const option of await p.locator(".efficacy-choice").all()) {
        const box = await option.boundingBox();
        expect(box!.height).toBeGreaterThanOrEqual(44);
        expect(box!.x + box!.width).toBeLessThanOrEqual(width + 1);
      }
    }
    await p.screenshot({ path: path.join(work, "self-belief-question-mobile.png"), fullPage: true });
    await p.setViewportSize({ width: 1365, height: 900 });
    await expect(p.getByRole("button", { name: "Next", exact: true })).toBeDisabled();
    await p.getByRole("radio").nth(0).focus(); await p.keyboard.press("Space");
    await p.keyboard.press("ArrowDown"); await p.keyboard.press("ArrowUp"); await expect(p.getByRole("radio").nth(0)).toBeChecked();
    await p.getByRole("button", { name: "Next", exact: true }).click(); await expect(p.locator(".efficacy-form [role='status']")).toHaveText("Statement 2 of 3");
    await p.getByRole("radio").nth(1).check(); await p.getByRole("button", { name: "Previous", exact: true }).click();
    await expect(p.getByRole("radio").nth(0)).toBeChecked(); await p.getByRole("button", { name: "Next", exact: true }).click();
    await expect(p.getByRole("radio").nth(1)).toBeChecked();
    await p.getByRole("navigation", { name: "Student navigation", exact: true }).getByRole("link", { name: "Home", exact: true }).click();
    await confirm(p, "Cancel"); await expect(p).toHaveURL(/\/student\/self-efficacy$/);
    await p.getByRole("button", { name: "Next", exact: true }).click(); await p.getByRole("radio").nth(1).check();
    await p.getByRole("button", { name: "Save self-belief", exact: true }).click(); await confirm(p, "Cancel");
    await expect(p.getByRole("radio").nth(1)).toBeChecked();
    await p.route("**/api/client/self-efficacy/entries", route => route.fulfill({ json: { code: 500, msg: "The service is temporarily unavailable. Please try again later." } }), { times: 1 });
    await p.getByRole("button", { name: "Save self-belief", exact: true }).click(); await confirm(p);
    await expect(p.getByRole("alert")).toContainText("temporarily unavailable"); await expect(p.getByRole("radio").nth(1)).toBeChecked();
    await p.getByRole("button", { name: "Save self-belief", exact: true }).click(); await confirm(p);
    await expect(p.getByText(savedText, { exact: true })).toBeVisible(); await expect(p.getByRole("heading", { name: "Score: 1.67 / 5", exact: true })).toBeVisible();
    await expect(p.getByRole("button", { name: "Save self-belief", exact: true })).toHaveCount(0);
    await expect(p.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(1);
    await p.getByText("My answers", { exact: true }).click();
    await expect(p.locator(".efficacy-history details ol > li")).toHaveCount(3);
    await p.getByRole("button", { name: "View table", exact: true }).click(); await expect(p.getByRole("table").locator("tbody tr")).toHaveCount(1);
    await p.getByRole("button", { name: "View chart", exact: true }).click();
    await p.getByLabel("Time range", { exact: true }).selectOption("4"); await expect(p.locator(".efficacy-chart circle")).toHaveCount(1);
    await p.getByRole("button", { name: "Refresh self-belief", exact: true }).click();
    await expect(p.getByRole("heading", { name: "Score: 1.67 / 5", exact: true })).toBeVisible();
    await p.getByText("About this check-in", { exact: true }).click();
    await expect(p.getByRole("link", { name: "CC BY-NC-SA 4.0", exact: true })).toHaveAttribute("href", "https://creativecommons.org/licenses/by-nc-sa/4.0/");
    await p.screenshot({ path: path.join(work, "student-self-belief.png"), fullPage: true });
    await p.setViewportSize({ width: 390, height: 844 });
    expect(await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await expect.poll(async () => (await p.locator(".efficacy-chart circle").last().boundingBox())!.x).toBeLessThan(360);
    await p.screenshot({ path: path.join(work, "student-self-belief-mobile.png"), fullPage: true });
    await p.reload(); await expect(p.getByRole("heading", { name: "Score: 1.67 / 5", exact: true })).toBeVisible();
    await p.getByRole("button", { name: "Sign out", exact: true }).click();
    const again = await login("student", "STUDENT"); await own(again);
    await expect(again.getByRole("heading", { name: "Score: 1.67 / 5", exact: true })).toBeVisible(); await expect(again.getByRole("radio")).toHaveCount(0);
    const historical = await login("student", "HISTORY"); await own(historical);
    await expect(historical.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(10);
    await historical.getByRole("button", { name: "Next", exact: true }).click(); await expect(historical.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(3);
    await historical.getByRole("button", { name: "Previous", exact: true }).click();
    await historical.getByLabel("Time range", { exact: true }).selectOption("52"); await expect(historical.locator(".efficacy-chart circle")).toHaveCount(13);
    await historical.getByRole("button", { name: "View table", exact: true }).click(); await expect(historical.getByRole("table").locator("tbody tr")).toHaveCount(13);
    await historical.screenshot({ path: path.join(work, "self-belief-history-table.png"), fullPage: true });
    const mentor = await login("mentor", "MENTOR");
    await mentor.getByRole("navigation", { name: "Mentor navigation", exact: true }).getByRole("link", { name: "Students", exact: true }).click();
    await mentor.getByRole("navigation", { name: "Student sections", exact: true }).getByRole("link", { name: "Student check-ins", exact: true }).click();
    await mentor.getByPlaceholder("Search", { exact: true }).fill(process.env.UPSELF_EFFICACY_STUDENT!);
    await mentor.getByRole("button", { name: "Search", exact: true }).click();
    await mentor.getByRole("link", { name: "View check-ins: " + process.env.UPSELF_EFFICACY_STUDENT, exact: true }).click();
    await mentor.getByRole("navigation", { name: "Student check-ins", exact: true }).getByRole("link", { name: "Self-belief", exact: true }).click();
    await expect(mentor).toHaveURL(new RegExp("/mentor/self-efficacy/" + sid + "$"));
    await expect(mentor.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(1);
    await expect(mentor.getByText("Score: 1.67 / 5", { exact: true })).toBeVisible();
    await expect(mentor.getByText("My answers", { exact: true })).toHaveCount(0); await expect(mentor.getByRole("radio")).toHaveCount(0);
    await expect(mentor.getByRole("button", { name: "Save self-belief", exact: true })).toHaveCount(0);
    await mentor.screenshot({ path: path.join(work, "mentor-self-belief.png"), fullPage: true });
    await mentor.goto("/#/mentor/self-efficacy/" + hid);
    await expect(mentor.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(10);
    await mentor.getByRole("button", { name: "Next", exact: true }).click(); await expect(mentor.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(3);
    await mentor.goto("/#/mentor/self-efficacy/" + sid);
    const unrelated = await login("mentor", "OTHERMENTOR"); await unrelated.goto("/#/mentor/self-efficacy/" + sid);
    await expect(unrelated.getByRole("alert")).toContainText("not in your current published courses"); await expect(unrelated.locator(".efficacy-history")).toHaveCount(0);
    const admin = await login("admin", "ADMIN");
    await expect(admin.getByRole("link", { name: "My check-in", exact: true })).toHaveCount(0);
    await admin.goto("/#/student/self-efficacy"); await expect(admin).toHaveURL(/\/403$/);
    await admin.goto("/#/mentor/self-efficacy/" + sid); await expect(admin).toHaveURL(/\/403$/);
    const reassigned = await request.put("/api/learning/courses/" + process.env.UPSELF_EFFICACY_COURSE + "/mentor", { headers: await auth(admin), data: { mentorId: Number(process.env.UPSELF_EFFICACY_OTHERMENTOR_ID) } });
    expect((await reassigned.json()).code).toBe(200);
    await mentor.reload(); await expect(mentor.getByRole("alert")).toContainText("not in your current published courses"); await expect(mentor.locator(".efficacy-history")).toHaveCount(0);
    await unrelated.reload(); await expect(unrelated.locator(".efficacy-history .efficacy-history-list > li")).toHaveCount(1);
    expect(errors).toEqual([]);
    console.log("PASS independent first-use notice, one-statement flow, keyboard/back/cancel, unsaved guard, failed save retry, server mean, 28-day lock, reload/relogin, historical pagination/trend/table, mobile, current mentor read-only and live reassignment");
  } finally {
    for (const c of contexts) {
      for (const p of c.pages()) {
        try {
          const session = await p.evaluate(() => ({ token: sessionStorage.getItem("upself.token"), channel: sessionStorage.getItem("upself.channel") }));
          if (session.token) await c.request.post((process.env.UPSELF_TEST_UI_URL || "http://127.0.0.1:5173") + (session.channel === "admin" ? "/api/logout" : "/api/client/logout"), { headers: { Authorization: "Bearer " + session.token } });
        } catch { /* Cleanup must not replace the test result. */ }
      }
      await c.close();
    }
  }
});
