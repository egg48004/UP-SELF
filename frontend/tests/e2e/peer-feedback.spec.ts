import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
test("peer feedback: students exchange versioned work and mentor moderates", async ({
  browser,
}) => {
  test.setTimeout(300000);
  const file = process.env.UPSELF_PEER_FIXTURE;
  test.skip(!file, "Run verify-peer-feedback.py --browser");
  const f = JSON.parse(fs.readFileSync(file!, "utf8"));
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(name: string, role: string) {
    const c = await browser.newContext();
    contexts.push(c);
    const p = await c.newPage();
    p.on("pageerror", (e) => errors.push(e.message));
    await p.goto("/#/login");
    await p.getByLabel("Choose your role", { exact: true }).selectOption(role);
    await p
      .getByLabel("Username", { exact: true })
      .fill(f.actors[name].username);
    await p
      .getByLabel("Password", { exact: true })
      .fill(f.actors[name].password);
    await p.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(p).toHaveURL(new RegExp("/" + role + "/home$"));
    return p;
  }
  async function submit(p: Page, answer: string) {
    await p.goto(`/#/student/assignments/${f.assignmentId}`);
    await expect(
      p.getByText("A classmate may view your submission and give feedback.", {
        exact: true,
      }),
    ).toBeVisible();
    await p.locator("#work-answer").fill(answer);
    await p
      .getByRole("button", { name: "Submit my work", exact: true })
      .click();
    await p
      .getByRole("dialog")
      .getByRole("button", { name: "Confirm", exact: true })
      .click();
    await expect(p.locator("#work-answer")).toHaveCount(0);
  }
  try {
    const student = await login("student", "student"),
      second = await login("second", "student"),
      mentor = await login("mentor", "mentor"),
      admin = await login("admin", "admin");
    await expect(
      student
        .getByRole("navigation", { name: "Student navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(5);
    await second.goto(`/#/student/assignments/${f.assignmentId}`);
    await second
      .locator("#work-file")
      .setInputFiles({
        name: "peer-example.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("A shared business example."),
      });
    await second
      .getByRole("button", { name: "Upload file", exact: true })
      .click();
    await expect(
      second.getByText("peer-example.txt", { exact: false }),
    ).toBeVisible();
    await submit(second, "I want to sell handmade bookmarks.");
    await submit(student, "I want to offer plant care.");
    const peer = student.locator(".peer-feedback");
    await peer
      .getByRole("button", { name: "Review a classmate", exact: true })
      .click();
    await expect(
      peer.getByText("I want to sell handmade bookmarks.", { exact: true }),
    ).toBeVisible();
    const downloaded = student.waitForEvent("download");
    await peer
      .getByRole("button", { name: "Download: peer-example.txt", exact: true })
      .click();
    const download = await downloaded;
    expect(download.suggestedFilename()).toBe("peer-example.txt");
    const stream = await download.createReadStream();
    const chunks = [];
    for await (const chunk of stream!) chunks.push(chunk);
    expect(Buffer.concat(chunks).toString()).toBe("A shared business example.");
    await peer
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(peer.getByRole("alert")).toHaveText(
      "Share one thing that works well.",
    );
    await peer
      .getByLabel("One thing that works well", { exact: true })
      .fill("kill yourself");
    await peer
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(peer.getByRole("alert")).toContainText("Change hurtful");
    await peer
      .getByLabel("One thing that works well", { exact: true })
      .fill("Your product idea is easy to understand.");
    await peer
      .getByLabel("One idea to try (optional)", { exact: true })
      .fill("Try showing one example.");
    await peer
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(peer.getByRole("status")).toHaveText("Feedback sent.");
    await expect(
      peer.getByRole("button", { name: "Send feedback", exact: true }),
    ).toHaveCount(0);
    await second
      .locator(".peer-feedback")
      .getByRole("button", { name: "Refresh peer feedback", exact: true })
      .click();
    await expect(
      second
        .locator(".peer-feedback")
        .getByText("Your product idea is easy to understand.", { exact: true }),
    ).toBeVisible();
    await expect(second.locator(".peer-feedback")).not.toContainText(
      f.actors.student.username,
    );
    // Reverse feedback is independently allocated and optional suggestion can be empty.
    await second
      .locator(".peer-feedback")
      .getByRole("button", { name: "Review a classmate", exact: true })
      .click();
    await second
      .getByLabel("One thing that works well", { exact: true })
      .fill("Plant care sounds useful.");
    await second
      .getByRole("button", { name: "Send feedback", exact: true })
      .click();
    await expect(
      second.locator(".peer-feedback").getByRole("status"),
    ).toHaveText("Feedback sent.");
    await mentor.goto(`/#/mentor/assignments/${f.assignmentId}`);
    const panel = mentor.locator(".peer-feedback");
    await expect(panel).toContainText(f.actors.student.username);
    await expect(panel).toContainText(f.actors.second.username);
    // Closing and reopening reuses the original disclosure and preserves sent feedback.
    for (const enabled of [false, true]) {
      await mentor.getByRole("button", { name: "Edit assignment", exact: true }).click();
      const editor=mentor.getByRole("dialog", { name: "Edit assignment", exact: true });
      await editor.getByLabel("Allow peer feedback", { exact: true }).setChecked(enabled);
      await editor.getByRole("button", { name: "Save assignment", exact: true }).click();
      await expect(editor).not.toBeVisible();
      await student.locator(".peer-feedback").getByRole("button", { name: "Refresh peer feedback", exact: true }).click();
      if (!enabled) await expect(student.getByText("Peer feedback is closed. Existing feedback is still available.", {exact:true})).toBeVisible();
      else await expect(student.getByText("Peer feedback is closed. Existing feedback is still available.", {exact:true})).toHaveCount(0);
    }
    const row = panel
      .locator("article")
      .filter({ hasText: "Your product idea is easy to understand." });
    await row
      .getByRole("button", { name: "Hide feedback", exact: true })
      .click();
    let d = mentor.getByRole("dialog", { name: "Hide feedback", exact: true });
    await d.getByRole("button", { name: "Cancel", exact: true }).click();
    await row
      .getByRole("button", { name: "Hide feedback", exact: true })
      .click();
    d = mentor.getByRole("dialog", { name: "Hide feedback", exact: true });
    await d
      .getByLabel("Reason for hiding", { exact: true })
      .fill("Moderation practice.");
    await d.getByRole("button", { name: "Hide feedback", exact: true }).click();
    await expect(row).toContainText("Hidden: Moderation practice.");
    await second
      .locator(".peer-feedback")
      .getByRole("button", { name: "Refresh peer feedback", exact: true })
      .click();
    await expect(
      second
        .locator(".peer-feedback")
        .getByText("Your product idea is easy to understand.", { exact: true }),
    ).toHaveCount(0);
    await student
      .locator(".peer-feedback")
      .getByRole("button", { name: "Refresh peer feedback", exact: true })
      .click();
    await expect(
      peer.getByText("Feedback hidden by your mentor.", { exact: true }),
    ).toBeVisible();
    await expect(
      admin
        .getByRole("navigation")
        .getByRole("link", { name: "Assignments", exact: true }),
    ).toHaveCount(0);
    await admin.goto(`/#/mentor/assignments/${f.assignmentId}`);
    await expect(
      admin.getByRole("button", { name: "Hide feedback", exact: true }),
    ).toHaveCount(0);
    await student.setViewportSize({ width: 320, height: 780 });
    await student.locator(".peer-feedback").scrollIntoViewIfNeeded();
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(path.dirname(file!), "student-peer-mobile.png"),
      fullPage: true,
    });
    await mentor.screenshot({
      path: path.join(path.dirname(file!), "mentor-peer.png"),
      fullPage: true,
    });
    expect(errors).toEqual([]);
  } finally {
    for (const c of contexts) await c.close();
  }
});
