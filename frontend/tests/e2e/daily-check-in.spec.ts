import { test, expect } from "@playwright/test";
import fs from "node:fs";
test("daily check-in and private history", async ({ browser }) => {
  const file = process.env.UPSELF_DAILY_FIXTURE;
  test.skip(!file, "Run local daily verifier");
  const f = JSON.parse(fs.readFileSync(file!, "utf8"));
  for (const name of ["student", "second", "mentor", "admin"]) {
    const context = await browser.newContext();
    try {
      const page = await context.newPage();
      const role = name === "mentor" ? "mentor" : name === "admin" ? "admin" : "student";
      await page.goto("/#/login");
      await page
        .getByLabel("Choose your role", { exact: true })
        .selectOption(role);
      await page
        .getByLabel("Username", { exact: true })
        .fill(f.actors[name].username);
      await page
        .getByLabel("Password", { exact: true })
        .fill(f.actors[name].password);
      await page.getByRole("button", { name: "Sign in", exact: true }).click();
      await expect(page).toHaveURL(new RegExp("/" + role + "/home$"));
      const card = page.locator(".daily-check-in");
      if (name === "mentor" || name === "admin") {
        await expect(card).toHaveCount(0);
        continue;
      }
      await expect(card).toBeVisible();
      if (name === "second") {
        await card
          .getByRole("button", { name: "Check in today", exact: true })
          .click();
        await expect(card).toContainText("1 day in a row");
      } else await expect(card).toContainText("3 days in a row");
      await expect(
        card.getByRole("button", { name: "Checked in today", exact: true }),
      ).toBeDisabled();
      await card.locator("summary").click();
      await expect(card.locator("time").first()).toHaveText(f.today);
      await card
        .getByRole("button", { name: "Refresh check-in", exact: true })
        .click();
      await expect(card).toContainText("Checked in today");
      await page.reload();
      await expect(
        card.getByRole("button", { name: "Checked in today", exact: true }),
      ).toBeDisabled();
      await page.setViewportSize({ width: 320, height: 780 });
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth + 1,
        ),
      ).toBe(true);
    } finally {
      await context.close();
    }
  }
});
