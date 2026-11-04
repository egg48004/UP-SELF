import { test, expect, type BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("platform questionnaires: mentor controls visibility for students without courses", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixture = process.env.UPSELF_QUESTIONNAIRE_FIXTURE;
  test.skip(
    !fixture,
    "Run scripts/verify-questionnaires.py --browser with local services.",
  );
  const f = JSON.parse(fs.readFileSync(fixture!, "utf8"));
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(
    role: string,
    actor: { username: string; password: string },
  ) {
    const context = await browser.newContext();
    contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(actor.username);
    await page.getByLabel("Password", { exact: true }).fill(actor.password);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/" + role + "/home$"));
    return page;
  }
  try {
    const mentor = await login("mentor", f.actors.mentor);
    const student = await login("student", f.actors.student);
    await expect(
      student
        .getByRole("navigation", { name: "Student navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(5);
    await mentor
      .locator('a[href="#/mentor/questionnaires"]')
      .click();
    await mentor
      .getByRole("button", { name: "Add questionnaire", exact: true })
      .click();
    const title = f.prefix + "Browser questionnaire";
    const dialog = mentor.getByRole("dialog");
    await dialog.getByLabel("Title", { exact: true }).fill(title);
    await dialog
      .getByLabel("Questionnaire link", { exact: true })
      .fill("https://example.org/qtest-form");
    await expect(
      dialog.getByLabel("Visible to all students", { exact: true }),
    ).not.toBeChecked();
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const ownRow = () =>
      mentor.locator(".questionnaire-list li").filter({ hasText: title });
    await expect(ownRow()).toContainText("Hidden");
    await student.goto("/#/student/questionnaires");
    await expect(
      student.getByRole("heading", { name: "Questionnaires", exact: true }),
    ).toBeVisible();
    await expect(
      student.getByRole("link", { name: "Questionnaires", exact: true }),
    ).toBeVisible();
    await expect(
      student.locator(".questionnaire-list li").filter({ hasText: title }),
    ).toHaveCount(0);
    await ownRow()
      .getByRole("button", { name: "Show to students", exact: true })
      .click();
    await expect(ownRow()).toContainText("Visible");
    await student
      .getByRole("button", { name: "Refresh questionnaires", exact: true })
      .click();
    const studentRow = () =>
      student.locator(".questionnaire-list li").filter({ hasText: title });
    const link = studentRow().getByRole("link", {
      name: "Open questionnaire: " + title,
      exact: true,
    });
    await expect(link).toHaveAttribute(
      "href",
      "https://example.org/qtest-form",
    );
    await expect(link).toHaveAttribute("target", "_blank");
    await expect(link).toHaveAttribute("rel", "noopener noreferrer");
    await student
      .context()
      .route("https://example.org/qtest-form", (route) =>
        route.fulfill({
          contentType: "text/html",
          body: "<h1>External questionnaire navigation check</h1>",
        }),
      );
    const popup = student.waitForEvent("popup");
    await link.click();
    const opened = await popup;
    await expect(opened.getByRole("heading")).toHaveText(
      "External questionnaire navigation check",
    );
    await opened.close();
    await ownRow()
      .getByRole("button", { name: "Hide from students", exact: true })
      .click();
    await expect(ownRow()).toContainText("Hidden");
    await student
      .getByRole("button", { name: "Refresh questionnaires", exact: true })
      .click();
    await expect(studentRow()).toHaveCount(0);
    await ownRow().getByRole("button", { name: "Edit", exact: true }).click();
    await mentor
      .getByRole("dialog")
      .getByLabel("Questionnaire link", { exact: true })
      .fill("https://example.org/qtest-revised");
    await mentor
      .getByRole("dialog")
      .getByLabel("Visible to all students", { exact: true })
      .check();
    await mentor
      .getByRole("dialog")
      .getByRole("button", { name: "Save", exact: true })
      .click();
    await expect(ownRow()).toContainText("https://example.org/qtest-revised");
    await student
      .getByRole("button", { name: "Refresh questionnaires", exact: true })
      .click();
    await expect(studentRow().getByRole("link")).toHaveAttribute(
      "href",
      "https://example.org/qtest-revised",
    );
    await student.setViewportSize({ width: 320, height: 800 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(
        process.env.UPSELF_SCREENSHOTS!,
        "student-questionnaires-mobile.png",
      ),
      fullPage: true,
    });
    const admin = await login("admin", f.admin);
    await admin.goto("/#/mentor/questionnaires");
    await expect(
      admin.getByRole("heading", { name: "Questionnaires", exact: true }),
    ).toHaveCount(0);
    await ownRow()
      .getByRole("button", { name: "Delete questionnaire", exact: true })
      .click();
    await mentor
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
    await expect(ownRow()).toHaveCount(0);
    await student
      .getByRole("button", { name: "Refresh questionnaires", exact: true })
      .click();
    await expect(studentRow()).toHaveCount(0);
    expect(errors).toEqual([]);
  } finally {
    for (const context of contexts) await context.close();
  }
});
