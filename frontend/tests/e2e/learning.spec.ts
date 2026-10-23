import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("real administrator → mentor → student learning workflow", async ({
  browser,
  request,
}) => {
  test.setTimeout(240000);
  const suffix = Date.now().toString(36);
  const title = "Learning together · " + suffix;
  const work = process.env.UPSELF_TEST_WORK!;
  const media = process.env.UPSELF_MEDIA_DIR!;
  const screenshots = process.env.UPSELF_SCREENSHOTS!;
  const created: { courses: number[]; users: number[]; demo?: number } = {
    courses: [],
    users: [],
  };
  const checks: string[] = [];
  const contexts: BrowserContext[] = [];
  const pageErrors: string[] = [];
  let adminToken = "",
    mentorToken = "",
    studentToken = "";
  function record(name: string) {
    checks.push(name);
    console.log("PASS", name);
  }
  function persist() {
    fs.writeFileSync(
      path.join(work, "e2e-generated.json"),
      JSON.stringify(created),
    );
  }
  async function api(
    url: string,
    token: string,
    method = "GET",
    data?: unknown,
  ) {
    const response = await request.fetch("http://127.0.0.1:8080" + url, {
      method,
      data,
      headers: { Authorization: "Bearer " + token },
    });
    return response.json();
  }
  async function login(page: Page, user: string, management: boolean) {
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(
        user === "mentor01" ? "mentor" : management ? "admin" : "student",
      );
    await page.getByLabel("Username", { exact: true }).fill(user);
    await page
      .getByLabel("Password", { exact: true })
      .fill(process.env["UPSELF_" + user.toUpperCase() + "_PASSWORD"]!);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(
      user === "mentor01"
        ? /mentor\/home/
        : management
          ? /admin\/home/
          : /student\/home/,
    );
    await expect(page.getByRole("button", { name: "Sign out" })).toBeVisible();
    await page
      .getByRole("navigation")
      .getByRole("link", {
        name: management
          ? user === "mentor01"
            ? "Teaching courses"
            : "Course allocation"
          : /My learning/,
      })
      .click();
    record(user + " browser login");
  }
  async function context() {
    const c = await browser.newContext();
    contexts.push(c);
    const p = await c.newPage();
    p.on("pageerror", (e) => pageErrors.push(e.message));
    return p;
  }
  try {
    for (const [name, target] of [
      ["admin01", "admin"],
      ["mentor01", "mentor"],
      ["student01", "student"],
    ]) {
      const r = await request.post(
        "http://127.0.0.1:8080" +
          (target === "student" ? "/client/login" : "/empLogin"),
        {
          data: {
            username: name,
            password:
              process.env["UPSELF_" + name!.toUpperCase() + "_PASSWORD"],
          },
        },
      );
      const j = await r.json();
      expect(j.code).toBe(200);
      if (target === "admin") adminToken = j.token;
      else if (target === "mentor") mentorToken = j.token;
      else studentToken = j.token;
    }
    const info = await api("/getInfo", mentorToken),
      sinfo = await api("/client/getInfo", studentToken);
    const mid = info.user.id,
      sid = sinfo.user.id;
    const admin = await context();
    await login(admin, "admin01", true);
    await admin
      .getByRole("button", { name: "Create course", exact: true })
      .click();
    let dialog = admin.getByRole("dialog", { name: "Create course" });
    await dialog.getByLabel("Course title").fill(title);
    await dialog
      .getByLabel("Description")
      .fill("A short demo course. Take one small step, at your own pace.");
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await admin
      .getByRole("button", { name: "Create course", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Create course" });
    await dialog.getByLabel("Course title").fill(title);
    await dialog
      .getByLabel("Description")
      .fill("A short demo course. Take one small step, at your own pace.");
    const creation = admin.waitForResponse(
      (r) =>
        r.url().endsWith("/api/learning/courses") &&
        r.request().method() === "POST",
    );
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const cid = (await (await creation).json()).data;
    created.courses.push(cid);
    persist();
    await expect(
      admin.getByRole("heading", { name: title, exact: true }),
    ).toBeVisible();
    await expect(
      admin.getByRole("button", { name: "Publish", exact: true }),
    ).toBeDisabled();
    record("course create/cancel and missing-mentor publish guard");
    await admin
      .getByRole("button", { name: "Edit", exact: true })
      .first()
      .click();
    dialog = admin.getByRole("dialog", { name: "Edit", exact: true });
    await dialog
      .getByLabel("Description")
      .fill("Welcome! Watch, pause and explore at your own pace.");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).not.toBeVisible();
    await admin
      .getByRole("button", { name: "Assign mentor", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Assign mentor", exact: true });
    await dialog
      .getByPlaceholder("Search by name or username")
      .fill("mentor01");
    await dialog.getByRole("button", { name: "Search", exact: true }).click();
    await dialog
      .getByRole("button", { name: "Choose an account: mentor01", exact: true })
      .click();
    await expect(dialog).not.toBeVisible();
    await admin
      .getByRole("button", { name: "Add student", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Add student", exact: true });
    await dialog
      .getByPlaceholder("Search by name or username")
      .fill("student01");
    await dialog.getByRole("button", { name: "Search", exact: true }).click();
    await dialog
      .getByRole("button", {
        name: "Choose an account: student01",
        exact: true,
      })
      .click();
    await expect(dialog).not.toBeVisible();
    await admin.getByRole("button", { name: "Publish", exact: true }).click();
    await expect(admin.locator(".page-heading .status")).toHaveText(
      "Published",
    );
    await admin.reload();
    await expect(
      admin.getByRole("heading", { name: title, exact: true }),
    ).toBeVisible();
    expect(
      (await api("/learning/courses/" + cid, adminToken)).data.mentorId,
    ).toBe(mid);
    expect(
      (
        await api("/learning/courses/" + cid + "/students", adminToken)
      ).data.some((x: { studentId: number }) => x.studentId === sid),
    ).toBe(true);
    record(
      "course edit, assign, enroll, publish, browser reload and real persistence",
    );
    await admin.screenshot({
      path: path.join(screenshots, "up-self-admin.png"),
      fullPage: true,
    });

    const mentor = await context();
    await login(mentor, "mentor01", true);
    await expect(
      mentor.getByRole("button", { name: "Create course", exact: true }),
    ).toHaveCount(0);
    await mentor.getByPlaceholder("Search", { exact: true }).fill(title);
    await mentor.getByRole("button", { name: "Search", exact: true }).click();
    await mentor
      .getByRole("link", { name: "Open course", exact: true })
      .click();
    await expect(
      mentor.getByRole("button", { name: "Assign mentor", exact: true }),
    ).toHaveCount(0);
    await mentor
      .getByLabel("Chapter title", { exact: true })
      .fill("A calm start");
    await mentor
      .getByRole("button", { name: "Add chapter", exact: true })
      .click();
    await expect(mentor.getByLabel("Text", { exact: true })).toBeVisible();
    const chapterId = Number(new URL(mentor.url()).hash.split("/").pop());
    await mentor
      .getByLabel("Text", { exact: true })
      .fill(
        "Take a breath. Read the short introduction, then watch the video. You can pause whenever you need.",
      );
    await mentor.getByLabel("Order", { exact: true }).fill("1");
    await mentor
      .getByLabel("Status", { exact: true })
      .first()
      .selectOption("PUBLISHED");
    await mentor.getByRole("button", { name: "Save", exact: true }).click();
    await expect(mentor.getByRole("status")).toContainText("Chapter saved.");
    record("mentor owns course and saves published chapter");
    async function upload(
      kind: string,
      filename: string,
      resourceTitle: string,
    ) {
      await mentor.getByLabel("File type", { exact: true }).selectOption(kind);
      await mentor
        .getByLabel("File", { exact: true })
        .setInputFiles(path.join(media, filename));
      await mentor
        .getByLabel("Resource title", { exact: true })
        .fill(resourceTitle);
      if (kind === "IMAGE")
        await mentor
          .getByLabel("Image description", { exact: true })
          .fill(
            "A gentle reminder to pause, breathe and learn at your own pace.",
          );
      if (kind === "VIDEO") {
        const caption = (
          await api(
            "/learning/chapters/" + chapterId + "/resources",
            mentorToken,
          )
        ).data.find((r: { kind: string }) => r.kind === "CAPTION");
        await mentor
          .getByLabel("Caption file", { exact: true })
          .selectOption(String(caption.id));
        await mentor
          .getByLabel("Video transcript", { exact: true })
          .fill("Take one small step. Learn at your own pace.");
      }
      await mentor
        .getByLabel("Status", { exact: true })
        .last()
        .selectOption("PUBLISHED");
      await mentor
        .getByRole("button", { name: "Upload file", exact: true })
        .click();
      await expect(
        mentor.getByRole("listitem").filter({ hasText: resourceTitle }),
      ).toBeVisible();
    }
    await upload("IMAGE", "lesson.png", "A gentle reminder");
    await upload("CAPTION", "lesson.vtt", "English captions");
    await upload("VIDEO", "lesson.mp4", "Your first small step");
    record(
      "real image, WebVTT and playable H.264/AAC video uploaded through browser",
    );
    const imageRow = mentor
      .getByRole("listitem")
      .filter({ hasText: "A gentle reminder" });
    await imageRow
      .getByRole("button", { name: "Preview", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", { name: "Preview", exact: true });
    await expect(
      dialog.getByRole("img", {
        name: "A gentle reminder to pause, breathe and learn at your own pace.",
        exact: true,
      }),
    ).toBeVisible();
    await dialog
      .getByRole("button", { name: "Close this dialog", exact: true })
      .click();
    await imageRow.getByRole("button", { name: "Edit", exact: true }).click();
    dialog = mentor.getByRole("dialog", { name: "Edit file details" });
    await dialog
      .getByLabel("Image description")
      .fill("Learning reminder: pause, breathe, and take one small step.");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).not.toBeVisible();
    await mentor.getByRole("link", { name: "Back to courses" }).click();
    await mentor
      .getByRole("button", { name: "Preview", exact: true })
      .first()
      .click();
    await expect(
      mentor
        .getByRole("dialog", { name: "Preview" })
        .getByRole("heading", { name: "A calm start" }),
    ).toBeVisible();
    await mentor
      .getByRole("dialog", { name: "Preview" })
      .getByRole("button", { name: "Close this dialog" })
      .click();
    await mentor
      .getByRole("button", { name: "Add update", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", { name: "Add update" });
    await dialog
      .getByLabel("Update title")
      .fill("Welcome to our learning space");
    await dialog
      .getByLabel("Text", { exact: true })
      .fill("Take your time. You can ask your mentor for help.");
    await dialog
      .getByLabel("Status", { exact: true })
      .selectOption("PUBLISHED");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).not.toBeVisible();
    await mentor
      .getByRole("button", { name: "Add update", exact: true })
      .click();
    dialog = mentor.getByRole("dialog", { name: "Add update" });
    await dialog.getByLabel("Update title").fill("Our next check-in");
    await dialog
      .getByLabel("Text", { exact: true })
      .fill("Join when you are ready.");
    await dialog.getByLabel("Update type").selectOption("MEETING");
    await dialog
      .getByLabel("Meeting link (HTTPS)")
      .fill("https://example.org/up-self-meeting");
    await dialog.getByLabel("Meeting time (Malaysia)").fill("2026-11-01T10:00");
    await dialog
      .getByLabel("Status", { exact: true })
      .selectOption("PUBLISHED");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).not.toBeVisible();
    record("mentor announcement and meeting post published with real data");
    await mentor.screenshot({
      path: path.join(screenshots, "up-self-mentor.png"),
      fullPage: true,
    });

    const student = await context();
    await login(student, "student01", false);
    await student.getByLabel("Search", { exact: true }).fill(title);
    await student.getByRole("button", { name: "Search", exact: true }).click();
    await student
      .getByRole("link", { name: "Open course", exact: true })
      .click();
    await expect(
      student.getByRole("heading", { name: "A calm start", exact: true }),
    ).toBeVisible();
    await expect(
      student.getByRole("img", {
        name: "Learning reminder: pause, breathe, and take one small step.",
      }),
    ).toBeVisible();
    const video = student.locator("video");
    await expect
      .poll(() => video.evaluate((v: HTMLVideoElement) => v.readyState))
      .toBeGreaterThanOrEqual(2);
    await video.focus();
    await video.press("Space");
    await expect
      .poll(() => video.evaluate((v: HTMLVideoElement) => v.currentTime))
      .toBeGreaterThan(0);
    await video.press("Space");
    await expect
      .poll(() => video.evaluate((v: HTMLVideoElement) => v.paused))
      .toBe(true);
    const videoBounds = await video.boundingBox();
    expect(videoBounds).not.toBeNull();
    // Use the actual native progress bar, rather than the focused play button's arrow keys.
    await video.click({
      position: { x: videoBounds!.width * 0.65, y: videoBounds!.height - 22 },
    });
    await expect
      .poll(() => video.evaluate((v: HTMLVideoElement) => v.currentTime))
      .toBeGreaterThan(3);
    await expect
      .poll(() =>
        video.evaluate(
          (v: HTMLVideoElement) => v.textTracks[0]?.cues?.length || 0,
        ),
      )
      .toBe(2);
    await student.getByText("Read transcript", { exact: true }).click();
    await expect(
      student.getByText("Take one small step. Learn at your own pace.", {
        exact: true,
      }),
    ).toBeVisible();
    record(
      "student real video play/pause/seek, image alt, loaded captions and transcript",
    );
    await student
      .getByRole("button", { name: "Read aloud", exact: true })
      .click();
    const speechAvailable = await student
      .getByRole("button", { name: "Pause", exact: true })
      .count();
    if (speechAvailable) {
      // Native voices can finish or fail between rendering a control and clicking it.
      // Deterministic pause/resume behavior is also covered in composable unit tests.
      try {
        await student
          .getByRole("button", { name: "Pause", exact: true })
          .click({ timeout: 1500 });
        await student
          .getByRole("button", { name: "Resume", exact: true })
          .click({ timeout: 1500 });
        await student
          .getByRole("button", { name: "Stop", exact: true })
          .click({ timeout: 1500 });
        record("TTS controls operate");
      } catch {
        await expect(
          student.getByRole("button", { name: "Read aloud", exact: true }),
        ).toBeVisible();
        record("native TTS ended or failed safely; reading remains usable");
      }
    } else {
      await expect(
        student.getByText("Read aloud is not available", { exact: false }),
      ).toBeVisible();
      record("TTS unavailable fallback is explicit");
    }
    await expect(
      student.getByRole("button", { name: "Previous", exact: false }),
    ).toBeDisabled();
    expect(
      (
        await api(
          "/learning/courses/" + cid + "/chapters",
          mentorToken,
          "POST",
          {
            title: "Your next small step",
            body: "You can return to the first chapter whenever you like.",
            sortOrder: 20,
            status: "PUBLISHED",
            languageCode: "en",
          },
        )
      ).code,
    ).toBe(200);
    await student.reload();
    await student.getByRole("button", { name: "Next →", exact: true }).click();
    await expect(
      student.getByRole("heading", {
        name: "Your next small step",
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      student.getByRole("button", { name: "Next →", exact: true }),
    ).toBeDisabled();
    await student
      .getByRole("button", { name: "Previous", exact: false })
      .click();
    await expect(
      student.getByRole("heading", { name: "A calm start", exact: true }),
    ).toBeVisible();
    record("student previous/next navigation switches real published chapters");
    await student
      .getByRole("button", { name: "Updates", exact: false })
      .click();
    await expect(
      student.getByRole("heading", { name: "Welcome to our learning space" }),
    ).toBeVisible();
    const meetingLink = student.getByRole("link", {
      name: "Open meeting link",
      exact: false,
    });
    await expect(meetingLink).toHaveAttribute(
      "href",
      "https://example.org/up-self-meeting",
    );
    // The platform opens a mentor-supplied URL; it does not host the meeting provider.
    // Make the sample destination deterministic while testing the actual new-tab action.
    await student
      .context()
      .route("https://example.org/up-self-meeting", (route) =>
        route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<h1>Sample meeting destination</h1>",
        }),
      );
    const popupPromise = student.waitForEvent("popup");
    await meetingLink.click();
    const popup = await popupPromise;
    await expect(
      popup.getByRole("heading", { name: "Sample meeting destination" }),
    ).toBeVisible();
    await popup.close();
    record("student update tab and meeting link button operate");
    await student
      .getByRole("button", { name: "Chapters", exact: false })
      .click();
    await student.screenshot({
      path: path.join(screenshots, "up-self-student.png"),
      fullPage: true,
    });
    await student.setViewportSize({ width: 390, height: 844 });
    await expect(
      student.getByRole("heading", { name: "A calm start", exact: true }),
    ).toBeVisible();
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(screenshots, "up-self-student-mobile.png"),
      fullPage: true,
    });
    await student.setViewportSize({ width: 1365, height: 900 });
    await student
      .getByRole("link", { name: "Reading settings", exact: false })
      .click();
    await student.getByLabel("Text size").selectOption("extraLarge");
    await student
      .getByLabel("Contrast", { exact: true })
      .selectOption("highContrast");
    await student.getByLabel("Reduce motion").check();
    await student.reload();
    await expect(student.getByLabel("Text size")).toHaveValue("extraLarge");
    await expect(student.locator("html")).toHaveAttribute(
      "data-contrast",
      "highContrast",
    );
    await student
      .getByRole("button", { name: "Reset settings", exact: true })
      .click();
    await expect(student.getByLabel("Text size")).toHaveValue("standard");
    await student.goto("/#/manage/courses");
    await expect(student).toHaveURL(/\/403/);
    await expect(
      student.getByRole("heading", {
        name: "You do not have access to this page.",
      }),
    ).toBeVisible();
    const denied = await api("/client/learning/courses", studentToken, "POST", {
      title: "Should not exist",
      description: "",
    });
    expect(denied.code).toBe(403);
    record(
      "mobile layout, saved preferences, reset and student page/API authorization",
    );

    // Exercise non-destructive account actions on the actual account-management page.
    await admin.getByRole("link", { name: "Accounts", exact: true }).click();
    await admin
      .getByRole("button", { name: "Create account", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Create account" });
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await admin.getByPlaceholder("Username", { exact: true }).fill("student01");
    await admin.getByRole("button", { name: "Search", exact: true }).click();
    await expect(
      admin.getByRole("row").filter({ has: admin.getByRole("cell", { name: "student01", exact: true }) }),
    ).toHaveCount(1);
    await admin
      .getByRole("row")
      .filter({ has: admin.getByRole("cell", { name: "student01", exact: true }) })
      .getByRole("button", { name: "Edit", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Edit", exact: true });
    await expect(dialog.getByLabel("Display name")).toBeVisible();
    await dialog.getByRole("button", { name: "Close", exact: true }).click();
    await admin.getByRole("button", { name: "Clear", exact: true }).click();
    record(
      "account page search/clear/create-cancel/edit-close work without altering existing accounts",
    );

    await admin.goto("/#/manage/courses/" + cid);
    await admin.getByRole("button", { name: "Remove", exact: true }).click();
    await admin.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(admin.getByText("student01", { exact: true })).toBeVisible();
    await admin.getByRole("button", { name: "Remove", exact: true }).click();
    await admin.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect
      .poll(
        async () =>
          (await api("/client/learning/courses/" + cid, studentToken)).code,
      )
      .toBe(403);
    await admin
      .getByRole("button", { name: "Add student", exact: true })
      .click();
    dialog = admin.getByRole("dialog", { name: "Add student", exact: true });
    await dialog
      .getByPlaceholder("Search by name or username")
      .fill("student01");
    await dialog.getByRole("button", { name: "Search", exact: true }).click();
    await dialog
      .getByRole("button", {
        name: "Choose an account: student01",
        exact: true,
      })
      .click();
    await expect(dialog).not.toBeVisible();
    await admin
      .getByRole("button", { name: "Move to draft", exact: true })
      .click();
    await expect(admin.locator(".page-heading .status")).toHaveText("Draft");
    expect(
      (await api("/client/learning/courses/" + cid, studentToken)).code,
    ).toBe(403);
    await admin.getByRole("button", { name: "Publish", exact: true }).click();
    await expect(admin.locator(".page-heading .status")).toHaveText(
      "Published",
    );
    await admin.getByRole("button", { name: "Archive", exact: true }).click();
    await admin.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(
      admin.getByText("This course is archived. Content is read-only."),
    ).toBeVisible();
    await admin.getByRole("button", { name: "Publish", exact: true }).click();
    await expect(admin.locator(".page-heading .status")).toHaveText(
      "Published",
    );
    record(
      "remove cancel/confirm, reenroll, withdraw, republish and archive/restore change real access",
    );
    for (const p of [admin, mentor, student]) {
      await p.getByRole("button", { name: "Sign out", exact: true }).click();
      await expect(p).toHaveURL(/login/);
    }
    expect(pageErrors).toEqual([]);
    created.demo = cid;
    persist();
    fs.writeFileSync(
      path.join(work, "browser-check-results.json"),
      JSON.stringify(
        {
          checks,
          count: checks.length,
          demoCourseId: cid,
          browserErrors: pageErrors,
        },
        null,
        2,
      ),
    );
    record("all three sign-out buttons and no unhandled browser errors");
  } finally {
    persist();
    for (const c of contexts) await c.close();
    for (const [token, prefix] of [
      [adminToken, ""],
      [mentorToken, ""],
      [studentToken, "/client"],
    ])
      if (token) await api(prefix + "/logout", token, "POST").catch(() => {});
  }
});
