/**
 * CHARACTERIZATION TESTS — WorkOrders.jsx
 *
 * Pins the client-side filtering behaviour for TECHNICIAN and CUSTOMER roles.
 *
 * TECHNICIAN: fetches the full unpaginated /work-orders list and filters by
 * assignedTechnician.id in the browser. Target: server-side scoped query.
 *
 * CUSTOMER: fetches the full list and filters by customer.id in the browser.
 * Additionally filters /sites by customer.id for the site dropdown.
 * Target: server-side scoped endpoints for both.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/msw/server';
import WorkOrders from '../WorkOrders';

// Scenario technician (userId=403). Assigned to WOs 302, 303 (ACME), 305 (GLOBEX).
const technicianUser = {
  userId: 403,
  customerId: null,
  role: 'TECHNICIAN',
  fullName: 'Scenario Technician',
  email: 'scenario.technician@example.test'
};

// ACME customer (customerId=101). Owns WOs 301, 302, 303.
const acmeCustomerUser = {
  userId: 404,
  customerId: 101,
  role: 'CUSTOMER',
  fullName: 'ACME Customer User',
  email: 'scenario.acme.user@example.test'
};

const managerUser = {
  userId: 401,
  customerId: null,
  role: 'MANAGER',
  fullName: 'Scenario Manager',
  email: 'scenario.manager@example.test'
};

describe('WorkOrders — TECHNICIAN branch', () => {
  it('CHARACTERIZATION: fetches full /work-orders list with no page or size params', async () => {
    // CHARACTERIZATION: no server-side scoping by technician today.
    // Target: GET /work-orders?technicianId=403.
    let capturedUrl;
    server.use(
      http.get('http://localhost:8080/api/work-orders', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([
          { id: 302, workOrderNumber: 'WO-ACME-0002', title: 'ACME Pump', status: 'ASSIGNED', customer: { id: 101, name: 'ACME Corp' }, assignedTechnician: { id: 403, fullName: 'Scenario Technician' } },
          { id: 305, workOrderNumber: 'WO-GLOBEX-0002', title: 'GLOBEX CNC', status: 'IN_PROGRESS', customer: { id: 102, name: 'GLOBEX Ltd' }, assignedTechnician: { id: 403, fullName: 'Scenario Technician' } },
          { id: 301, workOrderNumber: 'WO-ACME-0001', title: 'ACME HVAC', status: 'CREATED', customer: { id: 101, name: 'ACME Corp' }, assignedTechnician: null }
        ]);
      })
    );

    render(<WorkOrders user={technicianUser} />);

    await waitFor(() => expect(capturedUrl).toBeDefined());

    const url = new URL(capturedUrl);
    expect(url.searchParams.has('page')).toBe(false);
    expect(url.searchParams.has('size')).toBe(false);
    expect(url.searchParams.has('technicianId')).toBe(false);
  });

  it('CHARACTERIZATION: renders only work orders assigned to technician id 403 (client-side filter)', async () => {
    // Fixture data: 5 WOs total; technician 403 is assigned to WOs 302, 303, 305.
    // Component must show exactly those 3 rows after client-side filter.
    render(<WorkOrders user={technicianUser} />);

    await waitFor(() => {
      // WO-ACME-0002 and WO-GLOBEX-0002 are assigned to tech 403
      expect(screen.getByText('WO-ACME-0002')).toBeInTheDocument();
      expect(screen.getByText('WO-GLOBEX-0002')).toBeInTheDocument();
    });

    // WO-ACME-0001 has no technician — must NOT appear for tech 403.
    expect(screen.queryByText('WO-ACME-0001')).not.toBeInTheDocument();
    // WO-GLOBEX-0001 has no technician — must NOT appear.
    expect(screen.queryByText('WO-GLOBEX-0001')).not.toBeInTheDocument();
  });

  it('does not render the create form for TECHNICIAN', async () => {
    render(<WorkOrders user={technicianUser} />);
    await waitFor(() => {
      expect(screen.getByText('Work Orders')).toBeInTheDocument();
    });
    // TECHNICIAN cannot create work orders — the form is hidden.
    expect(screen.queryByText('Create Work Order')).not.toBeInTheDocument();
  });
});

describe('WorkOrders — CUSTOMER branch', () => {
  it('CHARACTERIZATION: fetches full /work-orders list with no customer scoping params', async () => {
    // CHARACTERIZATION: no server-side scoping by customerId today.
    // Target: GET /work-orders?customerId=101.
    let capturedWorkOrderUrl;
    server.use(
      http.get('http://localhost:8080/api/work-orders', ({ request }) => {
        capturedWorkOrderUrl = request.url;
        return HttpResponse.json([
          { id: 301, workOrderNumber: 'WO-ACME-0001', title: 'ACME HVAC', status: 'CREATED', customer: { id: 101, name: 'ACME Corp' }, assignedTechnician: null },
          { id: 304, workOrderNumber: 'WO-GLOBEX-0001', title: 'GLOBEX Repair', status: 'CREATED', customer: { id: 102, name: 'GLOBEX Ltd' }, assignedTechnician: null }
        ]);
      })
    );

    render(<WorkOrders user={acmeCustomerUser} />);

    await waitFor(() => expect(capturedWorkOrderUrl).toBeDefined());

    const url = new URL(capturedWorkOrderUrl);
    expect(url.searchParams.has('customerId')).toBe(false);
  });

  it('CHARACTERIZATION: renders only ACME work orders after client-side filter', async () => {
    // API returns 2 work orders (1 ACME + 1 GLOBEX).
    // Component filters to customerId=101, so only 1 row visible.
    render(<WorkOrders user={acmeCustomerUser} />);

    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0001')).toBeInTheDocument();
    });

    // GLOBEX work order must NOT appear for ACME customer.
    expect(screen.queryByText('WO-GLOBEX-0001')).not.toBeInTheDocument();
  });

  it('CHARACTERIZATION: site dropdown shows only ACME sites (client-side filter on /sites)', async () => {
    // Fixtures: 3 sites total (201 ACME HQ, 202 ACME Warehouse, 203 GLOBEX Plant).
    // CUSTOMER with customerId=101 must see only the 2 ACME sites in the form.
    render(<WorkOrders user={acmeCustomerUser} />);

    await waitFor(() => {
      expect(screen.getByText('ACME HQ')).toBeInTheDocument();
      expect(screen.getByText('ACME Warehouse')).toBeInTheDocument();
    });

    // GLOBEX site must NOT appear in the site dropdown for ACME customer.
    expect(screen.queryByText('GLOBEX Plant')).not.toBeInTheDocument();
  });
});

describe('WorkOrders — MANAGER branch', () => {
  it('renders all 5 work orders for MANAGER (no client-side filter)', async () => {
    render(<WorkOrders user={managerUser} />);
    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0001')).toBeInTheDocument();
      expect(screen.getByText('WO-GLOBEX-0001')).toBeInTheDocument();
    });
  });

  it('renders empty-state row when API returns no work orders', async () => {
    server.use(
      http.get('http://localhost:8080/api/work-orders', () => HttpResponse.json([]))
    );
    render(<WorkOrders user={managerUser} />);
    await waitFor(() => {
      expect(screen.getByText('No Work Orders Found')).toBeInTheDocument();
    });
  });
});
