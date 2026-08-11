/**
 * MSW v2 request handlers covering every endpoint the SPA calls today.
 *
 * Handlers return the same entity-shaped, unpaginated payloads as the backend
 * produces today (sourced from the WO-007 JSON fixtures). The pagination epic
 * will flip these handlers to return page-envelope responses in a single diff.
 *
 * Base URL matches axiosConfig.js: VITE_API_BASE_URL || "http://localhost:8080/api"
 * The .env.test file sets VITE_API_BASE_URL=http://localhost:8080/api so all test
 * requests target this origin.
 */
import { http, HttpResponse } from 'msw';
import customers from '../fixtures/customers.json';
import sites from '../fixtures/sites.json';
import workOrdersAcme from '../fixtures/work-orders-acme.json';
import workOrdersGlobex from '../fixtures/work-orders-globex.json';
import users from '../fixtures/users.json';
import parts from '../fixtures/parts.json';
import dashboardSummary from '../fixtures/dashboard-summary.json';

const BASE = 'http://localhost:8080/api';

// Combined all-tenants work order list (two tenants — used by client-side filter tests)
const allWorkOrders = [...workOrdersAcme, ...workOrdersGlobex];

// Technicians only
const technicians = users.filter((u) => u.role === 'TECHNICIAN');

export const handlers = [
  // ── Auth ──────────────────────────────────────────────────────────────────
  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const body = await request.json();
    // Return a synthetic MANAGER auth response for any credentials in tests.
    // No real credentials ever appear here — synthetic test-only token.
    return HttpResponse.json({
      token: 'test-fixture-bearer-token-not-a-real-secret',
      userId: 401,
      fullName: 'Scenario Manager',
      email: body.email || 'scenario.manager@example.test',
      role: 'MANAGER',
      customerId: null,
      customerName: null
    });
  }),

  http.post(`${BASE}/auth/signup`, async () => {
    return HttpResponse.json({
      token: 'test-fixture-bearer-token-not-a-real-secret',
      userId: 406,
      fullName: 'New User',
      email: 'new.user@fixture.test',
      role: 'CUSTOMER',
      customerId: null,
      customerName: null
    });
  }),

  // ── Dashboard ─────────────────────────────────────────────────────────────
  http.get(`${BASE}/dashboard/summary`, () => {
    return HttpResponse.json(dashboardSummary);
  }),

  // ── Customers ─────────────────────────────────────────────────────────────
  http.get(`${BASE}/customers`, () => {
    return HttpResponse.json(customers);
  }),

  http.post(`${BASE}/customers`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 999, ...body });
  }),

  http.get(`${BASE}/customers/:id`, ({ params }) => {
    const customer = customers.find((c) => c.id === Number(params.id));
    return customer
      ? HttpResponse.json(customer)
      : new HttpResponse(null, { status: 404 });
  }),

  http.put(`${BASE}/customers/:id`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: Number(params.id), ...body });
  }),

  http.delete(`${BASE}/customers/:id`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // ── Sites ─────────────────────────────────────────────────────────────────
  http.get(`${BASE}/sites`, () => {
    return HttpResponse.json(sites);
  }),

  http.post(`${BASE}/sites`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 999, ...body });
  }),

  http.get(`${BASE}/sites/customer/:customerId`, ({ params }) => {
    const filtered = sites.filter(
      (s) => s.customer?.id === Number(params.customerId)
    );
    return HttpResponse.json(filtered);
  }),

  http.put(`${BASE}/sites/:id`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: Number(params.id), ...body });
  }),

  http.delete(`${BASE}/sites/:id`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // ── Work Orders ───────────────────────────────────────────────────────────
  http.get(`${BASE}/work-orders`, () => {
    // Returns the full two-tenant unpaginated list.
    // CHARACTERIZATION: no page/size parameters — client filters in the browser.
    // The pagination epic will change this to return a page envelope.
    return HttpResponse.json(allWorkOrders);
  }),

  http.post(`${BASE}/work-orders`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      id: 999,
      workOrderNumber: 'WO-TEST-0001',
      status: 'CREATED',
      ...body
    });
  }),

  http.get(`${BASE}/work-orders/:id`, ({ params }) => {
    const wo = allWorkOrders.find((w) => w.id === Number(params.id));
    return wo
      ? HttpResponse.json(wo)
      : new HttpResponse(null, { status: 404 });
  }),

  http.patch(`${BASE}/work-orders/:id/assign`, async ({ params, request }) => {
    const body = await request.json();
    const wo = allWorkOrders.find((w) => w.id === Number(params.id));
    return HttpResponse.json({ ...wo, assignedTechnicianId: body.technicianId });
  }),

  http.patch(`${BASE}/work-orders/:id/status`, async ({ params, request }) => {
    const body = await request.json();
    const wo = allWorkOrders.find((w) => w.id === Number(params.id));
    return HttpResponse.json({ ...wo, status: body.status });
  }),

  http.get(`${BASE}/work-orders/:id/history`, () => {
    return HttpResponse.json([]);
  }),

  http.post(`${BASE}/work-orders/:id/parts`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 1, ...body });
  }),

  http.post(`${BASE}/work-orders/:id/time-logs`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 1, ...body });
  }),

  http.put(`${BASE}/work-orders/:id`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: Number(params.id), ...body });
  }),

  http.delete(`${BASE}/work-orders/:id`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // ── Parts ─────────────────────────────────────────────────────────────────
  http.get(`${BASE}/parts`, () => {
    return HttpResponse.json(parts);
  }),

  http.post(`${BASE}/parts`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 999, ...body });
  }),

  http.put(`${BASE}/parts/:id`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: Number(params.id), ...body });
  }),

  http.delete(`${BASE}/parts/:id`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // ── Part Usage ────────────────────────────────────────────────────────────
  http.post(`${BASE}/part-usage`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 1, ...body });
  }),

  // ── Time Logs ─────────────────────────────────────────────────────────────
  http.post(`${BASE}/time-logs`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: 1, ...body });
  }),

  // ── Users ─────────────────────────────────────────────────────────────────
  http.get(`${BASE}/users`, () => {
    return HttpResponse.json(users);
  }),

  http.get(`${BASE}/users/technicians`, () => {
    return HttpResponse.json(technicians);
  }),

  http.put(`${BASE}/users/:id`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({ id: Number(params.id), ...body });
  }),

  http.delete(`${BASE}/users/:id`, () => {
    return new HttpResponse(null, { status: 204 });
  })
];
