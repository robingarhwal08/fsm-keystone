/**
 * CHARACTERIZATION TESTS — Dashboard.jsx
 *
 * Pins today's client-side filtering behaviour for CUSTOMER, TECHNICIAN, and
 * MANAGER/DISPATCHER roles. The key defects documented here are:
 *
 * 1. CUSTOMER: Dashboard fetches the full unpaginated work-order and site
 *    collections (/work-orders, /sites with NO page or size params) and
 *    filters client-side by customerId. Target: server-side scoped endpoints.
 *
 * 2. TECHNICIAN: Dashboard fetches the full unpaginated work-order collection
 *    and filters by assignedTechnician.id in the browser.
 *    Target: server-side scoped endpoint.
 */
import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/msw/server';
import Dashboard from '../Dashboard';

// ACME customer user — customerId matches the 3 ACME work orders in fixtures.
const acmeCustomerUser = {
  userId: 404,
  customerId: 101,
  role: 'CUSTOMER',
  fullName: 'ACME Customer User',
  email: 'scenario.acme.user@example.test',
  token: 'test-fixture-token-not-a-real-secret'
};

// Scenario technician — id=403, assigned to WOs 302, 303 (ACME) and 305 (GLOBEX).
const technicianUser = {
  userId: 403,
  customerId: null,
  role: 'TECHNICIAN',
  fullName: 'Scenario Technician',
  email: 'scenario.technician@example.test',
  token: 'test-fixture-token-not-a-real-secret'
};

const managerUser = {
  userId: 401,
  customerId: null,
  role: 'MANAGER',
  fullName: 'Scenario Manager',
  email: 'scenario.manager@example.test',
  token: 'test-fixture-token-not-a-real-secret'
};

describe('Dashboard — CUSTOMER branch', () => {
  it('CHARACTERIZATION: requests unscoped /work-orders with no page or size params', async () => {
    // CHARACTERIZATION: today the component calls GET /work-orders with no query params.
    // Target behaviour: GET /work-orders?customerId=101 (server-side scoping).
    let capturedUrl;
    server.use(
      http.get('http://localhost:8080/api/work-orders', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([
          // Two ACME work orders returned (simplified for this URL assertion test)
          {
            id: 301,
            workOrderNumber: 'WO-ACME-0001',
            title: 'ACME HVAC Repair',
            status: 'CREATED',
            priority: 'HIGH',
            customer: { id: 101, name: 'ACME Corp' },
            site: { id: 201, siteName: 'ACME HQ' },
            assignedTechnician: null
          }
        ]);
      })
    );

    render(<Dashboard user={acmeCustomerUser} setPage={() => {}} />);

    await waitFor(() => {
      expect(capturedUrl).toBeDefined();
    });

    const url = new URL(capturedUrl);
    // CHARACTERIZATION: no pagination params — full list fetch.
    expect(url.searchParams.has('page')).toBe(false);
    expect(url.searchParams.has('size')).toBe(false);
    expect(url.searchParams.has('customerId')).toBe(false);
  });

  it('CHARACTERIZATION: renders only ACME stats even though API returned two tenants data', async () => {
    // Fixtures: 5 WOs total (3 ACME + 2 GLOBEX), 3 sites (2 ACME + 1 GLOBEX).
    // ACME work orders: 301 (CREATED), 302 (ASSIGNED), 303 (IN_PROGRESS).
    // Expected stats for customer 101: open=2, inProgress=1, completed=0, sites=2.
    render(<Dashboard user={acmeCustomerUser} setPage={() => {}} />);

    // Wait for async fetch + state update
    await waitFor(() => {
      expect(screen.getByText('Open Requests')).toBeInTheDocument();
    });

    // The component renders stat cards with numeric values.
    // Assert the card values reflect only ACME data, not the combined 5-WO total.
    await waitFor(() => {
      // open = CREATED(301) + ASSIGNED(302) = 2
      const openCard = screen.getByText('Open Requests').closest('.stat-card') ||
        screen.getByText('Open Requests').parentElement;
      expect(openCard).toHaveTextContent('2');
    });
  });

  it('renders CUSTOMER PORTAL heading', async () => {
    render(<Dashboard user={acmeCustomerUser} setPage={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('CUSTOMER PORTAL')).toBeInTheDocument();
    });
  });
});

describe('Dashboard — TECHNICIAN branch', () => {
  it('CHARACTERIZATION: requests unscoped /work-orders and filters client-side by assignedTechnician.id', async () => {
    // CHARACTERIZATION: component fetches ALL work orders and filters by assignedTechnician.id.
    // Target behaviour: server-side endpoint scoped to technician.
    let capturedUrl;
    server.use(
      http.get('http://localhost:8080/api/work-orders', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([
          { id: 302, workOrderNumber: 'WO-ACME-0002', status: 'ASSIGNED', assignedTechnician: { id: 403, fullName: 'Scenario Technician' } },
          { id: 303, workOrderNumber: 'WO-ACME-0003', status: 'IN_PROGRESS', assignedTechnician: { id: 403, fullName: 'Scenario Technician' } },
          { id: 301, workOrderNumber: 'WO-ACME-0001', status: 'CREATED', assignedTechnician: null }
        ]);
      })
    );

    render(<Dashboard user={technicianUser} setPage={() => {}} />);

    await waitFor(() => expect(capturedUrl).toBeDefined());

    const url = new URL(capturedUrl);
    expect(url.searchParams.has('page')).toBe(false);
    expect(url.searchParams.has('technicianId')).toBe(false);
  });

  it('renders TECHNICIAN WORKSPACE heading', async () => {
    render(<Dashboard user={technicianUser} setPage={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('TECHNICIAN WORKSPACE')).toBeInTheDocument();
    });
  });

  it('renders technician stat cards', async () => {
    render(<Dashboard user={technicianUser} setPage={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('My Jobs')).toBeInTheDocument();
      expect(screen.getByText('In Progress')).toBeInTheDocument();
      expect(screen.getByText('Completed')).toBeInTheDocument();
      expect(screen.getByText('Hours Logged')).toBeInTheDocument();
    });
  });
});

describe('Dashboard — MANAGER branch', () => {
  it('fetches /dashboard/summary and renders stat cards', async () => {
    render(<Dashboard user={managerUser} setPage={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('FIELD SERVICE CONTROL CENTER')).toBeInTheDocument();
    });
    // stat cards driven by dashboardSummary fixture
    expect(screen.getByText('Total Work Orders')).toBeInTheDocument();
  });

  it('renders Create Work Order button', async () => {
    render(<Dashboard user={managerUser} setPage={() => {}} />);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /create work order/i })).toBeInTheDocument();
    });
  });
});
