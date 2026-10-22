import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test('all remaining management actions, pagination and ownership boundaries', async ({ page, request }) => {
  test.setTimeout(180000);
  const suffix = Date.now().toString(36), prefix = 'UI action check ' + suffix;
  const created: { courses: number[]; users: number[] } = { courses: [], users: [] };
  const work = process.env.UPSELF_TEST_WORK!, media = process.env.UPSELF_MEDIA_DIR!;
  const save = () => fs.writeFileSync(path.join(work, 'e2e-extra.json'), JSON.stringify(created));
  const loginResponse = await request.post('http://127.0.0.1:8080/empLogin', { data: { username: 'admin01', password: process.env.UPSELF_ADMIN01_PASSWORD } });
  const token = (await loginResponse.json()).token;
  async function api(url: string, method = 'GET', data?: unknown) {
    const response = await request.fetch('http://127.0.0.1:8080' + url, { method, data, headers: { Authorization: 'Bearer ' + token } });
    const result = await response.json(); expect(result.code).toBe(200); return result;
  }
  try {
    for (let i = 0; i < 12; i++) {
      created.courses.push((await api('/learning/courses', 'POST', { title: prefix + ' ' + i, description: 'Disposable pagination fixture' })).data); save();
    }
    for (let i = 0; i < 11; i++) {
      created.users.push((await api('/system/user', 'POST', { userName: 'ui_' + suffix + '_' + i, nickName: 'UI mentor ' + i, roleId: 20, password: 'Fixture-Only-' + suffix })).data); save();
    }
    await page.goto('/#/login'); await page.getByLabel('Sign in', { exact: true }).selectOption('admin');
    await page.getByLabel('Username', { exact: true }).fill('admin01'); await page.getByLabel('Password', { exact: true }).fill(process.env.UPSELF_ADMIN01_PASSWORD!);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click(); await expect(page).toHaveURL(/manage\/courses/);
    await page.getByPlaceholder('Search', { exact: true }).fill(prefix); await page.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(page.getByText('Page 1 · 12 items')).toBeVisible();
    await page.getByRole('button', { name: 'Next', exact: true }).click(); await expect(page.getByText('Page 2 · 12 items')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Open course', exact: true })).toHaveCount(2);
    await page.getByRole('button', { name: 'Previous', exact: true }).click(); await expect(page.getByText('Page 1 · 12 items')).toBeVisible();
    await page.getByRole('button', { name: 'Clear', exact: true }).click();
    const cid = created.courses[0]!;
    await page.goto('/#/manage/courses/' + cid);
    await page.getByRole('button', { name: 'Assign mentor', exact: true }).click();
    let dialog = page.getByRole('dialog', { name: 'Assign mentor', exact: true });
    await dialog.getByRole('button', { name: 'Next', exact: true }).click(); await expect(dialog.getByText('Page 2', { exact: false })).toBeVisible();
    await dialog.getByRole('button', { name: 'Previous', exact: true }).click();
    await dialog.getByPlaceholder('Search by name or username').fill('mentor01'); await dialog.getByRole('button', { name: 'Search', exact: true }).click();
    await dialog.getByRole('button', { name: 'Choose an account: mentor01', exact: true }).click(); await expect(dialog).not.toBeVisible();
    await page.getByRole('link', { name: 'Accounts', exact: true }).click();
    await page.getByRole('button', { name: 'Next', exact: true }).click(); await expect(page.getByText('Page 2', { exact: false })).toBeVisible();
    await page.getByRole('button', { name: 'Previous', exact: true }).click();
    const username = 'ui_' + suffix + '_0';
    await page.getByPlaceholder('Username', { exact: true }).fill(username); await page.getByRole('button', { name: 'Search', exact: true }).click();
    await page.getByRole('button', { name: 'Edit', exact: true }).click(); dialog = page.getByRole('dialog', { name: 'Edit', exact: true });
    await dialog.getByLabel('Display name').fill('Edited UI mentor'); await dialog.getByLabel('Status', { exact: true }).selectOption('1');
    await dialog.getByRole('button', { name: 'Save account', exact: true }).click(); await expect(dialog).not.toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: username })).toContainText('Disabled');
    console.log('PASS real course/account pagination, role-candidate second page and account edit/status');
    await page.getByRole('button', { name: 'Sign out', exact: true }).click();
    await page.getByLabel('Sign in', { exact: true }).selectOption('admin'); await page.getByLabel('Username', { exact: true }).fill('mentor01'); await page.getByLabel('Password', { exact: true }).fill(process.env.UPSELF_MENTOR01_PASSWORD!); await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page).toHaveURL(/manage\/courses/);
    await page.goto('/#/manage/courses/' + cid);
    await page.getByLabel('Chapter title', { exact: true }).fill('Temporary chapter'); await page.getByRole('button', { name: 'Add chapter', exact: true }).click();
    await expect(page.getByLabel('Text', { exact: true })).toBeVisible();
    await page.getByLabel('Text', { exact: true }).fill('Unsaved change'); await page.getByRole('link', { name: 'Back to courses' }).click();
    await page.getByRole('button', { name: 'Cancel', exact: true }).click(); await expect(page.getByLabel('Text', { exact: true })).toHaveValue('Unsaved change');
    await page.getByRole('button', { name: 'Save', exact: true }).click(); await expect(page.getByRole('status')).toContainText('Chapter saved');
    await page.getByLabel('File', { exact: true }).setInputFiles(path.join(media, 'invalid.svg')); await page.getByLabel('Resource title').fill('Invalid upload'); await page.getByLabel('Image description').fill('Invalid fixture'); await page.getByRole('button', { name: 'Upload file', exact: true }).click();
    await expect(page.getByText('Select a supported file for this type.')).toBeVisible();
    await page.getByLabel('File', { exact: true }).setInputFiles(path.join(media, 'lesson.png')); await page.getByLabel('Resource title').fill('Temporary image'); await page.getByRole('button', { name: 'Upload file', exact: true }).click();
    const resource = page.getByRole('listitem').filter({ hasText: 'Temporary image' }); await expect(resource).toBeVisible();
    await resource.getByRole('button', { name: 'Delete', exact: true }).click(); await page.getByRole('button', { name: 'Cancel', exact: true }).click(); await expect(resource).toBeVisible();
    await resource.getByRole('button', { name: 'Delete', exact: true }).click(); await page.getByRole('button', { name: 'Confirm', exact: true }).click(); await expect(resource).toHaveCount(0);
    await page.getByRole('link', { name: 'Back to courses' }).click();
    await page.getByRole('listitem').filter({ hasText: 'Temporary chapter' }).getByRole('link', { name: 'Edit', exact: true }).click(); await expect(page.getByLabel('Chapter title')).toHaveValue('Temporary chapter'); await page.getByRole('link', { name: 'Back to courses' }).click();
    const chapter = page.getByRole('listitem').filter({ hasText: 'Temporary chapter' }); await chapter.getByRole('button', { name: 'Delete', exact: true }).click(); await page.getByRole('button', { name: 'Confirm', exact: true }).click(); await expect(chapter).toHaveCount(0);
    console.log('PASS unsaved guard cancel, chapter edit/delete, unsupported upload and resource delete/cancel');
    await page.getByRole('button', { name: 'Add update', exact: true }).click(); dialog = page.getByRole('dialog', { name: 'Add update', exact: true }); await dialog.getByRole('button', { name: 'Cancel', exact: true }).click();
    await page.getByRole('button', { name: 'Add update', exact: true }).click(); dialog = page.getByRole('dialog', { name: 'Add update', exact: true }); await dialog.getByLabel('Update title').fill('Temporary update'); await dialog.getByLabel('Text', { exact: true }).fill('Before editing'); await dialog.getByRole('button', { name: 'Save', exact: true }).click(); await expect(dialog).not.toBeVisible();
    const post = page.locator('.update-card').filter({ hasText: 'Temporary update' }); await post.getByRole('button', { name: 'Edit', exact: true }).click(); dialog = page.getByRole('dialog', { name: 'Edit', exact: true }); await dialog.getByLabel('Text', { exact: true }).fill('After editing'); await dialog.getByLabel('Status', { exact: true }).selectOption('PUBLISHED'); await dialog.getByRole('button', { name: 'Save', exact: true }).click(); await expect(post).toContainText('After editing');
    await post.getByRole('button', { name: 'Take offline', exact: true }).click(); await page.getByRole('button', { name: 'Confirm', exact: true }).click(); await expect(post.locator('.status')).toHaveText('Draft');
    await post.getByRole('button', { name: 'Delete', exact: true }).click(); await page.getByRole('button', { name: 'Confirm', exact: true }).click(); await expect(post).toHaveCount(0);
    console.log('PASS post create-cancel, author edit, publish, offline and delete');
    await page.getByRole('link', { name: 'My account', exact: true }).click(); await expect(page.getByLabel('Current password')).toBeVisible(); await expect(page.getByRole('button', { name: 'Save and sign out' })).toBeVisible();
    await page.getByRole('button', { name: 'Sign out', exact: true }).click();
    await page.getByLabel('Sign in', { exact: true }).selectOption('admin'); await page.getByLabel('Username', { exact: true }).fill('maintenance'); await page.getByLabel('Password', { exact: true }).fill(process.env.UPSELF_MAINTENANCE_PASSWORD!); await page.getByRole('button', { name: 'Sign in', exact: true }).click(); await expect(page).toHaveURL(/manage\/courses/); await expect(page.getByRole('link', { name: 'Accounts', exact: true })).toBeVisible(); await page.getByRole('button', { name: 'Sign out' }).click();
    console.log('PASS maintenance shares the same management layout and signs out');
  } finally { save(); await api('/logout', 'POST').catch(() => {}); }
});
