import { test, expect } from '@playwright/test';
import { WebSocket } from 'ws';

test.describe('Server Health', () => {
  test('backend WebSocket server accepts connections', async () => {
    const wsConnected = await new Promise<boolean>((resolve) => {
      const ws = new WebSocket('ws://localhost:1234');
      const timeout = setTimeout(() => {
        ws.close();
        resolve(false);
      }, 5000);

      ws.on('open', () => {
        clearTimeout(timeout);
        ws.close();
        resolve(true);
      });

      ws.on('error', () => {
        clearTimeout(timeout);
        resolve(false);
      });
    });

    expect(wsConnected).toBe(true);
  });
});
