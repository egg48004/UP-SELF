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
test("course group chat: mentor manages, students share text/image/file/voice, access is revoked", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixturePath = process.env.UPSELF_CHAT_FIXTURE;
  test.skip(
    !fixturePath,
    "Run scripts/verify-chat.py --browser to create disposable fixtures.",
  );
  const f = JSON.parse(fs.readFileSync(fixturePath!, "utf8"));
  const folder = process.env.UPSELF_SCREENSHOTS!;
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
  async function closeDialog(page: Page) {
    await page
      .getByRole("dialog", {name:"Members", exact:true})
      .getByRole("button", { name: "Close this dialog", exact: true })
      .click();
  }
  async function play(audio: Locator) {
    await expect(audio).toBeVisible();
    await audio.evaluate(async (el) => {
      await (el as HTMLAudioElement).play();
    });
    await expect
      .poll(() => audio.evaluate((el) => (el as HTMLAudioElement).currentTime))
      .toBeGreaterThan(0.05);
    expect(
      await audio.evaluate((el) => (el as HTMLAudioElement).error),
    ).toBeNull();
    await audio.evaluate((el) => (el as HTMLAudioElement).pause());
  }
  async function record(page: Page) {
    const recorder = page.getByRole("region", {
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
    await expect(recorder.locator("audio")).toHaveAttribute("src", /^blob:/);
    return recorder;
  }
  async function openStudent(page: Page) {
    await page.goto("/#/student/courses/" + f.courseId);
    await page.getByRole("link", { name: "Discussion", exact: true }).click();
    await page
      .getByRole("button", { name: "Peer study circle · Active", exact: true })
      .click();
    await expect(
      page.getByRole("heading", { name: "Peer study circle", exact: true }),
    ).toBeVisible();
  }
  try {
    const mentor = await login(f.actors.mentor, "mentor");
    await mentor.goto("/#/manage/courses/" + f.courseId);
    await mentor.getByRole("link", { name: "Discussion", exact: true }).click();
    await mentor
      .getByRole("button", { name: "Create group", exact: true })
      .click();
    let dialog = mentor.getByRole("dialog");
    await dialog
      .getByLabel("Group name", { exact: true })
      .fill("Peer study circle");
    await dialog
      .getByRole("button", { name: "Create group", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", { name: "Peer study circle", exact: true }),
    ).toBeVisible();
    await mentor.getByRole("button", { name: "Members", exact: true }).click();
    dialog = mentor.getByRole("dialog");
    for (const name of ["alice", "bob"]) {
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
    await closeDialog(mentor);
    const alice = await login(f.actors.alice, "student");
    const bob = await login(f.actors.bob, "student");
    await openStudent(alice);
    await openStudent(bob);
    await alice
      .getByLabel("Your message", { exact: true })
      .fill("Can we explain step one together?");
    await alice.getByRole("button", { name: "Send", exact: true }).click();
    await expect(
      bob.getByText("Can we explain step one together?", { exact: true }),
    ).toBeVisible({ timeout: 15000 });
    await bob
      .getByLabel("Your message", { exact: true })
      .fill("Yes. Let us start with the instructions.");
    await bob.getByRole("button", { name: "Send", exact: true }).click();
    await expect(
      alice.getByText("Yes. Let us start with the instructions.", {
        exact: true,
      }),
    ).toBeVisible({ timeout: 15000 });
    await alice.getByText("Add a file", { exact: true }).click();
    await alice
      .getByLabel("Choose discussion file", { exact: true })
      .setInputFiles(path.join(folder, "diagram.png"));
    await alice
      .getByLabel("Image description", { exact: true })
      .fill("A diagram of the first learning step");
    await alice
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(
      alice.getByRole("heading", { name: "Your pending files", exact: true }),
    ).toBeVisible();
    await alice
      .getByRole("button", {
        name: "Remove attachment: diagram.png",
        exact: true,
      })
      .click();
    await expect(
      alice.getByRole("heading", { name: "Your pending files", exact: true }),
    ).toHaveCount(0);
    await alice
      .getByLabel("Choose discussion file", { exact: true })
      .setInputFiles(path.join(folder, "diagram.png"));
    await alice
      .getByLabel("Image description", { exact: true })
      .fill("A diagram of the first learning step");
    await alice
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await alice
      .getByLabel("Your message", { exact: true })
      .fill("Here is a diagram.");
    await alice.getByRole("button", { name: "Send", exact: true }).click();
    const imageMessage = bob
      .locator("li.message")
      .filter({ hasText: "Here is a diagram." });
    await expect(imageMessage).toBeVisible({ timeout: 15000 });
    await imageMessage
      .getByRole("button", { name: "View image", exact: true })
      .click();
    await expect(
      imageMessage.getByAltText("A diagram of the first learning step"),
    ).toBeVisible();
    await expect
      .poll(() =>
        imageMessage
          .locator("img")
          .evaluate((el) => (el as HTMLImageElement).naturalWidth),
      )
      .toBeGreaterThan(0);
    await imageMessage
      .getByRole("button", { name: "Hide image", exact: true })
      .click();
    await alice
      .getByLabel("Choose discussion file", { exact: true })
      .setInputFiles(path.join(folder, "study-notes.txt"));
    await alice
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(
      alice.getByRole("button", {
        name: "Remove attachment: study-notes.txt",
        exact: true,
      }),
    ).toBeVisible();
    let recorder = await record(alice);
    await play(recorder.locator("audio"));
    await recorder
      .getByRole("button", { name: "Delete recording", exact: true })
      .click();
    await expect(recorder.locator("audio")).toHaveCount(0);
    recorder = await record(alice);
    await recorder
      .getByRole("button", { name: "Upload recording", exact: true })
      .click();
    await expect(recorder.locator("audio")).toHaveCount(0);
    await alice
      .getByLabel("Your message", { exact: true })
      .fill("My notes and a short explanation.");
    await alice.getByRole("button", { name: "Send", exact: true }).click();
    const voiceMessage = bob
      .locator("li.message")
      .filter({ hasText: "My notes and a short explanation." });
    await expect(voiceMessage).toBeVisible({ timeout: 15000 });
    const download = bob.waitForEvent("download");
    await voiceMessage
      .getByRole("button", { name: "Download: study-notes.txt", exact: true })
      .click();
    const downloaded = await download;
    expect(downloaded.suggestedFilename()).toBe("study-notes.txt");
    expect(fs.readFileSync((await downloaded.path())!, "utf8")).toBe(
      "One step at a time.",
    );
    await voiceMessage.getByRole("button", { name: /^Listen:/ }).click();
    await play(voiceMessage.locator("audio"));
    const mentorVoice = mentor
      .locator("li.message")
      .filter({ hasText: "My notes and a short explanation." });
    await expect(mentorVoice).toBeVisible({ timeout: 15000 });
    await mentorVoice.getByRole("button", { name: /^Listen:/ }).click();
    await play(mentorVoice.locator("audio"));
    await mentor.getByLabel("Your message", {exact:true}).fill("Thanks. Explain your steps to each other.");
    await mentor.getByRole("button", {name:"Send",exact:true}).click();
    await expect(alice.getByText("Thanks. Explain your steps to each other.", {exact:true})).toBeVisible({timeout:15000});
    await mentor.screenshot({
      path: path.join(folder, "mentor-course-chat.png"),
      fullPage: true,
    });
    await alice.reload();
    await alice
      .getByRole("button", { name: "Peer study circle · Active", exact: true })
      .click();
    await expect(
      alice.getByText("My notes and a short explanation.", { exact: true }),
    ).toBeVisible();
    await bob.getByRole("button", { name: "Members", exact: true }).click();
    await expect(bob.getByRole("dialog", {name:"Members",exact:true}).getByText(/DEMO alice/)).toBeVisible();
    await closeDialog(bob);
    await mentor.getByRole("button", {name:"Refresh discussion",exact:true}).click();
    await mentor.getByRole("button", {name:"Create group",exact:true}).click();
    await mentor.getByRole("dialog", {name:"New group",exact:true}).getByRole("button", {name:"Cancel",exact:true}).click();
    await mentor.getByRole("button", {name:"Edit group",exact:true}).click();
    await mentor.getByRole("dialog", {name:"Edit group",exact:true}).getByRole("button", {name:"Cancel",exact:true}).click();
    await alice.getByRole("button", {name:"Assignment study group · Active",exact:true}).click();
    await alice.getByRole("button", {name:"Load earlier messages",exact:true}).click();
    await expect(alice.getByText("Hello, let us work together 👋", {exact:true})).toBeVisible();
    await alice.getByRole("button", {name:"Peer study circle · Active",exact:true}).click();
    await bob.setViewportSize({ width: 390, height: 844 });
    await bob.screenshot({
      path: path.join(folder, "student-course-chat-mobile.png"),
      fullPage: true,
    });
    expect(
      await bob.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
    ).toBe(true);
    await mentor
      .getByRole("button", { name: "Edit group", exact: true })
      .click();
    dialog = mentor.getByRole("dialog");
    await dialog
      .getByLabel("Group status", { exact: true })
      .selectOption("CLOSED");
    await dialog
      .getByRole("button", { name: "Save group", exact: true })
      .click();
    await expect(
      bob.getByText(
        "This discussion is closed. You can read earlier messages.",
        { exact: true },
      ),
    ).toBeVisible({ timeout: 15000 });
    await expect(bob.getByRole("button", {name:"Peer study circle · Closed",exact:true})).toBeVisible();
    await expect(bob.getByLabel("Your message", { exact: true })).toHaveCount(
      0,
    );
    await mentor
      .getByRole("button", { name: "Edit group", exact: true })
      .click();
    dialog = mentor.getByRole("dialog");
    await dialog
      .getByLabel("Group status", { exact: true })
      .selectOption("ACTIVE");
    await dialog
      .getByRole("button", { name: "Save group", exact: true })
      .click();
    await expect(bob.getByLabel("Your message", { exact: true })).toBeVisible({
      timeout: 15000,
    });
    await mentor.getByRole("button", { name: "Members", exact: true }).click();
    dialog = mentor.getByRole("dialog");
    await dialog
      .getByRole("button", { name: "Remove member: DEMO bob", exact: true })
      .click();
    await mentor.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(
      dialog.getByRole("button", {
        name: "Remove member: DEMO bob",
        exact: true,
      }),
    ).toHaveCount(0);
    await closeDialog(mentor);
    await expect(bob.getByRole("alert")).toContainText(
      "This discussion is not available to you.",
      { timeout: 15000 },
    );
    await expect(bob.locator("li.message")).toHaveCount(0);
    const admin = await login(
      { username: "admin01", password: f.adminPassword },
      "admin",
    );
    await admin.goto("/#/manage/courses/" + f.courseId);
    await expect(
      admin.getByRole("link", { name: "Discussion", exact: true }),
    ).toHaveCount(0);
    await admin.goto("/#/manage/courses/" + f.courseId + "/discussions");
    await expect(admin).toHaveURL(/\/403$/);
    expect(errors).toEqual([]);
  } finally {
    for (const context of contexts) await context.close();
  }
});
