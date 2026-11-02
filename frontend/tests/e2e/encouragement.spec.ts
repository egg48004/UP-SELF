import { test, expect } from "@playwright/test";
import type { BrowserContext, Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

test("mentor single/bulk encouragement, real retry, private inbox and three-role access", async ({
  browser,
}) => {
  test.setTimeout(180000);
  const fixture = process.env.UPSELF_ENCOURAGEMENT_FIXTURE;
  test.skip(
    !fixture,
    "Run scripts/verify-encouragement.py --browser with local services.",
  );
  const f = JSON.parse(fs.readFileSync(fixture!, "utf8")),
    folder = process.env.UPSELF_SCREENSHOTS!;
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(
    actor: { username: string; password: string },
    role: string,
  ) {
    const context = await browser.newContext();
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
  async function inbox(page: Page) {
    await page.goto("/#/student/home");
    await page
      .getByRole("link", { name: "View messages", exact: true })
      .click();
    await expect(
      page.getByRole("heading", {
        name: "Messages from your mentor",
        exact: true,
      }),
    ).toBeVisible();
  }
  try {
    const mentor = await login(f.actors.mentor, "mentor"),
      alice = await login(f.actors.alice, "student"),
      bob = await login(f.actors.bob, "student");
    await expect(
      mentor
        .getByRole("navigation", { name: "Mentor navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(4);
    await expect(
      alice
        .getByRole("navigation", { name: "Student navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(5);
    // Enter through an actual student-overview row and check server-validated preselection.
    await mentor.goto("/#/mentor/students");
    const aliceRow = mentor
      .locator("tbody tr")
      .filter({ hasText: f.actors.alice.username })
      .first();
    await aliceRow
      .getByRole("link", { name: "Send a message", exact: true })
      .click();
    await expect(
      mentor.getByText("Selected: 1", { exact: true }),
    ).toBeVisible();
    const picker = mentor.getByRole("region", {
      name: "Choose students",
      exact: true,
    });
    await expect(
      picker.getByRole("checkbox", { name: "Select DEMO alice", exact: true }),
    ).toBeChecked();
    await mentor
      .getByLabel("Your message", { exact: true })
      .fill("You are stupid");
    await mentor
      .getByRole("button", { name: "Send encouragement", exact: true })
      .click();
    await expect(mentor.getByRole("alert")).toContainText("Message not sent.");
    await expect(
      mentor.getByLabel("Your message", { exact: true }),
    ).toHaveValue("You are stupid");
    await expect(
      picker.getByRole("checkbox", { name: "Select DEMO alice", exact: true }),
    ).toBeChecked();
    const single = "You made a good start. Take one small step.";
    await mentor.getByLabel("Your message", { exact: true }).fill(single);
    await mentor
      .getByRole("button", { name: "Send encouragement", exact: true })
      .click();
    await expect(
      mentor.getByRole("status").filter({ hasText: "Message sent." }),
    ).toBeVisible();
    await expect(
      mentor.getByLabel("Your message", { exact: true }),
    ).toHaveValue("");
    await expect(
      mentor.getByText("Selected: 0", { exact: true }),
    ).toBeVisible();
    await inbox(alice);
    await expect(alice.getByText(single, { exact: true })).toBeVisible();
    await inbox(bob);
    await expect(bob.getByText(single, { exact: true })).toHaveCount(0);
    await expect(alice.getByText("Recipients:", { exact: false })).toHaveCount(
      0,
    );
    // Retain selection across searches and pages, with explicit clearing/removal controls.
    await picker
      .getByLabel("Search students", { exact: true })
      .fill(f.actors.alice.username);
    await picker.getByRole("button", { name: "Search", exact: true }).click();
    await picker
      .getByRole("checkbox", { name: "Select DEMO alice", exact: true })
      .check();
    await picker
      .getByLabel("Search students", { exact: true })
      .fill(f.actors.bob.username);
    await picker.getByRole("button", { name: "Search", exact: true }).click();
    await picker
      .getByRole("checkbox", { name: "Select DEMO bob", exact: true })
      .check();
    await expect(
      mentor.getByText("Selected: 2", { exact: true }),
    ).toBeVisible();
    await picker
      .getByRole("button", { name: "Clear selection", exact: true })
      .click();
    await expect(
      mentor.getByText("Selected: 0", { exact: true }),
    ).toBeVisible();
    await picker.getByRole("button", { name: "Clear", exact: true }).click();
    await picker
      .getByRole("checkbox", { name: "Select DEMO alice", exact: true })
      .check();
    await picker
      .getByRole("checkbox", { name: "Select DEMO bob", exact: true })
      .check();
    await picker.getByRole("button", { name: "Next", exact: true }).click();
    await picker
      .getByRole("checkbox", { name: "Select DEMO page08", exact: true })
      .check();
    await picker.getByRole("button", { name: "Previous", exact: true }).click();
    await expect(
      picker.getByRole("checkbox", { name: "Select DEMO alice", exact: true }),
    ).toBeChecked();
    await picker
      .getByRole("button", { name: "Remove DEMO page08", exact: true })
      .click();
    await expect(
      mentor.getByText("Selected: 2", { exact: true }),
    ).toBeVisible();
    // Complete the real server request, then deliberately drop its reply; retry must recover one delivery.
    const bulk = "This week, try one small step. You can ask for help.";
    await mentor.getByLabel("Your message", { exact: true }).fill(bulk);
    await mentor.route(
      "**/api/encouragement/messages",
      async (route) => {
        await route.fetch();
        await route.abort("failed");
      },
      { times: 1 },
    );
    await mentor
      .getByRole("button", { name: "Send encouragement", exact: true })
      .click();
    await expect(mentor.getByRole("alert")).toContainText(
      "Connection lost. Please try again.",
    );
    await expect(
      mentor.getByLabel("Your message", { exact: true }),
    ).toHaveValue(bulk);
    await expect(
      mentor.getByText("Selected: 2", { exact: true }),
    ).toBeVisible();
    await mentor
      .getByRole("button", { name: "Send encouragement", exact: true })
      .click();
    await expect(
      mentor.getByRole("status").filter({ hasText: "Message sent." }),
    ).toBeVisible();
    await alice
      .getByRole("button", { name: "Refresh messages", exact: true })
      .click();
    await bob
      .getByRole("button", { name: "Refresh messages", exact: true })
      .click();
    await expect(alice.getByText(bulk, { exact: true })).toHaveCount(1);
    await expect(bob.getByText(bulk, { exact: true })).toHaveCount(1);
    await expect(
      bob.getByText(f.actors.alice.username, { exact: true }),
    ).toHaveCount(0);
    await mentor
      .getByRole("button", { name: "Sent messages", exact: true })
      .click();
    const sent = mentor.locator("li.panel").filter({ hasText: bulk });
    await expect(sent).toHaveCount(1);
    await expect(sent).toContainText("Recipients: 2");
    await sent
      .getByRole("button", { name: "View recipients", exact: true })
      .click();
    await expect(sent.getByText("DEMO alice", { exact: false })).toBeVisible();
    await expect(sent.getByText("DEMO bob", { exact: false })).toBeVisible();
    await sent
      .getByRole("button", { name: "Hide recipients", exact: true })
      .click();
    await expect(sent.locator(".recipient-names")).toHaveCount(0);
    await mentor.getByRole("button", { name: "Next", exact: true }).click();
    await expect(mentor.getByText("Page 2", { exact: false })).toBeVisible();
    await mentor.getByRole("button", { name: "Previous", exact: true }).click();
    await mentor
      .getByRole("button", { name: "Refresh messages", exact: true })
      .click();
    await expect(mentor.getByText(bulk, { exact: true })).toHaveCount(1);
    await alice.getByRole("button", { name: "Next", exact: true }).click();
    await expect(alice.getByText("Page 2", { exact: false })).toBeVisible();
    await alice.getByRole("button", { name: "Previous", exact: true }).click();
    await alice.reload();
    await expect(alice.getByText(bulk, { exact: true })).toBeVisible();
    await mentor.screenshot({
      path: path.join(folder, "mentor-sent-encouragement.png"),
      fullPage: true,
    });
    // Current mentor relationship does not grant a second mentor access to the first mentor's history.
    const other = await login(f.actors.othermentor, "mentor");
    await other.goto("/#/mentor/students/encouragement?tab=history");
    await expect(
      other.getByText("Another mentor can support your learning.", {
        exact: true,
      }),
    ).toBeVisible();
    await expect(other.getByText(bulk, { exact: true })).toHaveCount(0);
    // Reading preferences, mobile layout and keyboard controls remain usable.
    await alice
      .getByRole("link", { name: "Reading settings", exact: true })
      .click();
    await alice.locator("#text-size").selectOption("extraLarge");
    await alice.locator("#contrast").selectOption("highContrast");
    await alice.goto("/#/student/encouragement");
    await alice.setViewportSize({ width: 320, height: 844 });
    await expect(alice.getByText(bulk, { exact: true })).toBeVisible();
    expect(
      await alice.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await alice.screenshot({
      path: path.join(folder, "student-encouragement-mobile.png"),
      fullPage: true,
    });
    await alice
      .getByRole("button", { name: "Refresh messages", exact: true })
      .focus();
    await alice.keyboard.press("Enter");
    await expect(alice.getByText(bulk, { exact: true })).toBeVisible();
    const empty = await login(f.actors.draftonly, "student");
    await inbox(empty);
    await expect(
      empty.getByText("No messages yet.", { exact: true }),
    ).toBeVisible();
    await alice.goto("/#/mentor/students/encouragement");
    await expect(alice).toHaveURL(/\/403$/);
    const admin = await login(f.admin, "admin");
    await expect(
      admin
        .getByRole("navigation", {
          name: "Administrator navigation",
          exact: true,
        })
        .getByRole("link"),
    ).toHaveCount(5);
    await expect(
      admin.getByRole("link", {
        name: /View messages|Send a message|Sent messages/,
      }),
    ).toHaveCount(0);
    await admin.goto("/#/mentor/students/encouragement");
    await expect(admin).toHaveURL(/\/403$/);
    await admin.goto("/#/student/encouragement");
    await expect(admin).toHaveURL(/\/403$/);
    await admin.screenshot({
      path: path.join(folder, "administrator-encouragement-denied.png"),
      fullPage: true,
    });
    expect(errors).toEqual([]);
  } finally {
    for (const context of contexts) await context.close();
  }
});
