import { test, expect } from "@playwright/test";
import type { BrowserContext, Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("knowledge and diaries: mentor authoring, student answers, sharing and role isolation", async ({
  browser,
}) => {
  test.setTimeout(240000);
  const fixturePath = process.env.UPSELF_KNOWLEDGE_FIXTURE;
  test.skip(
    !fixturePath,
    "Run scripts/verify-knowledge.py --browser to create disposable fixtures.",
  );
  const fixture = JSON.parse(fs.readFileSync(fixturePath!, "utf8"));
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  const screenshot = (page: Page, name: string) =>
    page.screenshot({
      path: path.join(process.env.UPSELF_SCREENSHOTS!, name),
      fullPage: true,
    });
  async function login(
    actor: { username: string; password: string },
    role: string,
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
  const questionCard = (page: Page, text: string) =>
    page
      .locator("article.panel")
      .filter({ has: page.getByRole("heading", { name: text, exact: true }) });
  const answerForm = (page: Page, text: string) =>
    page
      .locator("form.knowledge-answer")
      .filter({ has: page.getByRole("group", { name: text, exact: true }) });
  async function confirm(page: Page) {
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
  }
  async function closeDialog(page: Page) {
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Close this dialog", exact: true })
      .click();
  }
  try {
    const mentor = await login(fixture.actors.mentor, "mentor");
    await mentor
      .getByRole("navigation", { name: "Mentor navigation", exact: true })
      .getByRole("link", { name: "Students", exact: true })
      .click();
    await mentor
      .getByRole("link", { name: "Knowledge questions", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", { name: "Knowledge questions", exact: true }),
    ).toBeVisible();
    const text = "Can you name one safe study step?",
      edited = "Can you describe one safe study step?";
    async function openCreate() {
      await mentor
        .getByRole("button", { name: "Add question", exact: true })
        .click();
      const dialog = mentor.getByRole("dialog");
      await dialog
        .getByLabel("Search", { exact: true })
        .fill(fixture.courseTitle);
      await dialog.getByRole("button", { name: "Search", exact: true }).click();
      await dialog
        .getByRole("button", { name: "Confirm", exact: true })
        .click();
      return dialog;
    }
    let dialog = await openCreate();
    await dialog
      .getByLabel("Question", { exact: true })
      .fill("Cancelled question");
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(
      mentor.getByRole("heading", { name: "Cancelled question", exact: true }),
    ).toHaveCount(0);
    dialog = await openCreate();
    await dialog.getByLabel("Question", { exact: true }).fill(text);
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(questionCard(mentor, text)).toBeVisible();
    await questionCard(mentor, text)
      .getByRole("button", { name: "Edit", exact: true })
      .click();
    await mentor
      .getByRole("dialog")
      .getByLabel("Question", { exact: true })
      .fill(edited);
    await mentor
      .getByRole("dialog")
      .getByRole("button", { name: "Save", exact: true })
      .click();
    await expect(questionCard(mentor, edited)).toBeVisible();
    await questionCard(mentor, edited)
      .getByRole("button", { name: "Publish", exact: true })
      .click();
    await expect(questionCard(mentor, edited).locator(".status")).toHaveText(
      "Published",
    );

    const student = await login(fixture.actors.fresh, "student");
    const mainNav = student.getByRole("navigation", {
      name: "Student navigation",
      exact: true,
    });
    await expect(mainNav.getByRole("link")).toHaveCount(5);
    await mainNav
      .getByRole("link", { name: "My check-in", exact: true })
      .click();
    const sections = student.locator(".section-nav");
    await expect(sections.getByRole("link")).toHaveCount(3);
    await sections
      .getByRole("link", { name: "Check knowledge", exact: true })
      .click();
    await expect(
      student.getByRole("heading", { name: "Check knowledge", exact: true }),
    ).toBeVisible();
    let form = answerForm(student, edited);
    await expect(form).toBeVisible();
    await expect(form.locator("input:checked")).toHaveCount(0);
    await expect(
      form.getByRole("button", { name: "Save answer", exact: true }),
    ).toBeDisabled();
    for (const answer of ["Not sure", "Yes", "No"]) {
      await form.getByRole("radio", { name: answer, exact: true }).check();
      await form
        .getByRole("button", { name: "Save answer", exact: true })
        .click();
      await expect(form.getByRole("status")).toHaveText("Answer saved.");
      await student.reload();
      form = answerForm(student, edited);
      await expect(
        form.getByRole("radio", { name: answer, exact: true }),
      ).toBeChecked();
    }
    await student.setViewportSize({ width: 390, height: 844 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await screenshot(student, "knowledge-student-mobile.png");
    await student
      .getByRole("link", { name: "Reading settings", exact: true })
      .click();
    await student
      .getByLabel("Text size", { exact: true })
      .selectOption("extraLarge");
    await student
      .getByLabel("Contrast", { exact: true })
      .selectOption("highContrast");
    await student.goto("/#/student/knowledge");
    await expect(answerForm(student, edited)).toBeVisible();
    await student.setViewportSize({ width: 320, height: 844 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await screenshot(student, "knowledge-student-accessible.png");
    await student.goto("/#/preferences");
    await student
      .getByRole("button", { name: "Use platform defaults", exact: true })
      .click();
    await student.setViewportSize({ width: 1365, height: 900 });
    await student.goto("/#/student/mood");
    await expect(
      student.getByText(
        "All saved diaries, including earlier entries and voice recordings, are shared with your current course mentors.",
        { exact: true },
      ),
    ).toBeVisible();
    await student
      .getByLabel("I understand who can see my mood, diaries and recordings.", {
        exact: true,
      })
      .check();
    await student
      .getByRole("button", { name: "Continue to my mood", exact: true })
      .click();
    await student.getByRole("radio", { name: "Good", exact: true }).check();
    const diary = "Shared UI diary: One quiet learning step helped.";
    await student
      .getByLabel("My diary (optional)", { exact: true })
      .fill(diary);
    await student
      .getByRole("button", { name: "Save this week’s mood", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByText("Your mood is saved for this week.", { exact: true }),
    ).toBeVisible();

    await mentor.reload();
    const card = questionCard(mentor, edited);
    await card
      .getByRole("button", { name: "View answers", exact: true })
      .click();
    dialog = mentor.getByRole("dialog");
    await expect(
      dialog.getByRole("cell", { name: "No", exact: true }),
    ).toBeVisible();
    await expect(
      dialog.getByText(fixture.actors.fresh.username, { exact: false }),
    ).toBeVisible();
    await screenshot(mentor, "knowledge-mentor-answers.png");
    await closeDialog(mentor);
    await expect(
      card.getByRole("button", { name: "Delete", exact: true }),
    ).toBeDisabled();
    await card.getByRole("button", { name: "Edit", exact: true }).click();
    await expect(
      mentor.getByRole("dialog").getByLabel("Question", { exact: true }),
    ).toHaveAttribute("readonly", "");
    await mentor
      .getByRole("dialog")
      .getByRole("button", { name: "Cancel", exact: true })
      .click();
    await card
      .getByRole("button", { name: "Move to draft", exact: true })
      .click();
    await expect(questionCard(mentor, edited).locator(".status")).toHaveText(
      "Draft",
    );
    await student.goto("/#/student/knowledge");
    await expect(answerForm(student, edited)).toHaveCount(0);
    await questionCard(mentor, edited)
      .getByRole("button", { name: "Publish", exact: true })
      .click();
    await expect(questionCard(mentor, edited).locator(".status")).toHaveText(
      "Published",
    );
    await questionCard(mentor, edited)
      .getByRole("button", { name: "Archive", exact: true })
      .click();
    await confirm(mentor);
    await expect(questionCard(mentor, edited).locator(".status")).toHaveText(
      "Archived",
    );
    await student.reload();
    await expect(answerForm(student, edited)).toHaveCount(0);
    await questionCard(mentor, edited)
      .getByRole("button", { name: "Publish", exact: true })
      .click();
    await expect(questionCard(mentor, edited).locator(".status")).toHaveText(
      "Published",
    );
    await student.reload();
    await expect(
      answerForm(student, edited).getByRole("radio", {
        name: "No",
        exact: true,
      }),
    ).toBeChecked();
    await mentor.goto("/#/mentor/knowledge?courseId=" + fixture.courseId);
    await mentor
      .getByRole("button", { name: "Add question", exact: true })
      .click();
    dialog = mentor.getByRole("dialog");
    await dialog
      .getByLabel("Question", { exact: true })
      .fill("Disposable unanswered question?");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await questionCard(mentor, "Disposable unanswered question?")
      .getByRole("button", { name: "Delete", exact: true })
      .click();
    await confirm(mentor);
    await expect(
      questionCard(mentor, "Disposable unanswered question?"),
    ).toHaveCount(0);
    await mentor.goto("/#/mentor/mood/" + fixture.actors.fresh.id);
    await mentor.locator(".mood-history summary").click();
    await expect(mentor.getByText(diary, { exact: true })).toBeVisible();
    await expect(mentor.locator("textarea,input[type=radio]")).toHaveCount(0);
    await screenshot(mentor, "diary-mentor-readonly.png");
    await mentor.goto("/#/mentor/mood/" + fixture.actors.student.id);
    await expect(mentor.locator(".mood-history summary")).toHaveCount(2);
    await mentor.locator(".mood-history summary").first().click();
    await expect(
      mentor.getByText(fixture.sharedDiary, { exact: true }),
    ).toBeVisible();
    await expect(
      mentor.getByText(fixture.oldPrivate, { exact: true }),
    ).toHaveCount(1);
    await expect(mentor.locator(".mood-history b")).toHaveCount(0);

    const admin = await login(
      { username: "admin01", password: fixture.adminPassword },
      "admin",
    );
    await expect(admin.locator(".main-nav a")).toHaveCount(3);
    for (const url of [
      "/#/student/knowledge",
      "/#/mentor/knowledge",
      "/#/mentor/mood/" + fixture.actors.student.id,
    ]) {
      await admin.goto(url);
      await expect(admin).toHaveURL(/\/403$/);
    }
    await screenshot(admin, "knowledge-admin-access-denied.png");
    expect(errors).toEqual([]);
    console.log(
      "PASS Chrome: author/cancel/edit/publish/draft/archive/delete, all three answers, reload, mentor read-only diaries, old-note privacy, mobile/high contrast and administrator isolation",
    );
  } finally {
    for (const context of contexts) await context.close();
  }
});
