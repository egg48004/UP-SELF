import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
test("FR-34/58/54 reporting, private moderation, follow-ups and CSV exports", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixture = process.env.UPSELF_SUPPORT_FIXTURE;
  test.skip(!fixture, "Run scripts/verify-support.py --browser.");
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
  async function discussion(p: Page, student: boolean) {
    await p.goto(
      "/#/" +
        (student ? "student" : "manage") +
        "/courses/" +
        f.courseId +
        "/discussions",
    );
    await p.getByRole("button", { name: /Peer support group.*Active/ }).click();
    await expect(
      p.getByRole("heading", { name: "Peer support group", exact: true }),
    ).toBeVisible();
  }
  async function report(
    p: Page,
    text: string,
    reason: string,
    details: string,
    cancelFirst = false,
  ) {
    const row = p.locator("li.message").filter({ hasText: text });
    await row.locator("summary").click();
    await row.getByRole("button", { name: "Report user", exact: true }).click();
    let d = p.getByRole("dialog", { name: "Report user", exact: true });
    if (cancelFirst) {
      await d.getByRole("button", { name: "Cancel", exact: true }).click();
      await expect(d).toBeHidden();
      await row
        .getByRole("button", { name: "Report user", exact: true })
        .click();
      d = p.getByRole("dialog", { name: "Report user", exact: true });
    }
    await d.getByRole("button", { name: "Submit report", exact: true }).click();
    await expect(d.getByRole("alert")).toHaveText("Choose a reason.");
    await d.getByLabel(reason, { exact: true }).check();
    await d.getByLabel("Details (optional)", { exact: true }).fill(details);
    const [response] = await Promise.all([
      p.waitForResponse(
        (r) => r.url().endsWith("/reports") && r.request().method() === "POST",
      ),
      d.getByRole("button", { name: "Submit report", exact: true }).click(),
    ]);
    const body = await response.json();
    expect(body.code).toBe(200);
    await expect(d).toBeHidden();
    await expect(
      p.getByText("Report submitted.", { exact: true }),
    ).toBeVisible();
    return body.data as number;
  }
  async function download(p: Page, kind: string) {
    const [file] = await Promise.all([
      p.waitForEvent("download"),
      p.getByRole("button", { name: "Download CSV", exact: true }).click(),
    ]);
    expect(file.suggestedFilename()).toBe("up-self-" + kind + ".csv");
    const content = fs.readFileSync((await file.path())!, "utf8");
    expect(content.charCodeAt(0)).toBe(0xfeff);
    expect(content).not.toContain("PRIVATE-JOURNAL");
    expect(content).not.toContain("password");
    await expect(
      p.getByRole("status").filter({ hasText: "CSV downloaded." }),
    ).toBeVisible();
    return content;
  }
  try {
    const student = await login(f.actors.student, "student");
    await expect(
      student
        .getByRole("navigation", { name: "Student navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(5);
    await discussion(student, true);
    await expect(
      student
        .locator("li.message")
        .filter({ hasText: "Student message for mentor reporting" })
        .locator("summary"),
    ).toHaveCount(0);
    const sid = await report(
      student,
      "UI message to report",
      "Spam",
      "UI private report details.",
      true,
    );
    await student.locator('.account-nav a[href="#/profile/security"]').click();
    await student
      .getByRole("navigation", { name: "My account", exact: true })
      .getByRole("link", { name: "My reports", exact: true })
      .click();
    await expect(
      student.getByRole("heading", { name: "My reports", exact: true }),
    ).toBeVisible();
    await expect(
      student.locator(".reports-list article").first(),
    ).toContainText("Pending");
    await student.getByRole("button", { name: "Next", exact: true }).click();
    await expect(student.getByText(/Page 2/)).toBeVisible();
    await student
      .getByRole("button", { name: "Previous", exact: true })
      .click();
    await student.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() =>
        student.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      )
      .toBe(true);
    await student.screenshot({
      path: path.join(folder, "student-my-reports.png"),
      fullPage: true,
    });
    const admin = await login(f.admin, "admin");
    await admin
      .locator("#main")
      .getByRole("link", { name: "Reports", exact: true })
      .click();
    await admin.locator('a[href="#/manage/reports/' + sid + '"]').click();
    await expect(
      admin.getByRole("heading", { name: "Reported message", exact: true }),
    ).toBeVisible();
    await expect(
      admin.getByText("UI message to report", { exact: true }),
    ).toBeVisible();
    await expect(
      admin.getByText("UI private report details.", { exact: true }),
    ).toBeVisible();
    await admin.getByRole("button", { name: "Dismiss", exact: true }).click();
    await expect(admin.getByRole("alert")).toHaveText(
      "Enter a short processing note.",
    );
    await admin
      .getByLabel("Processing note", { exact: true })
      .fill("Checked the reported message. No account action required.");
    await admin.getByRole("button", { name: "Dismiss", exact: true }).click();
    await expect(
      admin.getByText("Report updated.", { exact: true }),
    ).toBeVisible();
    await expect(
      admin.getByRole("heading", { name: "Processing result", exact: true }),
    ).toBeVisible();
    await admin.screenshot({
      path: path.join(folder, "admin-report-review.png"),
      fullPage: true,
    });
    await admin.getByRole("link", { name: /Back to reports/ }).click();
    await admin
      .getByLabel("Filter reports", { exact: true })
      .selectOption("DISMISSED");
    await expect(
      admin.locator('a[href="#/manage/reports/' + sid + '"]'),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Refresh status", exact: true })
      .click();
    await expect(
      student.locator(".reports-list article").first(),
    ).toContainText("Dismissed");
    await expect(
      student.getByText(
        "Checked the reported message. No account action required.",
        { exact: true },
      ),
    ).toHaveCount(0);
    const mentor = await login(f.actors.mentor, "mentor");
    await mentor
      .locator("#main")
      .getByRole("link", { name: "Follow-up reminders", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", { name: "Follow-up reminders", exact: true }),
    ).toBeVisible();
    await expect(mentor.locator(".reminders article")).toHaveCount(3);
    for (const [heading, href, title] of [
      [
        "Check in with this student",
        `/mentor/mood/${f.actors.student.id}`,
        f.actors.student.username,
      ],
      [
        "Learning activity has paused",
        `/manage/courses/${f.courseId}`,
        f.prefix + "course",
      ],
      [
        "Assignment needs follow-up",
        `/mentor/assignments/${f.assignmentId}`,
        "Practice assignment",
      ],
    ]) {
      const card = mentor
        .locator(".reminders article")
        .filter({
          has: mentor.getByRole("heading", { name: heading, exact: true }),
        });
      await card
        .getByRole("link", { name: "View related record", exact: true })
        .click();
      await expect(mentor).toHaveURL(new RegExp("#" + href + "$"));
      await expect(mentor.locator("h1")).toContainText(title);
      await mentor.goto("/#/mentor/follow-ups");
      await expect(mentor.locator(".reminders article")).toHaveCount(3);
    }
    const mood = mentor.locator(".reminders article").filter({
      has: mentor.getByRole("heading", {
        name: "Check in with this student",
        exact: true,
      }),
    });
    await mood
      .getByRole("button", { name: "Mark as reviewed", exact: true })
      .click();
    await expect(mentor.locator(".reminders article")).toHaveCount(2);
    await mentor
      .getByRole("button", { name: "Refresh reminders", exact: true })
      .click();
    await expect(mentor.locator(".reminders article")).toHaveCount(2);
    await mentor
      .getByLabel("Reminder status", { exact: true })
      .selectOption("REVIEWED");
    await expect(mentor.locator(".reminders article")).toHaveCount(1);
    await mentor
      .locator(".reminders article")
      .getByRole("link", { name: "Mentor notes", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", { name: "Mentor notes", exact: true }),
    ).toBeVisible();
    await mentor.goto("/#/mentor/follow-ups");
    await mentor
      .getByLabel("Reminder status", { exact: true })
      .selectOption("ENDED");
    await expect(
      mentor.getByText("No reminders in this view.", { exact: true }),
    ).toBeVisible();
    await mentor
      .getByLabel("Reminder status", { exact: true })
      .selectOption("PENDING");
    await expect(mentor.locator(".reminders article")).toHaveCount(2);
    await mentor.screenshot({
      path: path.join(folder, "mentor-follow-ups.png"),
      fullPage: true,
    });
    await mentor.getByRole("link", { name: /Back to students/ }).click();
    const overview = mentor
      .getByRole("row")
      .filter({ hasText: f.actors.student.username });
    await overview.getByRole("link", { name: /Needs follow-up/ }).click();
    await expect(mentor).toHaveURL(
      new RegExp("studentId=" + f.actors.student.id),
    );
    await expect(mentor.locator(".reminders article")).toHaveCount(2);
    await discussion(mentor, false);
    const mid = await report(
      mentor,
      "Student message for mentor reporting",
      "Hurtful behaviour",
      "UI mentor report.",
    );
    await admin.goto("/#/manage/reports");
    await admin
      .getByLabel("Filter reports", { exact: true })
      .selectOption("PENDING");
    await admin
      .getByRole("button", { name: "Refresh reports", exact: true })
      .click();
    await admin.locator('a[href="#/manage/reports/' + mid + '"]').click();
    await admin
      .getByLabel("Processing note", { exact: true })
      .fill("Reviewed and followed up using existing account procedures.");
    await admin.getByRole("button", { name: "Resolve", exact: true }).click();
    await expect(
      admin.getByText("Report updated.", { exact: true }),
    ).toBeVisible();
    await admin.getByRole("link", { name: /Back to reports/ }).click();
    await admin
      .getByLabel("Filter reports", { exact: true })
      .selectOption("RESOLVED");
    await expect(
      admin.locator('a[href="#/manage/reports/' + mid + '"]'),
    ).toBeVisible();
    await admin.getByRole("link", { name: "Export data", exact: true }).click();
    await expect(
      admin.getByRole("heading", { name: "Export data", exact: true }),
    ).toBeVisible();
    await admin.getByLabel("Search (optional)", { exact: true }).fill(f.prefix);
    await admin.getByLabel("Role", { exact: true }).selectOption("student");
    await admin.getByLabel("Status", { exact: true }).selectOption("0");
    expect(await download(admin, "users")).toContain(f.actors.student.username);
    await admin
      .getByLabel("Report type", { exact: true })
      .selectOption("courses");
    await admin.getByLabel("Status", { exact: true }).selectOption("PUBLISHED");
    expect(await download(admin, "courses")).toContain(f.prefix + "course");
    await admin
      .getByLabel("Report type", { exact: true })
      .selectOption("learning");
    await admin
      .getByLabel("Course", { exact: true })
      .selectOption(String(f.courseId));
    expect(await download(admin, "learning")).toContain("Completed chapters");
    await admin.screenshot({
      path: path.join(folder, "admin-csv-export.png"),
      fullPage: true,
    });
    const other = await login(f.actors.otherstudent, "student");
    await other.goto("/#/profile/reports");
    await expect(other.locator(".reports-list article")).toHaveCount(1);
    await expect(
      other.getByText("UI private report details.", { exact: true }),
    ).toHaveCount(0);
    const otherMentor = await login(f.actors.othermentor, "mentor");
    await otherMentor.goto("/#/mentor/follow-ups");
    await expect(
      otherMentor.getByText("No reminders in this view.", { exact: true }),
    ).toBeVisible();
    for (const url of [
      "/#/manage/reports",
      "/#/manage/exports",
      "/#/mentor/follow-ups",
    ]) {
      await student.goto(url);
      await expect(student).toHaveURL(/\/403$/);
    }
    await mentor.goto("/#/manage/exports");
    await expect(mentor).toHaveURL(/\/403$/);
    await admin.goto("/#/mentor/follow-ups");
    await expect(admin).toHaveURL(/\/403$/);
    expect(errors).toEqual([]);
  } finally {
    for (const c of contexts) await c.close();
  }
});
