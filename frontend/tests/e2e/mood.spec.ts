import { test, expect } from "@playwright/test";
import type { Page, BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
test("weekly mood: first-use notice, once-only save, shared diaries, legacy private notes and current-mentor access", async ({
  browser,
  request,
}) => {
  test.setTimeout(240000);
  const suffix = Date.now().toString(36),
    prefix = "mood_ui_" + suffix + "_",
    title = "Mood UI check " + suffix,
    password = "Mood-Fixture-" + suffix + "!";
  const work = process.env.UPSELF_TEST_WORK!,
    screens = process.env.UPSELF_SCREENSHOTS!,
    fixture = { courses: [] as number[], users: [] as number[] };
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  const persist = () =>
    fs.writeFileSync(path.join(work, "e2e-mood.json"), JSON.stringify(fixture));
  const privateNote = "Shared diary: I need a quiet space. 🌱";
  const apiBase = process.env.UPSELF_TEST_API_URL || "http://127.0.0.1:8080";
  async function token(username: string, pwd: string, role: string) {
    const r = await request.post(
      apiBase + (role === "student" ? "/client/login" : "/empLogin"),
      { data: { username, password: pwd } },
    );
    const json = await r.json();
    expect(json.code).toBe(200);
    return json.token as string;
  }
  async function api(
    url: string,
    auth: string,
    method = "GET",
    data?: unknown,
    code = 200,
  ) {
    const r = await request.fetch(apiBase + url, {
      method,
      data,
      headers: { Authorization: "Bearer " + auth },
    });
    const json = await r.json();
    expect(json.code).toBe(code);
    return json;
  }
  async function login(username: string, pwd: string, role: string) {
    const context = await browser.newContext();
    contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("/#/login");
    await page
      .getByLabel("Choose your role", { exact: true })
      .selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(username);
    await page.getByLabel("Password", { exact: true }).fill(pwd);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/" + role + "/home"));
    return page;
  }
  async function dialog(page: Page, answer = "Confirm") {
    await page
      .getByRole("dialog")
      .getByRole("button", { name: answer, exact: true })
      .click();
  }
  function sql(statement: string) {
    const r = spawnSync(
      process.env.UPSELF_SQL_CLIENT!,
      [
        "--defaults-extra-file=" + process.env.UPSELF_SQL_CONFIG!,
        "--default-character-set=utf8mb4",
        "-N",
        "up_self_dev",
      ],
      { input: statement, encoding: "utf8", windowsHide: true },
    );
    expect(r.status, "Only this test fixture SQL may run").toBe(0);
    return r.stdout;
  }
  try {
    const adminToken = await token(
      "admin01",
      process.env.UPSELF_ADMIN01_PASSWORD!,
      "admin",
    );
    const actors: Record<string, { id: number; username: string }> = {};
    for (const [name, roleId] of [
      ["mentor", 20],
      ["othermentor", 20],
      ...Array.from({ length: 12 }, (_, i) => ["student" + i, 10]),
    ] as [string, number][]) {
      const username = prefix + name;
      const id = (
        await api("/system/user", adminToken, "POST", {
          userName: username,
          nickName: name === "student0" ? "Mood learner" : name,
          roleId,
          password,
        })
      ).data;
      actors[name] = { id, username };
      fixture.users.push(id);
      persist();
    }
    const mentor = actors.mentor!,
      otherMentor = actors.othermentor!,
      student = actors.student0!;
    const cid = (
      await api("/learning/courses", adminToken, "POST", {
        title,
        description: "Disposable weekly mood browser verification.",
      })
    ).data;
    fixture.courses.push(cid);
    persist();
    const cp = "/learning/courses/" + cid;
    await api(cp + "/mentor", adminToken, "PUT", { mentorId: mentor.id });
    for (let n = 0; n < 12; n++)
      await api(cp + "/students", adminToken, "POST", {
        studentId: actors["student" + n]!.id,
      });
    await api(cp + "/status", adminToken, "PUT", { status: "PUBLISHED" });
    const studentToken = await token(student.username, password, "student");
    const mentorToken = await token(mentor.username, password, "mentor");
    const admin = await login(
      "admin01",
      process.env.UPSELF_ADMIN01_PASSWORD!,
      "admin",
    );
    await expect(
      admin
        .getByRole("navigation")
        .getByRole("link", { name: /My check-in|Student check-ins/ }),
    ).toHaveCount(0);
    await admin.goto("/#/student/mood");
    await expect(
      admin.getByRole("heading", {
        name: /not have access|permission|unavailable/i,
      }),
    ).toBeVisible();
    await api(
      "/moods/students/" + student.id + "/history",
      adminToken,
      "GET",
      undefined,
      403,
    );
    const learner = await login(student.username, password, "student");
    await learner
      .getByRole("navigation")
      .getByRole("link", { name: "My check-in", exact: true })
      .click();
    await expect(
      learner.getByRole("heading", {
        name: "Before your first check-in",
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      learner.getByRole("button", { name: "Continue to my mood", exact: true }),
    ).toBeDisabled();
    await expect(
      learner.getByText(mentor.username, { exact: false }),
    ).toBeVisible();
    await learner.screenshot({
      path: path.join(screens, "mood-first-use.png"),
      fullPage: true,
    });
    await learner.getByRole("link", { name: "Not now", exact: true }).click();
    await expect(learner).toHaveURL(/student\/home/);
    await learner
      .getByRole("navigation")
      .getByRole("link", { name: "My check-in", exact: true })
      .click();
    await expect(
      learner.getByRole("heading", {
        name: "Before your first check-in",
        exact: true,
      }),
    ).toBeVisible();
    await api(
      "/client/moods/entries",
      studentToken,
      "POST",
      { weekStart: "2026-01-05", moodLevel: 3, privateNote: "not saved" },
      403,
    );
    await learner
      .getByLabel("I understand who can see my mood, diaries and recordings.", {
        exact: true,
      })
      .check();
    await learner
      .getByRole("button", { name: "Continue to my mood", exact: true })
      .click();
    await expect(
      learner.getByRole("group", {
        name: "How has your week felt?",
        exact: false,
      }),
    ).toBeVisible();
    await learner.reload();
    await expect(
      learner.getByRole("heading", {
        name: "Before your first check-in",
        exact: true,
      }),
    ).toHaveCount(0);
    await expect(
      learner.getByRole("radio", { name: "Good", exact: true }),
    ).not.toBeChecked();
    await learner.getByRole("radio", { name: "Good", exact: true }).check();
    await learner
      .getByLabel("My diary (optional)", { exact: true })
      .fill(privateNote);
    await learner
      .getByRole("navigation")
      .getByRole("link", { name: "Home", exact: true })
      .click();
    await dialog(learner, "Cancel");
    await expect(
      learner.getByLabel("My diary (optional)", { exact: true }),
    ).toHaveValue(privateNote);
    await learner
      .getByRole("button", { name: "Save this week’s mood", exact: true })
      .click();
    await dialog(learner, "Cancel");
    expect((await api("/client/moods/history", studentToken)).total).toBe(0);
    await learner
      .getByRole("button", { name: "Save this week’s mood", exact: true })
      .click();
    await dialog(learner);
    await expect(
      learner.getByText("Your mood is saved for this week.", { exact: true }),
    ).toBeVisible();
    await expect(learner.getByRole("radio")).toHaveCount(0);
    await expect(
      learner.getByLabel("My diary (optional)", { exact: true }),
    ).toHaveCount(0);
    await learner.reload();
    await expect(learner.locator(".mood-saved-value")).toHaveText(/Good/);
    const current = (await api("/client/moods/current", studentToken)).data;
    expect(current.entry.privateNote).toBe(privateNote);
    await api(
      "/client/moods/entries",
      studentToken,
      "POST",
      { weekStart: current.weekStart, moodLevel: 1, privateNote: "overwrite" },
      409,
    );
    for (let n = 1; n <= 14; n++) {
      if (n === 2 || n === 5) continue;
      const date = new Date(current.weekStart + "T00:00:00Z");
      date.setUTCDate(date.getUTCDate() - 7 * n);
      sql(
        "INSERT INTO mood_entry(student_id,week_start,mood_level,private_note) VALUES(" +
          student.id +
          ",'" +
          date.toISOString().slice(0, 10) +
          "'," +
          ((n % 5) + 1) +
          ",'Private: historical fixture');",
      );
    }
    await learner
      .getByRole("button", { name: "Refresh mood", exact: true })
      .click();
    await expect(learner.locator(".mood-trend-line")).toHaveCount(3);
    await expect(learner.locator(".mood-trend-point")).toHaveCount(10);
    await learner
      .getByRole("button", { name: "View table", exact: true })
      .click();
    await expect(
      learner.getByRole("cell", { name: "No record", exact: true }),
    ).toHaveCount(2);
    await learner
      .getByRole("button", { name: "View chart", exact: true })
      .click();
    const history = learner.locator(".mood-history");
    await expect(history.getByText("Page 1 · 13 items")).toBeVisible();
    await history.getByRole("button", { name: "Next", exact: true }).click();
    await expect(history.getByText("Page 2 · 13 items")).toBeVisible();
    await history
      .getByRole("button", { name: "Previous", exact: true })
      .click();
    await history.locator("summary").first().click();
    await expect(history.getByText(privateNote, { exact: true })).toBeVisible();
    await learner.getByLabel("Time range", { exact: true }).selectOption("4");
    await expect(learner.locator(".mood-trend-point")).toHaveCount(3);
    await learner
      .getByRole("button", { name: "View table", exact: true })
      .click();
    await expect(learner.locator(".table-scroll tbody tr")).toHaveCount(4);
    await learner.getByLabel("Time range", { exact: true }).selectOption("12");
    await expect(learner.locator(".table-scroll tbody tr")).toHaveCount(12);
    await learner
      .getByRole("button", { name: "View chart", exact: true })
      .click();
    await expect(learner.locator(".mood-trend-point")).toHaveCount(10);
    await learner.screenshot({
      path: path.join(screens, "mood-student-history.png"),
      fullPage: true,
    });
    await learner.setViewportSize({ width: 390, height: 844 });
    expect(
      await learner.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await learner
      .getByRole("button", { name: "View table", exact: true })
      .click();
    await learner.screenshot({
      path: path.join(screens, "mood-student-mobile.png"),
      fullPage: true,
    });
    await learner.setViewportSize({ width: 1365, height: 900 });
    const coach = await login(mentor.username, password, "mentor");
    await coach
      .getByRole("navigation", { name: "Mentor navigation", exact: true })
      .getByRole("link", { name: "Students", exact: true })
      .click();
    await coach
      .getByRole("navigation")
      .getByRole("link", { name: "Student check-ins", exact: true })
      .click();
    await expect(coach.getByText("Page 1 · 12 items")).toBeVisible();
    await coach.getByRole("button", { name: "Next", exact: true }).click();
    await expect(coach.getByText("Page 2 · 12 items")).toBeVisible();
    await coach.getByRole("button", { name: "Previous", exact: true }).click();
    await coach
      .getByPlaceholder("Search", { exact: true })
      .fill(student.username);
    await coach.getByRole("button", { name: "Search", exact: true }).click();
    await expect(coach.getByText("Page 1 · 1 items")).toBeVisible();
    await coach.getByRole("button", { name: "Clear", exact: true }).click();
    await expect(coach.getByText("Page 1 · 12 items")).toBeVisible();
    await coach
      .getByRole("link", {
        name: "View check-ins: " + student.username,
        exact: true,
      })
      .click();
    await expect(
      coach.getByRole("heading", { name: "Mood learner", exact: false }),
    ).toBeVisible();
    await expect(coach.locator(".mood-trend-line")).toHaveCount(3);
    await coach.locator(".mood-history summary").first().click();
    await expect(coach.getByText(privateNote, { exact: true })).toBeVisible();
    await expect(coach.locator("textarea,input[type=radio]")).toHaveCount(0);
    await expect(coach.locator(".mood-history summary")).toHaveCount(10);
    await coach.screenshot({
      path: path.join(screens, "mood-mentor-readonly.png"),
      fullPage: true,
    });
    const publicHistory = await api(
      "/moods/students/" + student.id + "/history",
      mentorToken,
    );
    expect(JSON.stringify(publicHistory)).not.toContain("privateNote");
    expect(JSON.stringify(publicHistory)).toContain("Private: historical fixture");
    await coach
      .getByRole("button", { name: "View table", exact: true })
      .click();
    await expect(
      coach.getByRole("cell", { name: "No record", exact: true }),
    ).toHaveCount(2);
    await coach
      .locator(".mood-history")
      .getByRole("button", { name: "Next", exact: true })
      .click();
    await expect(
      coach.locator(".mood-history").getByText("Page 2 · 13 items"),
    ).toBeVisible();
    await coach
      .getByRole("link", { name: "Back to students", exact: false })
      .click();
    await expect(coach).toHaveURL(/mentor\/mood$/);
    // A real administrator reassignment must revoke the old mentor and update the student's viewer list.
    await admin.goto("/#/manage/courses/" + cid);
    await admin
      .getByRole("button", { name: "Assign mentor", exact: true })
      .click();
    const picker = admin.getByRole("dialog", {
      name: "Assign mentor",
      exact: true,
    });
    await picker
      .getByPlaceholder("Search by name or username")
      .fill(otherMentor.username);
    await picker.getByRole("button", { name: "Search", exact: true }).click();
    await picker
      .getByRole("button", {
        name: "Choose an account: " + otherMentor.username,
        exact: true,
      })
      .click();
    await expect(picker).not.toBeVisible();
    await coach.goto("/#/mentor/mood/" + student.id);
    await expect(coach.getByRole("alert")).toContainText(
      "This student is not in your current published courses.",
    );
    await expect(coach.locator(".mood-trend-point")).toHaveCount(0);
    await learner
      .getByRole("button", { name: "Refresh mood", exact: true })
      .click();
    await learner.getByText("Who can see my mood?", { exact: true }).click();
    await expect(
      learner.getByText(otherMentor.username, { exact: false }),
    ).toBeVisible();
    await expect(
      learner.getByText(mentor.username, { exact: false }),
    ).toHaveCount(0);
    const newCoach = await login(otherMentor.username, password, "mentor");
    await newCoach.goto("/#/mentor/mood/" + student.id);
    await expect(
      newCoach.getByRole("heading", { name: "Mood learner", exact: false }),
    ).toBeVisible();
    await expect(newCoach.locator(".mood-trend-point")).toHaveCount(10);
    await newCoach.locator(".mood-history summary").first().click();
    await expect(
      newCoach.getByText(privateNote, { exact: true }),
    ).toBeVisible();
    await api(cp + "/students/" + student.id, adminToken, "DELETE");
    await newCoach.reload();
    await expect(newCoach.getByRole("alert")).toContainText(
      "This student is not in your current published courses.",
    );
    await learner
      .getByRole("button", { name: "Refresh mood", exact: true })
      .click();
    await learner.getByText("Who can see my mood?", { exact: true }).click();
    await expect(
      learner.getByText("Only you can view your mood right now.", {
        exact: true,
      }),
    ).toBeVisible();
    await learner.goto("/#/mentor/mood");
    await expect(
      learner.getByRole("heading", {
        name: /not have access|permission|unavailable/i,
      }),
    ).toBeVisible();
    // A transient transport failure must keep the session and offer a functioning retry.
    await learner.route("**/api/client/moods/current", (r) => r.abort());
    await learner.goto("/#/student/mood");
    await expect(learner.getByRole("alert")).toContainText(
      "Connection lost. Please try again.",
    );
    await learner.unroute("**/api/client/moods/current");
    await learner
      .getByRole("button", { name: "Try again", exact: true })
      .click();
    await expect(learner.locator(".mood-saved-value")).toHaveText(/Good/);
    expect(errors).toEqual([]);
    console.log(
      "PASS mood notice/decline/confirmation, once-only save, note privacy, truthful chart gaps/table, history/student pagination, mobile, current mentor reassignment/removal, administrator route/API denial, retries and persistence",
    );
  } finally {
    for (const c of contexts) await c.close();
  }
});
