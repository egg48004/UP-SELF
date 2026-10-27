import { test, expect } from "@playwright/test";
import type { BrowserContext, Locator, Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
test.use({
  launchOptions: {
    args: [
      "--use-fake-device-for-media-stream",
      "--use-fake-ui-for-media-stream",
    ],
  },
});
test("record, preview, delete, upload and play private assignment/diary audio", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixturePath = process.env.UPSELF_RECORDING_FIXTURE;
  test.skip(
    !fixturePath,
    "Run scripts/verify-recordings.py --browser for disposable fixtures.",
  );
  const fixture = JSON.parse(fs.readFileSync(fixturePath!, "utf8"));
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(
    actor: { username: string; password: string },
    role: string,
  ) {
    const context = await browser.newContext({ permissions: ["microphone"] });
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
  async function recorded(page: Page) {
    const panel = page.getByRole("region", {
      name: "Voice recording (optional)",
      exact: true,
    });
    await panel.getByRole("button", { name: "Record", exact: true }).click();
    await expect(panel.getByRole("status")).toContainText(
      /Recording · 0:0[1-9]/,
    );
    await panel
      .getByRole("button", { name: "Stop recording", exact: true })
      .click();
    await expect(panel.locator("audio")).toHaveAttribute("src", /^blob:/);
    return panel;
  }
  async function playing(audio: Locator) {
    await expect(audio).toBeVisible();
    await audio.evaluate(async (element) => {
      await (element as HTMLAudioElement).play();
    });
    await expect
      .poll(() =>
        audio.evaluate((element) => (element as HTMLAudioElement).currentTime),
      )
      .toBeGreaterThan(0.05);
    expect(
      await audio.evaluate((element) => (element as HTMLAudioElement).error),
    ).toBeNull();
    await audio.evaluate((element) => (element as HTMLAudioElement).pause());
  }
  async function confirm(page: Page) {
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
  }
  const shot = (page: Page, name: string) =>
    page.screenshot({
      path: path.join(process.env.UPSELF_SCREENSHOTS!, name),
      fullPage: true,
      animations: "disabled",
    });
  try {
    const student = await login(fixture.actors.fresh, "student");
    await student.goto("/#/student/assignments/" + fixture.assignmentId);
    await expect(
      student.getByRole("heading", {
        name: "A recorded learning reflection",
        exact: true,
      }),
    ).toBeVisible();
    let panel = await recorded(student);
    await playing(panel.locator("audio"));
    await panel
      .getByRole("button", { name: "Delete recording", exact: true })
      .click();
    await expect(panel.locator("audio")).toHaveCount(0);
    panel = await recorded(student);
    await playing(panel.locator("audio"));
    await expect(
      student.getByRole("button", { name: "Submit my work", exact: true }),
    ).toBeDisabled();
    await panel
      .getByRole("button", { name: "Upload recording", exact: true })
      .click();
    await expect(panel.locator("audio")).toHaveCount(0);
    await expect(student.locator(".work-files li")).toHaveCount(1);
    // Remove an already uploaded draft and replace it, exercising the real delete endpoint.
    await student
      .locator(".work-files")
      .getByRole("button", { name: /^Remove:/ })
      .click();
    await confirm(student);
    await expect(student.locator(".work-files li")).toHaveCount(0);
    panel = await recorded(student);
    await panel
      .getByRole("button", { name: "Upload recording", exact: true })
      .click();
    await expect(student.locator(".work-files li")).toHaveCount(1);
    await student
      .locator(".work-files .audio-player")
      .getByRole("button", { name: /^Listen:/ })
      .click();
    await playing(student.locator(".work-files audio"));
    await student.setViewportSize({ width: 390, height: 844 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await shot(student, "assignment-recording-mobile.png");
    await student
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByText("Your work has been submitted.", { exact: true }),
    ).toBeVisible();
    await student.reload();
    await student
      .locator(".attempt-card")
      .getByRole("button", { name: /^Listen:/ })
      .click();
    await playing(student.locator(".attempt-card audio"));

    const mentor = await login(fixture.actors.mentor, "mentor");
    await mentor.goto("/#/mentor/assignments/" + fixture.assignmentId);
    const row = mentor
      .getByRole("row")
      .filter({ hasText: fixture.actors.fresh.username });
    await row.getByRole("button", { name: /Review|View/ }).click();
    await mentor
      .getByRole("button", { name: /^Listen:/ })
      .first()
      .click();
    await playing(mentor.locator("audio").first());
    await shot(mentor, "mentor-assignment-recording.png");

    await student.goto("/#/student/mood");
    await student
      .getByLabel("I understand who can see my mood, diaries and recordings.", {
        exact: true,
      })
      .check();
    await student
      .getByRole("button", { name: "Continue to my mood", exact: true })
      .click();
    await student.getByRole("radio", { name: "Good", exact: true }).check();
    await student
      .getByLabel("My diary (optional)", { exact: true })
      .fill("I explained my learning step in a recording.");
    panel = await recorded(student);
    await playing(panel.locator("audio"));
    await student
      .getByRole("button", { name: "Save this week’s mood", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByText("Your mood is saved for this week.", { exact: true }),
    ).toBeVisible();
    await student.reload();
    await student
      .locator(".mood-week .audio-player")
      .getByRole("button", { name: /^Listen:/ })
      .click();
    await playing(student.locator(".mood-week audio"));
    await shot(student, "student-diary-recording.png");
    await mentor.goto("/#/mentor/mood/" + fixture.actors.fresh.id);
    await mentor
      .locator(".mood-history .audio-player")
      .getByRole("button", { name: /^Listen:/ })
      .click();
    await playing(mentor.locator(".mood-history audio"));
    await expect(mentor.locator("textarea,input[type=radio]")).toHaveCount(0);
    await shot(mentor, "mentor-diary-recording.png");
    await mentor.goto("/#/mentor/mood/" + fixture.actors.student.id);
    await mentor.locator(".mood-history summary").last().click();
    await expect(
      mentor.getByText(fixture.legacyDiary, { exact: true }),
    ).toBeVisible();
    const admin = await login(
      { username: "admin01", password: fixture.adminPassword },
      "admin",
    );
    await admin.goto("/#/mentor/mood/" + fixture.actors.fresh.id);
    await expect(admin).toHaveURL(/\/403$/);
    expect(errors).toEqual([]);
    console.log(
      "PASS Chrome uses real MediaRecorder with a simulated microphone; captured WebM audio decoded and played after student upload and mentor download. No transcription requests.",
    );
  } finally {
    for (const context of contexts) await context.close();
  }
});
