/**
 * Global Vitest setup: registers jest-dom matchers, starts/stops the MSW server,
 * and clears localStorage between tests to prevent cross-test leakage.
 *
 * Executed before every test file via vitest.config.js `setupFiles`.
 */
import '@testing-library/jest-dom';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './msw/server';

// Start MSW server before all tests in each suite.
// onUnhandledRequest: 'error' ensures any unmocked API call fails loudly,
// so a component calling an endpoint not covered by handlers cannot pass silently.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Reset any per-test handler overrides after each test.
afterEach(() => {
  server.resetHandlers();
  // Clear localStorage between tests — components read token/user from storage,
  // and stale state from a prior test would corrupt the next test's expectations.
  localStorage.clear();
});

// Shut down the server after all tests in the suite.
afterAll(() => server.close());
