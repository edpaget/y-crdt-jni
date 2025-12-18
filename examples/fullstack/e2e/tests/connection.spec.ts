import { test, expect } from '@playwright/test';

test.describe('Frontend Connection', () => {
  test('frontend loads without errors', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toContainText('YHocuspocus Collaborative Editor');
    await expect(page.locator('.connect-form')).toBeVisible();
  });

  test('connect form has required inputs', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#userName')).toBeVisible();
    await expect(page.locator('#documentName')).toBeVisible();
    await expect(page.locator('.connect-button')).toBeVisible();
  });

  test('user can connect to a document', async ({ page }) => {
    await page.goto('/');

    await page.fill('#userName', 'TestUser');
    await page.fill('#documentName', 'test-connection-doc');
    await page.click('.connect-button');

    await expect(page.locator('.editor-wrapper')).toBeVisible();
    await expect(page.locator('.status-indicator.status-connected')).toBeVisible({
      timeout: 10000,
    });
  });

  test('user can disconnect from a document', async ({ page }) => {
    await page.goto('/');

    await page.fill('#userName', 'TestUser');
    await page.fill('#documentName', 'test-disconnect-doc');
    await page.click('.connect-button');

    await expect(page.locator('.status-indicator.status-connected')).toBeVisible({
      timeout: 10000,
    });

    await page.click('.disconnect-button');
    await expect(page.locator('.connect-form')).toBeVisible();
  });
});
