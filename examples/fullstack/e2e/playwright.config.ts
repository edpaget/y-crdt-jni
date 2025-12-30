import { defineConfig, devices } from '@playwright/test';

// Backend configuration - can be overridden via BACKEND env var
// Options: 'jetty' (default) or 'spring'
const backend = process.env.BACKEND || 'jetty';

// Binding configuration - can be overridden via BINDING env var
// Options: 'jni' (default) or 'panama'
const binding = process.env.BINDING || 'jni';

const backendCommands: Record<string, string> = {
  jetty: `cd ../../.. && ./gradlew :examples:fullstack:backend:run -Pbinding=${binding}`,
  spring: `cd ../../.. && ./gradlew :examples:spring-boot:backend:bootRun -Pbinding=${binding}`,
};

const backendCommand = backendCommands[backend];
if (!backendCommand) {
  throw new Error(`Unknown backend: ${backend}. Use 'jetty' or 'spring'.`);
}

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],
  use: {
    baseURL: 'http://localhost:3001',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: backendCommand,
      port: 1234,
      reuseExistingServer: !process.env.CI,
      timeout: 180000,
      stdout: 'ignore',
      stderr: 'ignore',
    },
    {
      command: 'npm run dev',
      cwd: '../../frontend',
      url: 'http://localhost:3001',
      reuseExistingServer: !process.env.CI,
      timeout: 60000,
    },
  ],
});
