/**
 * CHARACTERIZATION TESTS — CustomerRequests.jsx (exports CustomerReports component).
 *
 * The component is imported as CustomerRequests in App.jsx and routed to the
 * "requests" case. It fetches the full unpaginated /work-orders and /sites
 * collections and filters client-side by customerId — the same pattern as
 * WorkOrders.jsx.
 *
 * CHARACTERIZATION: client-side filtering. Target: server-side scoped queries.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/msw/server';
import CustomerRequests from '../CustomerRequests';

const acmeCustomerUser = {
  userId: 404,
  customerId: 101,
  role: 'CUSTOMER',
  fullName: 'ACME Customer User',
  email: 'scenario.acme.user@example.test'
};

const noCustomerUser = {
  userId: 406,
  customerId: null,
  role: 'CUSTOMER',
  fullName: 'Unlinked Customer',
  email: 'unlinked@fixture.test'
};

describe('CustomerRequests — client-side filtering characterization', () => {
  it('CHARACTERIZATION: fetches full /work-orders list with no customerId scoping param', async () => {
    // CHARACTERIZATION: GET /work-orders returns all tenants; component filters in browser.
    // Target: GET /work-orders?customerId=101.
    let capturedUrl;
    server.use(
      http.get('http://localhost:8080/api/work-orders', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([]);
      })
    );

    render(<CustomerRequests user={acmeCustomerUser} />);

    await waitFor(() => expect(capturedUrl).toBeDefined());

    const url = new URL(capturedUrl);
    expect(url.searchParams.has('customerId')).toBe(false);
    expect(url.searchParams.has('page')).toBe(false);
  });

  it('CHARACTERIZATION: fetches full /sites list with no customerId scoping param', async () => {
    // CHARACTERIZATION: GET /sites returns all sites; component filters in browser.
    // Target: GET /sites/customer/101.
    let capturedSiteUrl;
    server.use(
      http.get('http://localhost:8080/api/sites', ({ request }) => {
        capturedSiteUrl = request.url;
        return HttpResponse.json([]);
      })
    );

    render(<CustomerRequests user={acmeCustomerUser} />);

    await waitFor(() => expect(capturedSiteUrl).toBeDefined());

    const url = new URL(capturedSiteUrl);
    expect(url.searchParams.has('customerId')).toBe(false);
  });

  it('CHARACTERIZATION: renders only ACME work orders after client-side filter', async () => {
    // Fixtures: 5 WOs (3 ACME + 2 GLOBEX). Only ACME ones visible for customer 101.
    render(<CustomerRequests user={acmeCustomerUser} />);

    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0001')).toBeInTheDocument();
      expect(screen.getByText('WO-ACME-0002')).toBeInTheDocument();
      expect(screen.getByText('WO-ACME-0003')).toBeInTheDocument();
    });

    // GLOBEX rows must not appear for ACME customer.
    expect(screen.queryByText('WO-GLOBEX-0001')).not.toBeInTheDocument();
    expect(screen.queryByText('WO-GLOBEX-0002')).not.toBeInTheDocument();
  });

  it('CHARACTERIZATION: site dropdown shows only ACME sites after client-side filter', async () => {
    // 3 sites in fixture: 2 ACME + 1 GLOBEX. ACME customer sees only ACME sites.
    render(<CustomerRequests user={acmeCustomerUser} />);

    await waitFor(() => {
      expect(screen.getByText('ACME HQ')).toBeInTheDocument();
    });

    expect(screen.queryByText('GLOBEX Plant')).not.toBeInTheDocument();
  });

  it('renders Create Customer Request heading', async () => {
    render(<CustomerRequests user={acmeCustomerUser} />);
    await waitFor(() => {
      expect(screen.getByText('Create Customer Request')).toBeInTheDocument();
    });
  });

  it('renders empty state when no work orders found', async () => {
    server.use(
      http.get('http://localhost:8080/api/work-orders', () => HttpResponse.json([]))
    );
    render(<CustomerRequests user={acmeCustomerUser} />);
    await waitFor(() => {
      expect(screen.getByText('No work order requests found.')).toBeInTheDocument();
    });
  });

  it('renders all sites when user has no customerId (unlinked customer)', async () => {
    // Edge case: user.customerId is null — component falls back to showing all sites.
    render(<CustomerRequests user={noCustomerUser} />);
    await waitFor(() => {
      // All 3 sites visible (ACME HQ, ACME Warehouse, GLOBEX Plant)
      expect(screen.getByText('ACME HQ')).toBeInTheDocument();
      expect(screen.getByText('GLOBEX Plant')).toBeInTheDocument();
    });
  });
});
