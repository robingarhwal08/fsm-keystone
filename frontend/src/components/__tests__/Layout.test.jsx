/**
 * Layout.jsx characterization tests.
 *
 * Verifies the nav items rendered per role and documents the known defect:
 * the "myrequests" nav entry rendered for CUSTOMER has no matching case in
 * App.jsx's switch statement. Clicking it renders nothing (falls through to
 * the default Dashboard case), making it a dead navigation target.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import Layout from '../../components/Layout';

const noop = vi.fn();

function renderLayout(user, page = 'dashboard') {
  return render(
    <Layout page={page} setPage={noop} user={user} logout={noop}>
      <div data-testid="content">page content</div>
    </Layout>
  );
}

describe('Layout nav items by role', () => {
  it('renders MANAGER nav with dashboard, customers, sites, workorders, parts, users', () => {
    const user = {
      role: 'MANAGER',
      fullName: 'Scenario Manager',
      email: 'manager@fixture.test'
    };
    renderLayout(user);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Customers')).toBeInTheDocument();
    expect(screen.getByText('Sites')).toBeInTheDocument();
    expect(screen.getByText('Work Orders')).toBeInTheDocument();
    expect(screen.getByText('Inventory')).toBeInTheDocument();
    expect(screen.getByText('Users')).toBeInTheDocument();
    // MANAGER does not see My Jobs, My Requests, or Time Logs
    expect(screen.queryByText('My Jobs')).not.toBeInTheDocument();
    expect(screen.queryByText('My Requests')).not.toBeInTheDocument();
  });

  it('renders DISPATCHER nav with dashboard, sites, workorders', () => {
    const user = {
      role: 'DISPATCHER',
      fullName: 'Scenario Dispatcher',
      email: 'dispatcher@fixture.test'
    };
    renderLayout(user);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Sites')).toBeInTheDocument();
    expect(screen.getByText('Work Orders')).toBeInTheDocument();
    expect(screen.queryByText('Customers')).not.toBeInTheDocument();
    expect(screen.queryByText('Users')).not.toBeInTheDocument();
  });

  it('renders TECHNICIAN nav with dashboard, my jobs, time logs, part usage', () => {
    const user = {
      role: 'TECHNICIAN',
      fullName: 'Scenario Technician',
      email: 'technician@fixture.test'
    };
    renderLayout(user);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('My Jobs')).toBeInTheDocument();
    expect(screen.getByText('Time Logs')).toBeInTheDocument();
    expect(screen.getByText('Part Usage')).toBeInTheDocument();
    expect(screen.queryByText('Customers')).not.toBeInTheDocument();
    expect(screen.queryByText('Work Orders')).not.toBeInTheDocument();
  });

  it('renders CUSTOMER nav with dashboard, create request, and the dead myrequests target', () => {
    const user = {
      role: 'CUSTOMER',
      fullName: 'ACME Customer',
      email: 'customer@fixture.test'
    };
    renderLayout(user);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Create Request')).toBeInTheDocument();

    // CHARACTERIZATION: "My Requests" nav button is rendered for CUSTOMER but
    // App.jsx has no case for "myrequests" in its switch statement. Clicking it
    // sets page="myrequests" which falls through to the default (Dashboard).
    // This dead navigation target will be fixed in the frontend routing work order.
    expect(screen.getByText('My Requests')).toBeInTheDocument();
  });

  it('renders CUSTOMER nav when user is null (undefined role fallback)', () => {
    // User object absent — Layout falls through to the CUSTOMER else branch.
    renderLayout(null);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Create Request')).toBeInTheDocument();
    // Dead myrequests target is present even for the anonymous/unknown user branch.
    expect(screen.getByText('My Requests')).toBeInTheDocument();
  });

  it('shows avatar initial from user fullName', () => {
    const user = {
      role: 'MANAGER',
      fullName: 'Scenario Manager',
      email: 'manager@fixture.test'
    };
    renderLayout(user);
    expect(screen.getByText('S')).toBeInTheDocument(); // first char of fullName
  });

  it('falls back to "U" avatar when fullName is absent', () => {
    renderLayout(null);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('renders children content', () => {
    renderLayout(null);
    expect(screen.getByTestId('content')).toBeInTheDocument();
  });
});
