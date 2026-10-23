import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
test("three roles: publish, upload, return, resubmit, grade and read feedback", async ({
  browser,
  request,
}) => {
  test.setTimeout(240000);
  const stamp = Date.now().toString(36),
    title = "Assignment UI check " + stamp;
  const work = process.env.UPSELF_TEST_WORK!,
    screens = process.env.UPSELF_SCREENSHOTS!;
  const contexts = [];
  const errors: string[] = [];
  const save = (cid: number) =>
    fs.writeFileSync(
      path.join(work, "e2e-assignments.json"),
      JSON.stringify({ courses: [cid] }),
    );
  async function login(user: string, role: string) {
    const c = await browser.newContext();
    contexts.push(c);
    const page = await c.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(user);
    await page
      .getByLabel("Password", { exact: true })
      .fill(process.env["UPSELF_" + user.toUpperCase() + "_PASSWORD"]!);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(
      new RegExp("/" + (role === "admin" ? "admin" : role) + "/home"),
    );
    return page;
  }
  async function confirm(page: Page) {
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
  }
  async function download(page: Page) {
    const event = page.waitForEvent("download");
    await page
      .getByRole("button", { name: "Download: reflection.txt", exact: true })
      .first()
      .click();
    const file = await event;
    expect(file.suggestedFilename()).toBe("reflection.txt");
    const stream = await file.createReadStream();
    const data = [];
    for await (const c of stream!) data.push(c);
    expect(Buffer.concat(data).toString()).toBe("My short reflection.");
  }
  try {
    const admin = await login("admin01", "admin");
    await expect(
      admin.getByRole("heading", { name: "Administration", exact: true }),
    ).toBeVisible();
    await expect(
      admin.getByRole("navigation").getByRole("link", { name: "Assignments" }),
    ).toHaveCount(0);
    await admin.screenshot({
      path: path.join(screens, "roles-admin-home.png"),
      fullPage: true,
    });
    await admin
      .getByRole("navigation")
      .getByRole("link", { name: "Course allocation" })
      .click();
    await admin
      .getByRole("button", { name: "Create course", exact: true })
      .click();
    let dialog = admin.getByRole("dialog", { name: "Create course" });
    await dialog.getByLabel("Course title").fill(title);
    await dialog
      .getByLabel("Description")
      .fill("A small assignment practice course.");
    const created = admin.waitForResponse(
      (r) =>
        r.url().endsWith("/api/learning/courses") &&
        r.request().method() === "POST",
    );
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const cid = (await (await created).json()).data;
    save(cid);
    for (const [button, user] of [
      ["Assign mentor", "mentor01"],
      ["Add student", "student01"],
    ]) {
      await admin.getByRole("button", { name: button, exact: true }).click();
      dialog = admin.getByRole("dialog", { name: button, exact: true });
      await dialog.getByPlaceholder("Search by name or username").fill(user!);
      await dialog.getByRole("button", { name: "Search", exact: true }).click();
      await dialog
        .getByRole("button", {
          name: "Choose an account: " + user,
          exact: true,
        })
        .click();
      await expect(dialog).not.toBeVisible();
    }
    await admin.getByRole("button", { name: "Publish", exact: true }).click();
    const mentor = await login("mentor01", "mentor");
    await expect(
      mentor.getByRole("heading", { name: "Teaching workspace", exact: true }),
    ).toBeVisible();
    await expect(
      mentor
        .getByRole("navigation")
        .getByRole("link", { name: "Accounts", exact: true }),
    ).toHaveCount(0);
    await mentor.screenshot({
      path: path.join(screens, "roles-mentor-home.png"),
      fullPage: true,
    });
    await mentor
      .getByRole("navigation")
      .getByRole("link", { name: "Assignments", exact: true })
      .click();
    await mentor
      .getByRole("button", { name: "New assignment", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", {
      name: "New assignment",
      exact: true,
    });
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await mentor
      .getByRole("button", { name: "New assignment", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", {
      name: "New assignment",
      exact: true,
    });
    await dialog.getByLabel("Choose a course", { exact: true }).fill(title);
    await dialog.getByRole("button", { name: "Search", exact: true }).click();
    await dialog
      .getByRole("button", { name: "Choose course: " + title, exact: true })
      .click();
    await dialog.getByLabel("Assignment title").fill("Share one learning idea");
    await dialog
      .getByLabel("What to do")
      .fill("Write one idea and attach a short file. Take your time.");
    await dialog
      .getByLabel("Due date (Malaysia, UTC+08:00)")
      .fill("2020-01-01T12:00");
    await dialog.getByLabel("Maximum score").fill("10");
    await dialog.getByLabel("Status", { exact: true }).selectOption("DRAFT");
    await dialog
      .getByRole("button", { name: "Save assignment", exact: true })
      .click();
    await expect(mentor).toHaveURL(/mentor\/assignments\/\d+/);
    const aid = Number(mentor.url().split("/").pop());
    const student = await login("student01", "student");
    await expect(
      student.getByRole("heading", { name: "Welcome back", exact: true }),
    ).toBeVisible();
    await expect(
      student
        .getByRole("navigation")
        .getByRole("link", { name: "Accounts", exact: true }),
    ).toHaveCount(0);
    await student.screenshot({
      path: path.join(screens, "roles-student-home.png"),
      fullPage: true,
    });
    await student.goto("/#/student/assignments?courseId=" + cid);
    await expect(
      student.getByText("No assignments to show yet."),
    ).toBeVisible();
    await mentor
      .getByRole("button", { name: "Edit assignment", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", { name: "Edit assignment" });
    await dialog
      .getByLabel("Status", { exact: true })
      .selectOption("PUBLISHED");
    await dialog
      .getByRole("button", { name: "Save assignment", exact: true })
      .click();
    await expect(dialog).not.toBeVisible();
    await student.reload();
    await student
      .getByRole("link", { name: "Open assignment", exact: false })
      .click();
    await expect(
      student.getByRole("heading", {
        name: "Share one learning idea",
        exact: true,
      }),
    ).toBeVisible();
    await student
      .getByLabel("Your answer", { exact: true })
      .fill("I learned that taking a break can help. 🌱");
    await student
      .getByRole("link", { name: "Back to assignments", exact: false })
      .click();
    await student
      .getByRole("dialog")
      .getByRole("button", { name: "Cancel", exact: true })
      .click();
    await expect(
      student.getByLabel("Your answer", { exact: true }),
    ).toHaveValue("I learned that taking a break can help. 🌱");
    await student
      .getByRole("button", { name: "Save draft", exact: true })
      .click();
    await expect(
      student.getByText("Draft saved.", { exact: true }),
    ).toBeVisible();
    await student.reload();
    await expect(
      student.getByLabel("Your answer", { exact: true }),
    ).toHaveValue("I learned that taking a break can help. 🌱");
    const attach = () =>
      student.getByLabel("Choose a file", { exact: true }).setInputFiles({
        name: "reflection.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("My short reflection."),
      });

    await student
      .getByLabel("Your answer", { exact: true })
      .fill("Autosaved: I can pause and take a break. 🌱");
    const auto = await student.waitForResponse(
      (r) => r.url().endsWith("/submission") && r.request().method() === "PUT",
      { timeout: 35000 },
    );
    expect((await auto.json()).code).toBe(200);
    await student.reload();
    await expect(
      student.getByLabel("Your answer", { exact: true }),
    ).toHaveValue("Autosaved: I can pause and take a break. 🌱");
    await student
      .getByLabel("Choose a file", { exact: true })
      .setInputFiles({
        name: "bad.exe",
        mimeType: "application/octet-stream",
        buffer: Buffer.from("bad"),
      });
    await student
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(student.getByRole("alert")).toContainText(
      "Choose a supported file up to 20 MB.",
    );
    let release: () => void = () => {};
    await student.route("**/api/client/assignments/*/files", async (route) => {
      await new Promise<void>((resolve) => {
        release = resolve;
      });
      await route.abort().catch(() => {});
    });
    await attach();
    await student
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await expect(student.getByRole("alert")).toContainText(
      "Upload the selected file before submitting.",
    );
    await student
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await student
      .getByRole("button", { name: "Cancel upload", exact: true })
      .click();
    release();
    await student.unroute("**/api/client/assignments/*/files");
    await expect(student.getByRole("alert")).toContainText(
      "Upload cancelled. Reload to check whether the server received it.",
    );
    await student
      .getByRole("button", { name: "Try again", exact: true })
      .click();
    await expect(
      student.getByRole("button", { name: "Upload file", exact: true }),
    ).toBeDisabled();
    await attach();
    await student
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(
      student.getByRole("button", {
        name: "Download: reflection.txt",
        exact: true,
      }),
    ).toBeVisible();
    await download(student);
    await student
      .getByRole("button", { name: "Remove: reflection.txt", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByRole("button", {
        name: "Download: reflection.txt",
        exact: true,
      }),
    ).toHaveCount(0);
    await attach();
    await student
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(
      student.getByRole("button", {
        name: "Download: reflection.txt",
        exact: true,
      }),
    ).toBeVisible();
    await mentor.reload();
    await expect(
      mentor.getByText("No submitted work yet. Student drafts stay private."),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByText("Your work has been submitted.", { exact: true }),
    ).toBeVisible();
    await expect(
      student.getByLabel("Your answer", { exact: true }),
    ).toHaveCount(0);
    await expect(
      student.getByText("Late submission", { exact: false }),
    ).toBeVisible();
    await mentor.reload();
    await mentor
      .getByRole("button", { name: "Review work: student01", exact: true })
      .click();
    await download(mentor);
    await mentor.getByLabel("Review decision").selectOption("RETURNED");
    await mentor
      .getByLabel("Feedback for the student")
      .fill("Please add one example.");
    await mentor
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(
      mentor.getByText("Feedback sent to the student.", { exact: true }),
    ).toBeVisible();
    await student
      .getByRole("button", { name: "Check for feedback", exact: true })
      .click();
    await expect(
      student.getByText("Please add one example.", { exact: true }),
    ).toBeVisible();
    await student
      .getByLabel("Your answer", { exact: true })
      .fill("For example, I can pause a video and return later.");
    await student
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await confirm(student);
    await expect(
      student.getByRole("heading", { name: "Attempt 2", exact: true }),
    ).toBeVisible();
    await mentor.reload();
    await mentor
      .getByRole("button", { name: "Review work: student01", exact: true })
      .click();
    await mentor.getByLabel("Score / 10").fill("9");
    await mentor
      .getByLabel("Feedback for the student")
      .fill("Clear example. Well done.");
    await mentor
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(
      mentor.getByText("Feedback sent to the student.", { exact: true }),
    ).toBeVisible();
    await mentor.screenshot({
      path: path.join(screens, "assignment-mentor-feedback.png"),
      fullPage: true,
    });
    await student
      .getByRole("button", { name: "Check for feedback", exact: true })
      .click();
    await expect(
      student.getByText("Clear example. Well done.", { exact: true }),
    ).toBeVisible();
    await expect(student.getByText("9 / 10", { exact: true })).toBeVisible();
    await expect(
      student.getByRole("heading", { name: "Attempt 1", exact: true }),
    ).toBeVisible();
    await download(student);
    await student.reload();
    await expect(student.getByText("9 / 10", { exact: true })).toBeVisible();
    await student.screenshot({
      path: path.join(screens, "assignment-student-feedback.png"),
      fullPage: true,
    });
    await student.setViewportSize({ width: 390, height: 844 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(screens, "assignment-student-mobile.png"),
      fullPage: true,
    });
    await student.goto("/#/mentor/assignments/" + aid);
    await expect(
      student.getByRole("heading", {
        name: /not have access|permission|unavailable/i,
      }),
    ).toBeVisible();

    const loginResponse = await request.post("http://127.0.0.1:8080/empLogin", {
      data: {
        username: "mentor01",
        password: process.env.UPSELF_MENTOR01_PASSWORD,
      },
    });
    const token = (await loginResponse.json()).token;
    for (let n = 0; n < 11; n++) {
      const r = await request.post("http://127.0.0.1:8080/assignments", {
        headers: { Authorization: "Bearer " + token },
        data: {
          courseId: cid,
          title: "Pager " + stamp + " " + n,
          instructions: "Pagination fixture",
          status: "DRAFT",
        },
      });
      expect((await r.json()).code).toBe(200);
    }
    await mentor.goto("/#/mentor/assignments?courseId=" + cid);
    await mentor
      .getByPlaceholder("Search", { exact: true })
      .fill("Pager " + stamp);
    await mentor.getByRole("button", { name: "Search", exact: true }).click();
    await expect(mentor.getByText("Page 1 · 11 items")).toBeVisible();
    await mentor.getByRole("button", { name: "Next", exact: true }).click();
    await expect(mentor.getByText("Page 2 · 11 items")).toBeVisible();
    await mentor.getByRole("button", { name: "Previous", exact: true }).click();
    await expect(mentor.getByText("Page 1 · 11 items")).toBeVisible();
    await mentor.getByRole("button", { name: "Clear", exact: true }).click();
    await expect(mentor.getByText("Page 1 · 12 items")).toBeVisible();
    await mentor.getByLabel("Only assignments awaiting review").check();
    await expect(mentor.getByText("No assignments to show yet.")).toBeVisible();
    await mentor.getByLabel("Only assignments awaiting review").uncheck();
    await expect(mentor.getByText("Page 1 · 12 items")).toBeVisible();
    await mentor
      .getByRole("link", { name: "All my courses", exact: true })
      .click();
    await expect(mentor).toHaveURL(/mentor\/assignments$/);
    expect(errors).toEqual([]);
    console.log(
      "PASS distinct home/menu, real course allocation, assignment draft/publish, Unicode draft, real upload/download/remove, late submission, return/resubmit, grade/history, reload persistence, mobile layout and role guard",
    );
  } finally {
    for (const c of contexts) await c.close();
  }
});
