import { test, expect } from "@playwright/test";
import type { BrowserContext, Page, Locator } from "@playwright/test";
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
test("chat filtering, own group visibility, teacher protection and bounded history", async ({
  browser,
}) => {
  test.setTimeout(240000);
  const file = process.env.UPSELF_CHAT_SAFETY_FIXTURE;
  test.skip(
    !file,
    "Run scripts/verify-chat-safety.py --browser with local services.",
  );
  const f = JSON.parse(fs.readFileSync(file!, "utf8")),
    folder = process.env.UPSELF_SCREENSHOTS!;
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(
    actor: { username: string; password: string },
    role: string,
  ) {
    const context = await browser.newContext({ permissions: ["microphone"] });
    contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
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
  async function open(
    page: Page,
    role: string,
    title = "Inclusive study circle",
  ) {
    await page.goto(
      `/#/${role === "student" ? "student" : "manage"}/courses/${f.courseId}/discussions`,
    );
    await page
      .getByRole("button", { name: title + " · Active", exact: true })
      .click();
    await expect(
      page.getByRole("heading", { name: title, exact: true }),
    ).toBeVisible();
  }
  async function text(page: Page, body: string) {
    await page.getByLabel("Your message", { exact: true }).fill(body);
    await page.getByRole("button", { name: "Send", exact: true }).click();
  }
  async function closeMembers(page: Page) {
    await page
      .getByRole("dialog", { name: "Members", exact: true })
      .getByRole("button", { name: "Close this dialog", exact: true })
      .click();
  }
  async function play(audio: Locator) {
    await audio.evaluate(async (el) => {
      await (el as HTMLAudioElement).play();
    });
    await expect
      .poll(() => audio.evaluate((el) => (el as HTMLAudioElement).currentTime))
      .toBeGreaterThan(0.05);
    expect(
      await audio.evaluate((el) => (el as HTMLAudioElement).error),
    ).toBeNull();
  }
  try {
    const mentor = await login(f.actors.mentor, "mentor");
    await mentor.goto(`/#/manage/courses/${f.courseId}/discussions`);
    await mentor
      .getByRole("button", { name: "Create group", exact: true })
      .click();
    let dialog = mentor.getByRole("dialog", { name: "New group", exact: true });
    await dialog
      .getByLabel("Group name", { exact: true })
      .fill("Inclusive study circle");
    await dialog
      .getByRole("button", { name: "Create group", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", {
        name: "Inclusive study circle",
        exact: true,
      }),
    ).toBeVisible();
    await mentor.getByRole("button", { name: "Members", exact: true }).click();
    dialog = mentor.getByRole("dialog", { name: "Members", exact: true });
    for (const name of ["alice", "bob", "charlie"]) {
      await dialog
        .getByLabel("Choose a student", { exact: true })
        .selectOption(String(f.actors[name].id));
      await dialog
        .getByRole("button", { name: "Add member", exact: true })
        .click();
      await expect(
        dialog.getByRole("button", {
          name: "Remove member: DEMO " + name,
          exact: true,
        }),
      ).toBeVisible();
    }
    await closeMembers(mentor);
    const alice = await login(f.actors.alice, "student"),
      bob = await login(f.actors.bob, "student"),
      charlie = await login(f.actors.charlie, "student");
    await alice.route(
      "**/api/client/chat/groups/*/sync*",
      (route) => route.abort(),
      { times: 1 },
    );
    await alice.goto(`/#/student/courses/${f.courseId}/discussions`);
    await alice
      .getByRole("button", {
        name: "Inclusive study circle · Active",
        exact: true,
      })
      .click();
    await expect(alice.getByRole("alert")).toContainText(
      "Connection lost. Please try again.",
    );
    await alice.getByRole("button", { name: "Try again", exact: true }).click();
    await expect(
      alice.getByRole("heading", {
        name: "Inclusive study circle",
        exact: true,
      }),
    ).toBeVisible();
    await open(bob, "student");
    await open(charlie, "student");
    await text(mentor, "Meeting link: https://meet.example.com/learning");
    await text(alice, "Can we work on the first step together?");
    await expect(
      bob.getByText("Can we work on the first step together?", { exact: true }),
    ).toBeVisible({ timeout: 15000 });
    // A rejected message keeps the original pending files and editor value.
    await bob.getByText("Add a file", { exact: true }).click();
    await bob
      .getByLabel("Choose discussion file", { exact: true })
      .setInputFiles(path.join(folder, "diagram.png"));
    await bob
      .getByLabel("Image description", { exact: true })
      .fill("A learning step diagram");
    await bob.getByRole("button", { name: "Upload file", exact: true }).click();
    await expect(
      bob.getByRole("button", {
        name: "Remove attachment: diagram.png",
        exact: true,
      }),
    ).toBeVisible();
    await bob
      .getByLabel("Choose discussion file", { exact: true })
      .setInputFiles(path.join(folder, "notes.txt"));
    await bob.getByRole("button", { name: "Upload file", exact: true }).click();
    const recorder = bob.getByRole("region", {
      name: "Voice recording (optional)",
      exact: true,
    });
    await recorder.getByRole("button", { name: "Record", exact: true }).click();
    await expect(recorder.getByRole("status")).toContainText(
      /Recording · 0:0[1-9]/,
    );
    await recorder
      .getByRole("button", { name: "Stop recording", exact: true })
      .click();
    await play(recorder.locator("audio"));
    await recorder
      .getByRole("button", { name: "Delete recording", exact: true })
      .click();
    await expect(recorder.locator("audio")).toHaveCount(0);
    await recorder.getByRole("button", { name: "Record", exact: true }).click();
    await expect(recorder.getByRole("status")).toContainText(
      /Recording · 0:0[1-9]/,
    );
    await recorder
      .getByRole("button", { name: "Stop recording", exact: true })
      .click();
    await recorder
      .getByRole("button", { name: "Upload recording", exact: true })
      .click();
    await expect(recorder.locator("audio")).toHaveCount(0);
    await text(bob, "YOU ARE STUPID!");
    await expect(bob.getByRole("alert")).toContainText(
      "Message not sent. Change hurtful or threatening words and try again.",
    );
    await expect(bob.getByLabel("Your message", { exact: true })).toHaveValue(
      "YOU ARE STUPID!",
    );
    await expect(
      bob.getByRole("button", {
        name: "Remove attachment: diagram.png",
        exact: true,
      }),
    ).toBeVisible();
    await text(bob, "Here are my notes, image and voice.");
    const peerMessage = alice
      .locator("li.message")
      .filter({ hasText: "Here are my notes, image and voice." });
    await expect(peerMessage).toBeVisible({ timeout: 15000 });
    await peerMessage
      .getByRole("button", { name: "View image", exact: true })
      .click();
    await expect(
      peerMessage.getByAltText("A learning step diagram"),
    ).toBeVisible();
    await expect
      .poll(() =>
        peerMessage
          .locator("img")
          .evaluate((el) => (el as HTMLImageElement).naturalWidth),
      )
      .toBeGreaterThan(0);
    const download = alice.waitForEvent("download");
    await peerMessage
      .getByRole("button", { name: "Download: notes.txt", exact: true })
      .click();
    expect(fs.readFileSync((await (await download).path())!, "utf8")).toBe(
      "One learning step.",
    );
    await peerMessage.getByRole("button", { name: /^Listen:/ }).click();
    await play(peerMessage.locator("audio"));
    const audio = await peerMessage.locator("audio").elementHandle();
    // Hide from the message menu; this unmounts image and audio players immediately.
    await peerMessage.locator("summary").click();
    await peerMessage
      .getByRole("button", { name: "Hide messages", exact: true })
      .click();
    await alice.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(peerMessage).toHaveCount(0);
    expect(await audio!.evaluate((el) => (el as HTMLAudioElement).paused)).toBe(
      true,
    );
    await expect(
      alice.getByText("Hidden classmates: 1.", {
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      alice.getByText("Meeting link: https://meet.example.com/learning", {
        exact: true,
      }),
    ).toBeVisible();
    const teacherMessage = alice
      .locator("li.message")
      .filter({ hasText: "Meeting link:" });
    await teacherMessage.locator("summary").click();
    await expect(
      teacherMessage.getByRole("button", {
        name: "Hide messages",
        exact: true,
      }),
    ).toHaveCount(0);
    await expect(
      teacherMessage.getByRole("button", { name: "Report user", exact: true }),
    ).toBeVisible();
    await teacherMessage.locator("summary").click();
    await text(bob, "A later peer message.");
    await alice
      .getByRole("button", { name: "Refresh discussion", exact: true })
      .click();
    await expect(
      charlie.getByText("A later peer message.", { exact: true }),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      mentor.getByText("A later peer message.", { exact: true }),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      alice.getByText("A later peer message.", { exact: true }),
    ).toHaveCount(0);
    await text(alice, "Thanks. I can continue learning.");
    await expect(
      bob.getByText("Thanks. I can continue learning.", { exact: true }),
    ).toBeVisible({ timeout: 15000 });
    await alice.reload();
    await alice
      .getByRole("button", {
        name: "Inclusive study circle · Active",
        exact: true,
      })
      .click();
    await expect(
      alice.getByText("Hidden classmates: 1.", {
        exact: true,
      }),
    ).toBeVisible();
    await expect(peerMessage).toHaveCount(0);
    await alice.screenshot({
      path: path.join(folder, "student-hidden-peer.png"),
      fullPage: true,
    });
    // Restore from Members, without changing anyone else's membership.
    await alice
      .getByRole("button", { name: "Manage in Members", exact: true })
      .click();
    await alice
      .getByRole("dialog", { name: "Members", exact: true })
      .getByRole("button", { name: "Show messages: DEMO bob", exact: true })
      .click();
    await closeMembers(alice);
    await expect(peerMessage).toBeVisible();
    await peerMessage.getByRole("button", { name: /^Listen:/ }).click();
    await play(peerMessage.locator("audio"));
    await peerMessage.locator("summary").click();
    await peerMessage
      .getByRole("button", { name: "Report user", exact: true })
      .click();
    await expect(
      alice.getByRole("dialog", { name: "Report user", exact: true }),
    ).toBeVisible();
    await alice
      .getByRole("dialog", { name: "Report user", exact: true })
      .getByRole("button", { name: "Cancel", exact: true })
      .click();
    // Mentor text uses the same filter, while teaching links are permitted.
    await text(mentor, "You are an idiot");
    await expect(mentor.getByRole("alert")).toContainText("Message not sent.");
    await text(mentor, "Please ask for help. We can practise together.");
    await expect(
      alice.getByText("Please ask for help. We can practise together.", {
        exact: true,
      }),
    ).toBeVisible({ timeout: 15000 });
    await mentor.getByRole("button", { name: "Members", exact: true }).click();
    await expect(
      mentor
        .getByRole("dialog", { name: "Members", exact: true })
        .getByRole("button", { name: /Hide messages|Show messages/ }),
    ).toHaveCount(0);
    await closeMembers(mentor);
    // Both hide and restore are keyboard-accessible from the member list.
    await alice.getByRole("button", { name: "Members", exact: true }).click();
    const hideButton = alice.getByRole("button", {
      name: "Hide messages: DEMO bob",
      exact: true,
    });
    await hideButton.focus();
    await alice.keyboard.press("Enter");
    await alice.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(
      alice.getByRole("button", {
        name: "Hide messages: DEMO bob",
        exact: true,
      }),
    ).toBeVisible();
    await closeMembers(alice);
    // Older history remains reachable with a bounded live DOM window.
    await open(alice, "student", "Long history group");
    for (let i = 0; i < 9; i++) {
      const earlier = alice.getByRole("button", {
        name: "Load earlier messages",
        exact: true,
      });
      if (!(await earlier.count())) break;
      await earlier.click();
      await expect(alice.locator("li.message")).toHaveCount(
        Math.min(60 + i * 30, 200),
      );
    }
    await expect(alice.getByText("History 000", { exact: true })).toBeVisible();
    expect(await alice.locator("li.message").count()).toBeLessThanOrEqual(200);
    await expect(
      alice.getByRole("button", {
        name: "Back to latest messages",
        exact: true,
      }),
    ).toBeVisible();
    await alice
      .getByLabel("Your message", { exact: true })
      .fill("My unsent question");
    await alice
      .getByRole("button", { name: "Back to latest messages", exact: true })
      .click();
    await expect(
      alice.getByText("Visible after hidden history.", { exact: true }),
    ).toBeVisible();
    await expect(alice.getByLabel("Your message", { exact: true })).toHaveValue(
      "My unsent question",
    );
    await alice.getByLabel("Your message", { exact: true }).fill("");
    await open(alice, "student");
    await alice.setViewportSize({ width: 390, height: 844 });
    await alice.screenshot({
      path: path.join(folder, "student-chat-mobile.png"),
      fullPage: true,
    });
    expect(
      await alice.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await open(alice, "student", "Safety study group");
    await alice.getByRole("button", { name: "Members", exact: true }).click();
    await alice
      .getByRole("button", { name: "Hide messages: DEMO bob", exact: true })
      .click();
    await alice.getByRole("button", { name: "Confirm", exact: true }).click();
    await closeMembers(alice);
    await expect(
      alice.getByText(
        "Some older messages stay visible to protect teaching information.",
        { exact: true },
      ),
    ).toBeVisible();
    await expect(
      alice.getByText("Legacy message stays visible", { exact: true }),
    ).toBeVisible();
    await expect(
      alice.getByText("Historical teacher message stays visible", {
        exact: true,
      }),
    ).toBeVisible();
    await mentor.screenshot({
      path: path.join(folder, "mentor-chat-safety.png"),
      fullPage: true,
    });
    const admin = await login(
      { username: "admin01", password: f.adminPassword },
      "admin",
    );
    await admin.goto(`/#/manage/courses/${f.courseId}`);
    await expect(
      admin.getByRole("link", { name: "Discussion", exact: true }),
    ).toHaveCount(0);
    await admin.goto(`/#/manage/courses/${f.courseId}/discussions`);
    await expect(admin).toHaveURL(/\/403$/);
    await admin.screenshot({
      path: path.join(folder, "administrator-chat-denied.png"),
      fullPage: true,
    });
    expect(errors).toEqual([]);
  } finally {
    for (const context of contexts) await context.close();
  }
});
