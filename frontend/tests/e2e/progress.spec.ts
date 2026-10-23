import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("learning progress: administrator metadata, student completion/resume/history, mentor overview", async ({
  browser,
  request,
}) => {
  test.setTimeout(180000);
  const root = path.resolve(".."),
    demo = JSON.parse(
      fs.readFileSync(path.join(root, ".local/learning-demo.json"), "utf8"),
    );
  const course = demo.courses[0];
  const student = demo.users.find(
    (u: { username: string }) => u.username === "demo_p6_student01",
  );
  const mentor = demo.users.find(
    (u: { username: string }) => u.username === "demo_p6_mentor01",
  );
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(username: string, password: string, role: string) {
    const context = await browser.newContext();
    contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(username);
    await page.getByLabel("Password", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/" + role + "/home"));
    return page;
  }
  async function shot(page: Page, name: string) {
    await page.screenshot({
      path: path.join(process.env.UPSELF_SCREENSHOTS!, name),
      fullPage: true,
    });
  }
  const loginResult = await request.post("http://127.0.0.1:8080/empLogin", {
    data: {
      username: "admin01",
      password: process.env.UPSELF_ADMIN01_PASSWORD!,
    },
  });
  const adminToken = (await loginResult.json()).token;
  async function api(url: string, method = "GET", data?: unknown) {
    const r = await request.fetch("http://127.0.0.1:8080" + url, {
      method,
      data,
      headers: { Authorization: "Bearer " + adminToken },
    });
    const j = await r.json();
    expect(j.code).toBe(200);
    return j.data;
  }
  const original = await api("/learning/courses/" + course.id);
  try {
    const admin = await login(
      "admin01",
      process.env.UPSELF_ADMIN01_PASSWORD!,
      "admin",
    );
    await admin.goto("/#/manage/courses/" + course.id);
    await admin.getByRole("button", { name: "Edit", exact: true }).click();
    const dialog = admin.getByRole("dialog");
    await dialog
      .getByLabel("Difficulty", { exact: true })
      .selectOption("ADVANCED");
    await dialog
      .getByLabel("Estimated learning time (minutes)", { exact: true })
      .fill("55");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).not.toBeVisible();
    await expect(
      admin.getByText("Difficulty: Advanced", { exact: false }),
    ).toBeVisible();
    await expect(
      admin.getByText("About 55 minutes", { exact: false }),
    ).toBeVisible();
    await shot(admin, "progress-admin.png");
    const s = await login(student.username, student.password, "student");
    await s.goto("/#/student/courses/" + course.id);
    await expect(
      s.getByText("About 55 minutes", { exact: false }),
    ).toBeVisible();
    // Choose a specific chapter rather than relying on the previous demo visit.
    const nav = s.getByRole("navigation", { name: "Chapters", exact: true });
    await nav.getByRole("button").nth(1).click();
    await expect(
      s.getByRole("heading", { name: "Step 2: Choose one idea", exact: true }),
    ).toBeVisible();
    const undo = s.getByRole("button", {
      name: "Mark as not complete",
      exact: true,
    });
    await expect(undo).toBeEnabled();
    await undo.click();
    await expect(
      s.getByText("5 of 6 chapters completed · 83%", { exact: true }),
    ).toBeVisible();
    const mark = s.getByRole("button", {
      name: "Mark as complete",
      exact: true,
    });
    await expect(mark).toBeEnabled();
    await mark.click();
    await expect(
      s.getByText("6 of 6 chapters completed · 100%", { exact: true }),
    ).toBeVisible();
    await expect(
      s.getByRole("button", { name: "Mark as not complete", exact: true }),
    ).toBeEnabled();
    await s.reload();
    await expect(
      s.getByRole("heading", { name: "Step 2: Choose one idea", exact: true }),
    ).toBeVisible();
    expect(await s.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await shot(s, "progress-student.png");
    await s.goto("/#/student/home");
    await s
      .getByRole("link", { name: "Continue learning", exact: false })
      .first()
      .click();
    await expect(
      s.getByRole("heading", { name: "Step 2: Choose one idea", exact: true }),
    ).toBeVisible();
    await s
      .getByRole("navigation", { name: "Chapters", exact: true })
      .getByRole("button")
      .nth(2)
      .click();
    await expect(
      s.getByRole("heading", {
        name: "Step 3: List what you need",
        exact: true,
      }),
    ).toBeVisible();
    await s.getByRole("button", { name: "Previous", exact: false }).click();
    await expect(
      s.getByRole("heading", { name: "Step 2: Choose one idea", exact: true }),
    ).toBeVisible();
    await s.getByRole("button", { name: "Next", exact: false }).click();
    await expect(
      s.getByRole("heading", {
        name: "Step 3: List what you need",
        exact: true,
      }),
    ).toBeVisible();
    await s.goto("/#/student/history");
    await expect(
      s.getByRole("heading", { name: "Learning history", exact: false }),
    ).toBeVisible();
    await expect(s.locator(".history-list > li")).toHaveCount(10);
    await s.getByRole("button", { name: "Next", exact: false }).click();
    await expect(s.locator(".history-list > li")).toHaveCount(2);
    await s.getByRole("button", { name: "Previous", exact: false }).click();
    await expect(s.locator(".history-list > li")).toHaveCount(10);
    await s
      .getByRole("link", { name: "Open chapter", exact: false })
      .first()
      .click();
    await expect(
      s.getByRole("button", { name: "Mark as not complete", exact: true }),
    ).toBeVisible();
    await s.setViewportSize({ width: 390, height: 844 });
    await s.goto("/#/student/history");
    await expect(s.locator(".history-list > li")).toHaveCount(10);
    expect(
      await s.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await shot(s, "progress-history-mobile.png");
    const m = await login(mentor.username, mentor.password, "mentor");
    await m
      .getByRole("navigation")
      .getByRole("link", { name: "Student overview", exact: true })
      .click();
    await expect(
      m.getByRole("heading", { name: "Student overview", exact: true }),
    ).toBeVisible();
    const search = m.getByRole("textbox", {
      name: "Search student or course",
      exact: true,
    });
    await search.fill(student.username);
    await m.getByRole("button", { name: "Search", exact: true }).click();
    await expect(m.locator("tbody tr")).toHaveCount(2);
    await expect(m.locator("tbody tr").first()).toContainText(
      "6 of 6 chapters completed",
    );
    expect(await m.locator("main").innerText()).not.toContain(
      "DEMO private note",
    );
    await shot(m, "progress-mentor.png");
    await m
      .getByRole("link", { name: "View mood history", exact: true })
      .first()
      .click();
    await expect(m).toHaveURL(new RegExp("/mentor/mood/" + student.id));
    await m.goto("/#/mentor/students");
    await m
      .getByRole("link", { name: "View course assignments", exact: true })
      .first()
      .click();
    await expect(m).toHaveURL(/mentor\/assignments/);
    await m.goto("/#/mentor/students");
    await search.fill("not-a-real-student");
    await m.getByRole("button", { name: "Search", exact: true }).click();
    await expect(
      m.getByText("No students match this search.", { exact: true }),
    ).toBeVisible();
    await m.getByRole("button", { name: "Clear", exact: true }).click();
    await expect(m.locator("tbody tr")).toHaveCount(9);
    for (const [page, url] of [
      [admin, "/#/student/history"],
      [s, "/#/mentor/students"],
      [m, "/#/student/history"],
    ] as [Page, string][]) {
      await page.goto(url);
      await expect(page).toHaveURL(/403/);
    }
    expect(errors).toEqual([]);
    console.log(
      "PASS real Chrome three-role metadata, completion/undo, reload, resume, history pagination, mobile, overview/search/detail links and route permissions",
    );
  } finally {
    await api("/learning/courses/" + course.id, "PUT", original);
    for (const c of contexts) await c.close();
  }
});
