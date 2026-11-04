import { test, expect, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("three personal milestones, persistence, refresh and role boundaries", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixture = process.env.UPSELF_BADGE_FIXTURE;
  test.skip(
    !fixture,
    "Run scripts/verify-badges.py --browser with local services.",
  );
  const f = JSON.parse(fs.readFileSync(fixture!, "utf8"));
  const contexts = [];
  const errors: string[] = [];
  async function login(
    role: string,
    account: { username: string; password: string },
  ) {
    const context = await browser.newContext();
    contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(account.username);
    await page.getByLabel("Password", { exact: true }).fill(account.password);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/" + role + "/home$"));
    return page;
  }
  async function stats(page: Page) {
    await page.goto("/#/student/statistics");
    await expect(
      page.getByRole("heading", { name: "My badges", exact: true }),
    ).toBeVisible();
    await expect(page.locator(".badge-list li")).toHaveCount(3);
  }
  try {
    const student = await login("student", f.actors.bob);
    await expect(
      student
        .getByRole("navigation", { name: "Student navigation" })
        .getByRole("link"),
    ).toHaveCount(5);
    await stats(student);
    await expect(
      student.locator(".badge-state").filter({ hasText: "To try" }),
    ).toHaveCount(3);
    await student
      .getByRole("button", { name: "Refresh badges", exact: true })
      .click();
    await expect(student.locator(".badge-list li")).toHaveCount(3);
    await student.goto("/#/student/courses/" + f.courseId);
    await student
      .getByRole("button", { name: "Mark as complete", exact: true })
      .click();
    await expect(
      student
        .getByRole("status")
        .filter({ hasText: "Badge earned: First step" }),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Mark as not complete", exact: true })
      .click();
    await stats(student);
    await expect(
      student.locator(".badge-list li").filter({ hasText: "First step" }),
    ).toContainText("Earned");
    await student.goto("/#/student/goals");
    await student
      .getByRole("button", { name: "Create goal", exact: true })
      .click();
    await student
      .getByLabel("My next step", { exact: true })
      .fill("One small browser step");
    await student
      .getByRole("dialog")
      .getByRole("button", { name: "Save", exact: true })
      .click();
    await student
      .getByRole("button", { name: "Mark as complete", exact: true })
      .click();
    await expect(
      student
        .getByRole("status")
        .filter({ hasText: "Badge earned: Goal reached" }),
    ).toBeVisible();
    await student.goto("/#/student/assignments/" + f.assignmentId);
    await student.locator("#work-answer").fill("My browser milestone answer.");
    await student
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await student
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
    await expect(
      student
        .getByRole("status")
        .filter({ hasText: "Badge earned: First submission" }),
    ).toBeVisible();
    await stats(student);
    await student.reload();
    await expect(
      student.locator(".badge-state").filter({ hasText: "Earned" }),
    ).toHaveCount(3);
    await student.route(
      "**/api/client/learning/badges",
      (route) =>
        route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ code: 500, msg: "Temporary badge outage." }),
        }),
      { times: 1 },
    );
    await student
      .getByRole("button", { name: "Refresh badges", exact: true })
      .click();
    await expect(
      student.getByText("Temporary badge outage.", { exact: true }),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Refresh badges", exact: true })
      .click();
    await expect(student.locator(".badge-list li")).toHaveCount(3);
    await student.setViewportSize({ width: 320, height: 800 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(
        process.env.UPSELF_SCREENSHOTS!,
        "student-badges-mobile.png",
      ),
      fullPage: true,
    });
    for (const [role, account] of [
      ["mentor", f.actors.mentor],
      ["admin", f.admin],
    ] as const) {
      const page = await login(role, account);
      await expect(
        page.getByRole("heading", { name: "My badges", exact: true }),
      ).toHaveCount(0);
      await page.goto("/#/student/statistics");
      await expect(
        page.getByRole("heading", { name: "My badges", exact: true }),
      ).toHaveCount(0);
    }
    expect(errors).toEqual([]);
  } finally {
    for (const context of contexts) await context.close();
  }
});
