import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";
import path from "node:path";

test("personal goals: real CRUD, persistence and role/owner isolation", async ({ browser, request }) => {
  test.skip(!process.env.UPSELF_GOAL_STUDENT, "Run through scripts/verify-goals.py --browser to create isolated goal fixtures.");
  test.setTimeout(180000);
  const contexts = [], errors: string[] = [];
  async function page() {
    const context = await browser.newContext();
    contexts.push(context);
    const p = await context.newPage();
    p.on("pageerror", e => errors.push(e.message));
    return p;
  }
  async function login(p: Page, role: string, username: string, password: string) {
    await p.goto("/#/login");
    await p.getByLabel("Choose your role", { exact: true }).selectOption(role);
    await p.getByLabel("Username", { exact: true }).fill(username);
    await p.getByLabel("Password", { exact: true }).fill(password);
    await p.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(p).toHaveURL(new RegExp(role + "/home"));
  }
  try {
    const student = await page();
    await login(student, "student", process.env.UPSELF_GOAL_STUDENT!, process.env.UPSELF_GOAL_STUDENT_PASSWORD!);
    await student.getByRole("navigation").getByRole("link", { name: "My goals" }).click();
    await expect(student.getByRole("heading", { name: "My goals", exact: true })).toBeVisible();
    await expect(student.getByText("Your goals are private. Only you can view and change them.")).toBeVisible();
    const title = "Finish one lesson", edited = "Finish a small lesson";
    await student.getByRole("button", { name: "Create goal", exact: true }).click();
    let dialog = student.getByRole("dialog", { name: "Create goal", exact: true });
    await dialog.getByLabel("My next step", { exact: true }).fill("Cancelled goal");
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(student.getByRole("heading", { name: "Cancelled goal" })).toHaveCount(0);
    await student.getByRole("button", { name: "Create goal", exact: true }).click();
    dialog = student.getByRole("dialog", { name: "Create goal", exact: true });
    await dialog.getByLabel("My next step", { exact: true }).fill("   ");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog.getByRole("alert")).toHaveText("Please enter a goal.");
    await dialog.getByLabel("My next step", { exact: true }).fill(title);
    await dialog.getByLabel("Notes (optional)", { exact: true }).fill("<img src=x onerror=alert(1)>\nTake one step.");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    let card = student.getByRole("article", { name: title, exact: true });
    await expect(card).toBeVisible();
    await expect(card.locator("img")).toHaveCount(0);
    await student.reload();
    await expect(card).toBeVisible();
    await card.getByRole("button", { name: "Edit goal", exact: true }).click();
    dialog = student.getByRole("dialog", { name: "Edit goal", exact: true });
    await dialog.getByLabel("My next step", { exact: true }).fill("Do not save");
    await student.keyboard.press("Escape");
    await expect(card).toBeVisible();
    await card.getByRole("button", { name: "Edit goal", exact: true }).click();
    await dialog.getByLabel("My next step", { exact: true }).fill(edited);
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    card = student.getByRole("article", { name: edited, exact: true });
    await expect(card).toBeVisible();
    await card.getByRole("button", { name: "Mark as complete", exact: true }).click();
    await expect(card.locator(".goal-status")).toContainText("Completed");
    await student.getByLabel("Show goals", { exact: true }).selectOption("ACTIVE");
    await expect(card).toHaveCount(0);
    await student.getByLabel("Show goals", { exact: true }).selectOption("COMPLETED");
    await expect(card).toBeVisible();
    await student.reload();
    await expect(card.locator(".goal-status")).toContainText("Completed");
    await card.getByRole("button", { name: "Mark as active", exact: true }).click();
    await expect(card.locator(".goal-status")).toContainText("Active");
    await student.getByRole("button", { name: "Refresh goals", exact: true }).click();
    await expect(card).toBeVisible();
    await student.setViewportSize({ width: 390, height: 844 });
    await expect(student.getByRole("button", { name: "Create goal", exact: true })).toBeVisible();
    expect(await student.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBe(true);
    await student.screenshot({ path: path.join(process.env.UPSELF_GOAL_WORK!, "student-goals-mobile.png"), fullPage: true });
    await student.setViewportSize({ width: 1365, height: 900 });
    await student.screenshot({ path: path.join(process.env.UPSELF_GOAL_WORK!, "student-goals.png"), fullPage: true });
    await card.getByRole("button", { name: "Delete goal", exact: true }).click();
    dialog = student.getByRole("dialog", { name: "Delete goal", exact: true });
    await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(card).toBeVisible();
    await card.getByRole("button", { name: "Delete goal", exact: true }).click();
    await dialog.getByRole("button", { name: "Delete goal", exact: true }).click();
    await expect(card).toHaveCount(0);
    await student.reload();
    await expect(card).toHaveCount(0);
    // A second student cannot read A's private goal, even with its exact ID.
    const other = await page();
    await login(other, "student", process.env.UPSELF_GOAL_OTHER!, process.env.UPSELF_GOAL_OTHER_PASSWORD!);
    await other.getByRole("navigation").getByRole("link", { name: "My goals" }).click();
    await expect(other.getByText("No goals here yet.")).toBeVisible();
    const signed = await request.post("/api/client/login", { data: { username: process.env.UPSELF_GOAL_OTHER, password: process.env.UPSELF_GOAL_OTHER_PASSWORD } });
    const token = (await signed.json()).token;
    const denied = await request.get("/api/client/goals/" + process.env.UPSELF_GOAL_ID, { headers: { Authorization: "Bearer " + token } });
    expect((await denied.json()).code).toBe(404);
    for (const [role, name, pwd] of [
      ["mentor", process.env.UPSELF_GOAL_MENTOR!, process.env.UPSELF_GOAL_MENTOR_PASSWORD!],
      ["admin", "admin01", process.env.UPSELF_GOAL_ADMIN_PASSWORD!],
    ]) {
      const p = await page();
      await login(p, role!, name!, pwd!);
      await expect(p.getByRole("navigation").getByRole("link", { name: "My goals" })).toHaveCount(0);
      await p.goto("/#/student/goals");
      await expect(p).toHaveURL(/\/403$/);
    }
    expect(errors).toEqual([]);
    console.log("PASS browser create/cancel/edit/complete/reopen/filter/refresh/delete, persistence, private notes escaping, mobile and three-role isolation");
  } finally {
    for (const context of contexts) await context.close();
  }
});
