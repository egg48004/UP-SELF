import { test, expect } from "@playwright/test";
import type { BrowserContext, Page } from "@playwright/test";
import path from "node:path";

test("role navigation: five student entries, grouped learning, Aa and management responsibilities", async ({ browser }) => {
  test.setTimeout(180000);
  const contexts: BrowserContext[] = [], errors: string[] = [];
  const screenshots = process.env.UPSELF_SCREENSHOTS!;
  async function login(role: string, username: string) {
    const context = await browser.newContext(); contexts.push(context);
    const page = await context.newPage();
    page.on("pageerror", error => errors.push(error.message));
    await page.goto(role === "internal" ? "/#/internal/login" : "/#/login");
    if (role !== "internal") await page.getByLabel("Choose your role", { exact: true }).selectOption(role);
    await page.getByLabel("Username", { exact: true }).fill(username);
    await page.getByLabel("Password", { exact: true }).fill(process.env["UPSELF_" + username.toUpperCase() + "_PASSWORD"]!);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/" + role + "/home"));
    return page;
  }
  function primary(page: Page, role: string) {
    return page.getByRole("navigation", { name: role + " navigation", exact: true });
  }
  async function settled(page: Page) {
    await expect(page.getByText("Loading…", { exact: true })).toHaveCount(0);
    await expect(page.getByRole("alert")).toHaveCount(0);
  }
  async function visibleNavigation(page: Page) {
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    for (const anchor of await page.locator(".main-nav a, .section-nav a, .reading-settings-link").all()) {
      await expect(anchor).toBeVisible();
      const box = await anchor.boundingBox();
      expect(box!.height).toBeGreaterThanOrEqual(44);
      expect(box!.x).toBeGreaterThanOrEqual(0);
      expect(box!.x + box!.width).toBeLessThanOrEqual(page.viewportSize()!.width + 1);
    }
  }
  try {
    const student = await login("student", "student01");
    const menu = primary(student, "Student");
    await expect(menu.getByRole("link")).toHaveCount(5);
    for (const name of ["Home", "My learning", "Assignments", "My check-in", "My goals"]) await expect(menu.getByRole("link", { name, exact: true })).toBeVisible();
    for (const name of ["Saved courses", "Learning history", "Reading settings"]) await expect(menu.getByRole("link", { name, exact: true })).toHaveCount(0);
    await expect(student.getByRole("heading", { name: "Welcome back", exact: true })).toBeVisible();
    const learning = menu.getByRole("link", { name: "My learning", exact: true });
    await learning.focus(); await student.keyboard.press("Enter");
    await expect(student).toHaveURL(/\/student\/courses$/);
    const sections = student.getByRole("navigation", { name: "My learning sections", exact: true });
    await expect(sections.getByRole("link")).toHaveCount(5);
    await sections.getByRole("link",{name:"Learning stats",exact:true}).click();
    await expect(student.getByRole("heading",{name:"Learning stats",exact:true})).toBeVisible();
    await sections.getByRole("link",{name:"My courses",exact:true}).click();
    await expect(learning).toHaveAttribute("aria-current", "page");
    await expect(sections.getByRole("link", { name: "My courses", exact: true })).toHaveAttribute("aria-current", "page");
    await sections.getByRole("link", { name: "Saved courses", exact: true }).click();
    await expect(student.getByRole("heading", { name: "Saved courses", exact: true })).toBeVisible();
    await expect(sections.getByRole("link", { name: "Saved courses", exact: true })).toHaveAttribute("aria-current", "page");
    await expect(learning).toHaveAttribute("aria-current", "page");
    await settled(student); await student.reload();
    await expect(sections.getByRole("link", { name: "Saved courses", exact: true })).toHaveAttribute("aria-current", "page");
    await sections.getByRole("link", { name: "Learning history", exact: true }).click();
    await expect(student.getByRole("heading", { name: "Learning history" })).toBeVisible();
    await settled(student); await expect(learning).toHaveAttribute("aria-current", "page");
    await student.goBack();
    await expect(student.getByRole("heading", { name: "Saved courses", exact: true })).toBeVisible();
    await sections.getByRole("link", { name: "My courses", exact: true }).click();
    await settled(student);
    const open = student.getByRole("link", { name: /^Open course/ }).first();
    if (await open.count()) {
      await open.click(); await settled(student);
      await expect(learning).toHaveAttribute("aria-current", "page");
      await expect(sections.getByRole("link", { name: "My courses", exact: true })).toHaveAttribute("aria-current", "page");
    }
    await learning.click(); await settled(student);
    await student.screenshot({ path: path.join(screenshots, "navigation-student.png"), fullPage: true });
    const settings = student.getByRole("link", { name: "Reading settings", exact: true });
    await expect(settings).toHaveText("Aa");
    const settingsBox = await settings.boundingBox();
    expect(settingsBox!.x).toBeGreaterThan(student.viewportSize()!.width / 2);
    await settings.focus(); await student.keyboard.press("Enter");
    await expect(student).toHaveURL(/\/preferences$/);
    await student.getByLabel("Text size", { exact: true }).selectOption("extraLarge");
    await student.getByLabel("Contrast", { exact: true }).selectOption("highContrast");
    await student.getByLabel("Reduce motion", { exact: true }).check();
    await learning.click();
    for (const width of [320, 390, 768]) {
      await student.setViewportSize({ width, height: 900 });
      await visibleNavigation(student);
      const aa = await settings.boundingBox();
      const brand = await student.locator(".brand").boundingBox();
      expect(Math.abs(aa!.y - brand!.y)).toBeLessThan(10);
    }
    await student.screenshot({ path: path.join(screenshots, "navigation-student-mobile-accessible.png"), fullPage: true });
    await student.reload();
    await expect(student.locator("html")).toHaveAttribute("data-size", "extraLarge");
    await settings.click(); await student.getByRole("button", { name: "Use platform defaults", exact: true }).click();
    await student.setViewportSize({ width: 390, height: 844 });
    await learning.click(); await settled(student); await visibleNavigation(student);
    await student.screenshot({ path: path.join(screenshots, "navigation-student-mobile.png"), fullPage: true });
    for (const [name, heading] of [["Assignments", "My assignments"], ["My check-in", "A moment for you"], ["My goals", "My goals"], ["Home", "Welcome back"]]) {
      await menu.getByRole("link", { name, exact: true }).click();
      await expect(student.getByRole("heading", { name: heading, exact: true })).toBeVisible();
      await expect(sections).toHaveCount(0); await settled(student);
    }
    await student.goto("/#/manage/accounts"); await expect(student).toHaveURL(/\/403$/);
    const mentor = await login("mentor", "mentor01");
    const teaching = primary(mentor, "Mentor");
    await expect(teaching.getByRole("link")).toHaveCount(4);
    for (const name of ["Home", "Teaching courses", "Assignments", "Students"]) await expect(teaching.getByRole("link", { name, exact: true })).toBeVisible();
    await teaching.getByRole("link", { name: "Students", exact: true }).click();
    const students = mentor.getByRole("navigation", { name: "Student sections", exact: true });
    await expect(students.getByRole("link")).toHaveCount(4);
    await students.getByRole("link", { name: "Student check-ins", exact: true }).click();
    await expect(mentor.getByRole("heading", { name: "Student check-ins", exact: true })).toBeVisible();
    await expect(teaching.getByRole("link", { name: "Students", exact: true })).toHaveAttribute("aria-current", "page");
    await settled(mentor); await mentor.reload();
    await expect(students.getByRole("link", { name: "Student check-ins", exact: true })).toHaveAttribute("aria-current", "page");
    await students.getByRole("link", { name: "Student overview", exact: true }).click();
    await expect(mentor.getByRole("heading", { name: "Student overview", exact: true })).toBeVisible();
    await settled(mentor); await mentor.screenshot({ path: path.join(screenshots, "navigation-mentor.png"), fullPage: true });
    await teaching.getByRole("link", { name: "Assignments", exact: true }).click();
    await expect(mentor.getByRole("heading", { name: "Teaching assignments", exact: true })).toBeVisible();
    await settled(mentor);
    await teaching.getByRole("link", { name: "Teaching courses", exact: true }).click();
    await expect(mentor.getByRole("heading", { name: "Courses", exact: true })).toBeVisible();
    await settled(mentor);
    await teaching.getByRole("link", { name: "Home", exact: true }).click();
    await expect(mentor.getByRole("heading", { name: "Teaching workspace", exact: true })).toBeVisible();
    await mentor.goto("/#/student/saved-courses"); await expect(mentor).toHaveURL(/\/403$/);
    const admin = await login("admin", "admin01");
    const management = primary(admin, "Administrator");
    await expect(management.getByRole("link")).toHaveCount(5);
    for (const [name, heading] of [["Accounts", "Accounts"], ["Course allocation", "Courses"], ["Home", "Administration"]]) {
      await management.getByRole("link", { name, exact: true }).click();
      await expect(admin.getByRole("heading", { name: heading, exact: true })).toBeVisible();
      await settled(admin);
    }
    await admin.screenshot({ path: path.join(screenshots, "navigation-admin.png"), fullPage: true });
    await admin.goto("/#/mentor/students"); await expect(admin).toHaveURL(/\/403$/);
    await admin.goto("/#/login");
    await expect(admin.getByLabel("Choose your role").locator("option")).toHaveCount(3);
    const internal = await login("internal", "maintenance");
    await expect(primary(internal, "Internal").getByRole("link")).toHaveCount(3);
    await expect(internal.getByRole("link", { name: "Saved courses", exact: true })).toHaveCount(0);
    await expect(internal.getByRole("heading", { name: "Internal support workspace", exact: true })).toBeVisible();
    await expect(internal.getByRole("link", { name: "Reading settings", exact: true })).toHaveCount(0);
    expect(errors).toEqual([]);
    console.log("PASS five student entries; grouped learning/mentor students; all role menus; Aa keyboard/settings; 320/390/768px large text; legacy URLs, refresh/back and route isolation");
  } finally {
    for (const context of contexts) {
      for (const page of context.pages()) {
        try {
          const auth = await page.evaluate(() => ({ token: sessionStorage.getItem("upself.token"), channel: sessionStorage.getItem("upself.channel") }));
          if (auth.token) await context.request.post((process.env.UPSELF_TEST_UI_URL || "http://127.0.0.1:5173") + (auth.channel === "admin" ? "/api/logout" : "/api/client/logout"), { headers: { Authorization: "Bearer " + auth.token } });
        } catch { /* Keep the original test result if the browser has already closed. */ }
      }
      await context.close();
    }
  }
});
