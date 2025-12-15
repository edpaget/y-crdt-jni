import { test, expect, Browser, Page, BrowserContext } from '@playwright/test';

async function connectUser(
  page: Page,
  userName: string,
  documentName: string
): Promise<void> {
  await page.goto('/');
  await page.fill('#userName', userName);
  await page.fill('#documentName', documentName);
  await page.click('.connect-button');
  await expect(page.locator('.status-indicator.status-connected')).toBeVisible({
    timeout: 10000,
  });
}

test.describe('Collaborative Editing', () => {
  test('two users can see each other\'s edits', async ({ browser }) => {
    const documentName = `collab-test-${Date.now()}`;

    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();

      await connectUser(page1, 'Alice', documentName);
      await connectUser(page2, 'Bob', documentName);

      // Alice types
      await page1.locator('.ProseMirror').click();
      await page1.keyboard.type('Hello from Alice');

      // Bob should see Alice's text
      await expect(page2.locator('.ProseMirror')).toContainText('Hello from Alice', {
        timeout: 5000,
      });

      // Bob types
      await page2.locator('.ProseMirror').click();
      await page2.keyboard.press('End');
      await page2.keyboard.type(' and Bob says hi');

      // Alice should see Bob's text
      await expect(page1.locator('.ProseMirror')).toContainText('and Bob says hi', {
        timeout: 5000,
      });
    } finally {
      await context1.close();
      await context2.close();
    }
  });

  test('both users have synced content', async ({ browser }) => {
    const documentName = `sync-test-${Date.now()}`;

    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();

      await connectUser(page1, 'UserA', documentName);
      await connectUser(page2, 'UserB', documentName);

      // User 1 types first
      await page1.locator('.ProseMirror').click();
      await page1.keyboard.type('First');

      // Wait for sync
      await page1.waitForTimeout(500);

      // User 2 types at the end
      await page2.locator('.ProseMirror').click();
      await page2.keyboard.press('End');
      await page2.keyboard.type('Second');

      // Wait for sync
      await page1.waitForTimeout(1000);

      // Both documents should contain both texts
      // Note: ProseMirror text includes cursor labels, so we check paragraphs
      await expect(page1.locator('.ProseMirror p')).toContainText('First');
      await expect(page1.locator('.ProseMirror p')).toContainText('Second');
      await expect(page2.locator('.ProseMirror p')).toContainText('First');
      await expect(page2.locator('.ProseMirror p')).toContainText('Second');
    } finally {
      await context1.close();
      await context2.close();
    }
  });

  test('edits persist when user reconnects', async ({ page }) => {
    const documentName = `persist-test-${Date.now()}`;

    // Connect and add content
    await connectUser(page, 'Tester', documentName);
    await page.locator('.ProseMirror').click();
    await page.keyboard.type('Persistent content');

    // Wait for sync to server
    await page.waitForTimeout(1000);

    // Disconnect
    await page.click('.disconnect-button');
    await expect(page.locator('.connect-form')).toBeVisible();

    // Reconnect to same document
    await connectUser(page, 'Tester', documentName);

    // Content should still be there
    await expect(page.locator('.ProseMirror')).toContainText('Persistent content', {
      timeout: 5000,
    });
  });

  test('new user joining sees existing content', async ({ browser }) => {
    const documentName = `join-test-${Date.now()}`;

    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();

      // First user connects and adds content
      await connectUser(page1, 'FirstUser', documentName);
      await page1.locator('.ProseMirror').click();
      await page1.keyboard.type('Pre-existing content');

      // Wait for sync
      await page1.waitForTimeout(500);

      // Second user joins later
      await connectUser(page2, 'SecondUser', documentName);

      // Second user should see the existing content
      await expect(page2.locator('.ProseMirror')).toContainText('Pre-existing content', {
        timeout: 5000,
      });
    } finally {
      await context1.close();
      await context2.close();
    }
  });
});
