/**
 * Tests for axiosConfig.js request interceptor.
 *
 * The interceptor reads localStorage.token and attaches it as
 * Authorization: Bearer {token}. When no token is stored it must
 * omit the header entirely (not send "Bearer null" or "Bearer undefined").
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/msw/server';

const BASE = 'http://localhost:8080/api';

// Import the axios instance. Because it is a singleton the interceptor
// registered in axiosConfig.js persists for all tests in the file.
let api;
beforeEach(async () => {
  // Re-import each test using dynamic import + vi.resetModules is unnecessary
  // here — the interceptor reads localStorage.getItem on every request, so
  // setting/clearing localStorage between tests is sufficient.
  api = (await import('../axiosConfig.js')).default;
});

describe('axiosConfig request interceptor', () => {
  it('attaches Authorization Bearer header when token is in localStorage', async () => {
    let capturedAuth;
    server.use(
      http.get(`${BASE}/work-orders`, ({ request }) => {
        capturedAuth = request.headers.get('authorization');
        return HttpResponse.json([]);
      })
    );

    // Synthetic test-only token — never a realistic production value.
    localStorage.setItem('token', 'test-fixture-token-not-a-real-secret');
    await api.get('/work-orders');

    expect(capturedAuth).toBe('Bearer test-fixture-token-not-a-real-secret');
  });

  it('omits Authorization header entirely when no token in localStorage', async () => {
    let capturedAuth;
    server.use(
      http.get(`${BASE}/work-orders`, ({ request }) => {
        capturedAuth = request.headers.get('authorization');
        return HttpResponse.json([]);
      })
    );

    // localStorage is cleared in setup.js afterEach — explicitly confirm it
    // is absent here to document the test invariant.
    localStorage.removeItem('token');
    await api.get('/work-orders');

    // Must be null (header absent), NOT "Bearer null" or "Bearer undefined".
    expect(capturedAuth).toBeNull();
  });

  it('reads the token value at request time (late binding)', async () => {
    // Token set after the module is imported — interceptor must read it dynamically.
    let capturedAuth;
    server.use(
      http.get(`${BASE}/work-orders`, ({ request }) => {
        capturedAuth = request.headers.get('authorization');
        return HttpResponse.json([]);
      })
    );

    localStorage.setItem('token', 'test-late-bound-token');
    await api.get('/work-orders');

    expect(capturedAuth).toBe('Bearer test-late-bound-token');
  });
});
