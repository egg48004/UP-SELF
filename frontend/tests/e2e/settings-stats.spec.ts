import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("FR-55/27 platform defaults, personal choice and private learning statistics", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixture = process.env.UPSELF_SETTINGS_FIXTURE;
  test.skip(!fixture, "Run scripts/verify-settings-stats.py --browser.");
  const f = JSON.parse(fs.readFileSync(fixture!, "utf8")),
    folder = process.env.UPSELF_SCREENSHOTS!;
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(
    actor: { username: string; password: string },
    role: string,
  ) {
    const c = await browser.newContext();
    contexts.push(c);
    const p = await c.newPage();
    p.on("pageerror", (e) => errors.push(e.message));
    await p.goto("/#/login");
    await p.getByLabel("Choose your role", { exact: true }).selectOption(role);
    await p.getByLabel("Username", { exact: true }).fill(actor.username);
    await p.getByLabel("Password", { exact: true }).fill(actor.password);
    await p.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(p).toHaveURL(new RegExp("/" + role + "/home$"));
    return p;
  }
  async function chooseDefaults(
    p: Page,
    size: string,
    contrast: string,
    motion: boolean,
  ) {
    await p.getByLabel("Text size", { exact: true }).selectOption(size);
    await p.getByLabel("Contrast", { exact: true }).selectOption(contrast);
    await p
      .getByLabel("Reduce motion by default", { exact: true })
      .setChecked(motion);
    await p.getByRole("button", { name: "Save defaults", exact: true }).click();
    await expect(
      p.getByText("Platform defaults saved.", { exact: true }),
    ).toBeVisible();
  }
  try {
    const admin = await login(f.actors.admin, "admin");
    await expect(admin.locator(".main-nav a")).toHaveCount(5);
    await admin
      .locator("#main")
      .getByRole("link", { name: "Platform reading defaults", exact: true })
      .click();
    await chooseDefaults(admin, "large", "highContrast", true);
    await admin
      .getByLabel("Text size", { exact: true })
      .selectOption("extraLarge");
    await admin
      .getByRole("button", { name: "Reload saved defaults", exact: true })
      .click();
    await expect(admin.getByLabel("Text size", { exact: true })).toHaveValue(
      "large",
    );
    await admin.reload();
    await expect(admin.getByLabel("Contrast", { exact: true })).toHaveValue(
      "highContrast",
    );
    await expect(
      admin.getByLabel("Reduce motion by default", { exact: true }),
    ).toBeChecked();
    await admin.screenshot({
      path: path.join(folder, "admin-platform-defaults.png"),
      fullPage: true,
    });
    await admin.getByRole("link", { name: /Back to home/ }).click();
    await expect(admin).toHaveURL(/admin\/home$/);
    await admin
      .locator("#main")
      .getByRole("link", { name: "Platform reading defaults", exact: true })
      .click();
    const student = await login(f.actors.student, "student");
    await expect(student.locator(".main-nav a")).toHaveCount(5);
    await expect(student.locator("html")).toHaveAttribute("data-size", "large");
    await expect(student.locator("html")).toHaveAttribute(
      "data-contrast",
      "highContrast",
    );
    await expect(student.locator("html")).toHaveAttribute(
      "data-motion",
      "true",
    );
    expect(
      await student.evaluate(() =>
        localStorage.getItem("upself.preferences.v2"),
      ),
    ).toBeNull();
    await student
      .getByRole("link", { name: "Reading settings", exact: true })
      .click();
    await student
      .getByLabel("Text size", { exact: true })
      .selectOption("extraLarge");
    await student
      .getByLabel("Contrast", { exact: true })
      .selectOption("standard");
    await student.getByLabel("Reduce motion", { exact: true }).uncheck();
    await student.reload();
    await expect(student.getByLabel("Text size", { exact: true })).toHaveValue(
      "extraLarge",
    );
    await chooseDefaults(admin, "standard", "standard", false);
    await student.reload();
    await expect(student.getByLabel("Text size", { exact: true })).toHaveValue(
      "extraLarge",
    );
    await expect(
      student.getByLabel("Reduce motion", { exact: true }),
    ).not.toBeChecked();
    await student
      .getByRole("button", { name: "Use platform defaults", exact: true })
      .click();
    await expect(student.getByLabel("Text size", { exact: true })).toHaveValue(
      "standard",
    );
    await expect(
      student.getByText("Using platform defaults.", { exact: true }),
    ).toBeVisible();
    await student.route("**/api/accessibility/defaults", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 503,
          msg: "Defaults temporarily unavailable.",
        }),
      }),
    );
    await student.reload();
    await expect(
      student.getByText(
        "Platform defaults are unavailable. Your current settings still work.",
        { exact: true },
      ),
    ).toBeVisible();
    await student
      .getByLabel("Text size", { exact: true })
      .selectOption("large");
    await expect(student.locator("html")).toHaveAttribute("data-size", "large");
    await student.unroute("**/api/accessibility/defaults");
    await student
      .getByRole("button", { name: "Retry platform defaults", exact: true })
      .click();
    await expect(
      student.getByRole("button", {
        name: "Retry platform defaults",
        exact: true,
      }),
    ).toHaveCount(0);
    await expect(student.getByLabel("Text size", { exact: true })).toHaveValue(
      "large",
    );
    await student
      .getByRole("button", { name: "Use platform defaults", exact: true })
      .click();
    await student
      .getByRole("link", { name: /My learning/, exact: false })
      .click();
    await student
      .getByRole("link", { name: "Learning stats", exact: true })
      .click();
    await expect(
      student.getByRole("heading", { name: "Learning stats", exact: true }),
    ).toBeVisible();
    await expect(
      student.getByText("2 of 4 chapters done · 50% complete", { exact: true }),
    ).toBeVisible();
    const states = student.locator(".stats-statuses");
    await expect(
      states.locator("div").filter({ hasText: "Not submitted" }).locator("dd"),
    ).toHaveText("2");
    await expect(
      states
        .locator("div")
        .filter({ hasText: "Waiting for feedback" })
        .locator("dd"),
    ).toHaveText("1");
    await expect(
      states.locator("div").filter({ hasText: "Feedback ready" }).locator("dd"),
    ).toHaveText("1");
    await expect(
      states
        .locator("div")
        .filter({ hasText: "Changes requested" })
        .locator("dd"),
    ).toHaveText("1");
    await expect(student.locator(".stats-courses li")).toHaveCount(10);
    await student.getByRole("button", { name: "Next", exact: true }).click();
    await expect(student.locator(".stats-courses li")).toHaveCount(1);
    await student
      .getByRole("link", { name: f.prefix + "Main course", exact: true })
      .click();
    await expect(student).toHaveURL(
      new RegExp("student/courses/" + f.courseId + "$"),
    );
    await student.getByRole("button", { name: /Step 3/ }).click();
    await student
      .getByRole("button", { name: "Mark as complete", exact: true })
      .click();
    await expect(
      student.getByText("Progress saved.", { exact: true }),
    ).toBeVisible();
    await student
      .getByRole("link", { name: "Learning stats", exact: true })
      .click();
    await expect(
      student.getByText("3 of 4 chapters done · 75% complete", { exact: true }),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Refresh stats", exact: true })
      .click();
    await expect(
      student.getByText("3 of 4 chapters done · 75% complete", { exact: true }),
    ).toBeVisible();
    await student.screenshot({
      path: path.join(folder, "student-learning-stats.png"),
      fullPage: true,
    });
    await student.setViewportSize({ width: 390, height: 844 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(folder, "student-learning-stats-mobile.png"),
      fullPage: true,
    });
    await student.setViewportSize({ width: 1365, height: 900 });
    await student.getByRole("button", { name: "Next", exact: true }).click();
    await student
      .getByRole("button", { name: "Previous", exact: true })
      .click();
    await expect(student.locator(".stats-courses li")).toHaveCount(10);
    const empty = await login(f.actors.empty, "student");
    await empty.getByRole("link", { name: /My learning/ }).click();
    await empty
      .getByRole("link", { name: "Learning stats", exact: true })
      .click();
    await expect(
      empty.getByRole("heading", { name: "No courses yet.", exact: true }),
    ).toBeVisible();
    const mentor = await login(f.actors.mentor, "mentor");
    await expect(mentor.locator(".main-nav a")).toHaveCount(4);
    await expect(
      mentor.getByRole("link", {
        name: "Platform reading defaults",
        exact: true,
      }),
    ).toHaveCount(0);
    await mentor.goto("/#/manage/accessibility");
    await expect(mentor).toHaveURL(/403$/);
    await mentor.goto("/#/student/statistics");
    await expect(mentor).toHaveURL(/403$/);
    await admin.goto("/#/student/statistics");
    await expect(admin).toHaveURL(/403$/);
    await student.goto("/#/manage/accessibility");
    await expect(student).toHaveURL(/403$/);
    const system = await browser.newContext({ reducedMotion: "reduce" });
    contexts.push(system);
    const systemPage = await system.newPage();
    await systemPage.goto("/#/login");
    await expect(systemPage.locator("html")).toHaveAttribute(
      "data-motion",
      "true",
    );
    expect(errors).toEqual([]);
  } finally {
    for (const c of contexts) await c.close();
  }
});
