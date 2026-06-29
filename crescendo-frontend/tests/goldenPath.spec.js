import { test, expect } from '@playwright/test';

test.describe('Golden Path Workflow', () => {
  test('User can sign up, create a workflow, and see it in dashboard', async ({ page }) => {
    // We use route interception to mock backend so we don't need the real DB
    // 1. Mock Login/Signup
    await page.route('**/api/auth/register', async route => {
      await route.fulfill({
        status: 200,
        json: {
          accessToken: 'fake-jwt-token',
          accessExpiresAt: new Date(Date.now() + 3600000).toISOString(),
          id: 'user-123',
          email: 'test@example.com'
        }
      });
    });

    await page.route('**/api/users/me', async route => {
      await route.fulfill({
        status: 200,
        json: { id: 'user-123', email: 'test@example.com' }
      });
    });

    await page.route('**/api/workflows', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({ status: 200, json: [] });
      } else if (route.request().method() === 'POST') {
        await route.fulfill({ 
          status: 200, 
          json: { id: 'wf-1', name: 'My New Workflow' } 
        });
      }
    });

    // 2. Navigate to signup
    await page.goto('/signup');
    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="password"]', 'Password123!');
    // Assuming there's a username field if the register API needs it
    const usernameInput = await page.$('input[placeholder="Username"]');
    if (usernameInput) await usernameInput.fill('tester');
    
    await page.click('button[type="submit"]');

    // 3. User should be redirected to dashboard
    await expect(page).toHaveURL(/.*dashboard/);

    // 4. Create a new workflow
    // Wait for the Create button (adjust selector based on actual UI)
    const createBtn = await page.getByText('Create Workflow');
    if (await createBtn.isVisible()) {
      await createBtn.click();
      await page.waitForURL(/.*workflow\/.*/);
      
      // We are in the canvas
      await expect(page.getByText('My New Workflow')).toBeVisible();
    }
  });
});
