import { test, expect, type BrowserContext } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
test("market task: mentor authoring, student decisions and reflection, readonly records", async ({
  browser,
}) => {
  test.setTimeout(240000);
  const fixture = process.env.UPSELF_MARKET_FIXTURE;
  test.skip(!fixture, "Run verify-market.py --browser");
  const f = JSON.parse(fs.readFileSync(fixture!, "utf8"));
  const contexts: BrowserContext[] = [],
    errors: string[] = [];
  async function login(name: string, role: string) {
    const context = await browser.newContext();
    contexts.push(context);
    const p = await context.newPage();
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
  try {
    const mentor = await login("mentor", "mentor"),
      student = await login("student", "student"),
      second = await login("second", "student"),
      admin = await login("admin", "admin");
    await expect(
      student
        .getByRole("navigation", { name: "Student navigation", exact: true })
        .getByRole("link"),
    ).toHaveCount(5);
    await mentor.goto(`/#/manage/courses/${f.courseId}`);
    await mentor
      .getByRole("link", { name: "Market practice", exact: false })
      .click();
    await expect(mentor).toHaveURL(/mentor\/market\?courseId=/);
    await mentor
      .getByRole("button", { name: "Create market task", exact: true })
      .click();
    let editor = mentor.getByRole("dialog", {
      name: "Create market task",
      exact: true,
    });
    await editor.getByRole("button", { name: "Cancel", exact: true }).click();
    await mentor
      .getByRole("button", { name: "Create market task", exact: true })
      .click();
    editor = mentor.getByRole("dialog", {
      name: "Create market task",
      exact: true,
    });
    await editor
      .getByRole("button", { name: "Save task", exact: true })
      .click();
    await expect(editor.getByRole("alert")).toHaveText(
      "Complete the required fields.",
    );
    const title = f.prefix + "Customer deadline";
    await editor.getByLabel("Task title", { exact: true }).fill(title);
    await editor
      .getByLabel("Learning goal", { exact: true })
      .fill("Consider customer needs.");
    await editor
      .getByLabel("The situation", { exact: true })
      .fill(
        "A customer needs an order on Friday. Your supplier can deliver on Saturday.",
      );
    await editor
      .getByLabel("Decision question", { exact: true })
      .fill("What would you do first?");
    for (const i of [0, 1]) {
      await editor
        .locator(`#market-option-${i}`)
        .fill(i === 0 ? "Contact the customer" : "Wait for the supplier");
      await editor
        .locator(`#market-outcome-${i}`)
        .fill(
          i === 0
            ? "The customer can choose a new date."
            : "The customer may miss their deadline.",
        );
      await editor
        .locator(`#market-why-${i}`)
        .fill(
          i === 0
            ? "Early communication leaves time to decide."
            : "Waiting leaves less time for another plan.",
        );
    }
    await editor
      .getByRole("button", { name: "Add a third response", exact: true })
      .click();
    await expect(editor.locator("#market-option-2")).toBeVisible();
    await editor
      .getByRole("button", { name: "Remove third response", exact: true })
      .click();
    await editor
      .getByRole("button", { name: "Save task", exact: true })
      .click();
    await expect(
      mentor.getByRole("heading", { name: title, exact: true }),
    ).toBeVisible();
    const id = Number(mentor.url().split("/").pop());
    await mentor
      .getByRole("button", { name: "Edit market task", exact: true })
      .click();
    editor = mentor.getByRole("dialog", {
      name: "Edit market task",
      exact: true,
    });
    await editor
      .getByLabel("Learning goal", { exact: true })
      .fill("Consider customer needs and timing.");
    await editor
      .getByRole("button", { name: "Save task", exact: true })
      .click();
    await expect(editor).not.toBeVisible();
    await mentor
      .getByRole("button", { name: "Publish task", exact: true })
      .click();
    await expect(
      mentor.getByRole("button", { name: "Close task", exact: true }),
    ).toBeVisible();
    await student.goto(`/#/student/courses/${f.courseId}`);
    await student
      .getByRole("link", { name: "Market practice", exact: false })
      .click();
    await student
      .locator(".market-task-list li")
      .filter({ hasText: title })
      .getByRole("link", { name: "Open task", exact: true })
      .click();
    await expect(
      student.getByText("The customer can choose a new date.", { exact: true }),
    ).toHaveCount(0);
    await student.setViewportSize({width:320,height:780});
    expect(await student.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);
    await student.screenshot({path:path.join(path.dirname(fixture!),"student-market-choice-mobile.png"),fullPage:true});
    await student
      .getByRole("button", { name: "See what happens", exact: true })
      .click();
    await expect(student.getByRole("alert")).toHaveText(
      "Choose a response first.",
    );
    await student
      .getByRole("radio", { name: "Contact the customer", exact: true })
      .check();
    await student
      .getByRole("button", { name: "See what happens", exact: true })
      .click();
    await expect(student.locator(".market-result")).toContainText(
      "The customer can choose a new date.",
    );
    await student.screenshot({path:path.join(path.dirname(fixture!),"student-market-result-mobile.png"),fullPage:true});
    await student
      .getByLabel("Your reflection (optional)", { exact: true })
      .fill("kill yourself");
    await student
      .getByRole("button", { name: "Save reflection", exact: true })
      .click();
    await expect(student.getByRole("alert")).toContainText("Change hurtful");
    await student
      .getByLabel("Your reflection (optional)", { exact: true })
      .fill("I would contact the customer early.");
    await student
      .getByRole("button", { name: "Save reflection", exact: true })
      .click();
    await expect(student.getByRole("status")).toHaveText("Reflection saved.");
    await student
      .getByRole("button", { name: "Try another response", exact: true })
      .click();
    await student
      .getByRole("radio", { name: "Wait for the supplier", exact: true })
      .check();
    await student
      .getByRole("button", { name: "See what happens", exact: true })
      .click();
    await expect(student.locator(".market-result")).toContainText(
      "The customer may miss their deadline.",
    );
    await student.locator(".market-history summary").click();
    await expect(student.locator(".market-history article")).toHaveCount(2);
    await student
      .locator(".market-history article")
      .filter({ hasText: "First attempt" })
      .getByRole("button", { name: "View attempt", exact: false })
      .click();
    await expect(
      student.getByLabel("Your reflection (optional)", { exact: true }),
    ).toHaveValue("I would contact the customer early.");
    await student.reload();
    await student.locator(".market-history summary").click();
    await expect(student.locator(".market-history article")).toHaveCount(2);
    await second.goto(`/#/student/market/${id}`);
    await second.locator(".market-history summary").click();
    await expect(second.locator(".market-history article")).toHaveCount(0);
    await mentor
      .getByRole("button", { name: "Refresh", exact: true })
      .first()
      .click();
    await expect(
      mentor.getByRole("button", { name: "Edit market task", exact: true }),
    ).toBeDisabled();
    const person = mentor
      .locator(".market-participants > article")
      .filter({ hasText: f.actors.student.username });
    await expect(person).toContainText("Contact the customer");
    await person
      .getByRole("button", {
        name: "View responses: " + f.actors.student.username,
        exact: true,
      })
      .click();
    await expect(mentor.locator(".market-participants")).toContainText(
      "I would contact the customer early.",
    );
    await expect(mentor.locator(".market-participants")).toContainText(
      "Not started",
    );
    await mentor
      .getByRole("button", { name: "Close task", exact: true })
      .click();
    await student
      .getByRole("button", { name: "Refresh", exact: true })
      .first()
      .click();
    await expect(
      student.getByText(
        "This task is closed. You can still read your past attempts.",
        { exact: true },
      ),
    ).toBeVisible();
    await expect(
      student.getByRole("button", { name: "See what happens", exact: true }),
    ).toHaveCount(0);
    await student.locator(".market-history summary").click();
    await expect(student.locator(".market-history article")).toHaveCount(2);
    await mentor
      .getByRole("button", { name: "Copy task", exact: true })
      .click();
    await expect(mentor).not.toHaveURL(new RegExp("/" + id + "$"));
    await expect(
      mentor.getByRole("button", { name: "Edit market task", exact: true }),
    ).toBeEnabled();
    await admin.goto(`/#/mentor/market/${id}`);
    await expect(
      admin.getByRole("button", { name: "Edit market task", exact: true }),
    ).toHaveCount(0);
    await student.setViewportSize({ width: 320, height: 780 });
    expect(
      await student.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true);
    await student.screenshot({
      path: path.join(path.dirname(fixture!), "student-market-mobile.png"),
      fullPage: true,
    });
    await mentor.screenshot({
      path: path.join(path.dirname(fixture!), "mentor-market.png"),
      fullPage: true,
    });
    expect(errors).toEqual([]);
  } finally {
    for (const c of contexts) await c.close();
  }
});
