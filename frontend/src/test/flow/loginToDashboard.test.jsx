/**
 * Integration flow test: login → dashboard → navigate to work orders → create work order.
 *
 * Renders the full App component so that real routing (page state), localStorage
 * token wiring, and component composition are exercised in a single test.
 * All network calls are intercepted by MSW — no backend is required.
 *
 * MSW login handler returns a synthetic MANAGER user with an obviously synthetic
 * test-only token (never a real credential).
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../msw/server';
import App from '../../App';

// The login handler defined in handlers.js returns this MANAGER user by default.
// Tests that need a different response can use server.use() to override for the call.

describe('Login → Dashboard → WorkOrders integration flow', () => {
  beforeEach(() => {
    // Ensure no residual token causes App to skip the login page.
    localStorage.clear();
  });

  it('renders login page when no token is present in localStorage', () => {
    render(<App />);
    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
  });

  it('transitions to MANAGER dashboard after successful login', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.type(screen.getByPlaceholderText('Email'), 'manager@fixture.test');
    await user.type(screen.getByPlaceholderText('Password'), 'fixture-password-not-real');
    await user.click(screen.getByRole('button', { name: /login/i }));

    // After login, the MANAGER dashboard heading must be visible.
    await waitFor(() => {
      expect(screen.getByText('FIELD SERVICE CONTROL CENTER')).toBeInTheDocument();
    });

    // Token must have been stored in localStorage by the Login component.
    expect(localStorage.getItem('token')).toBe('test-fixture-bearer-token-not-a-real-secret');
  });

  it('navigates to WorkOrders page when Create Work Order button is clicked', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.type(screen.getByPlaceholderText('Email'), 'manager@fixture.test');
    await user.type(screen.getByPlaceholderText('Password'), 'fixture-password-not-real');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('FIELD SERVICE CONTROL CENTER')).toBeInTheDocument();
    });

    // The "Create Work Order" button on the MANAGER dashboard calls setPage('workorders').
    await user.click(screen.getByRole('button', { name: /create work order/i }));

    // WorkOrders page heading
    await waitFor(() => {
      expect(screen.getByText('Work Orders')).toBeInTheDocument();
    });

    // The create form is visible for MANAGER (TECHNICIAN branch hides it)
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Title')).toBeInTheDocument();
    });
  });

  it('submits POST /work-orders with title, customerId, siteId, createdByUserId when form is filled', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('http://localhost:8080/api/work-orders', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({
          id: 999,
          workOrderNumber: 'WO-TEST-0001',
          status: 'CREATED',
          ...capturedBody
        });
      })
    );

    render(<App />);

    // Login first
    await user.type(screen.getByPlaceholderText('Email'), 'manager@fixture.test');
    await user.type(screen.getByPlaceholderText('Password'), 'fixture-password-not-real');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('FIELD SERVICE CONTROL CENTER')).toBeInTheDocument();
    });

    // Navigate to work orders
    await user.click(screen.getByRole('button', { name: /create work order/i }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Title')).toBeInTheDocument();
    });

    // Fill in the create form
    await user.type(screen.getByPlaceholderText('Title'), 'Integration Test Work Order');
    await user.type(screen.getByPlaceholderText('Description'), 'Created by integration test flow');

    // Select ACME Corp (customerId=101)
    await user.selectOptions(
      screen.getByRole('combobox', { name: '' }),
      '101'
    );

    // Select ACME HQ site (siteId=201)
    await waitFor(() => {
      // After customer select, site dropdown should have site options
      expect(screen.getByText('ACME HQ')).toBeInTheDocument();
    });

    // Submit
    await user.click(screen.getByRole('button', { name: /save|create|submit/i }));

    await waitFor(() => expect(capturedBody).toBeDefined());

    expect(capturedBody.title).toBe('Integration Test Work Order');
    // customerId is +f.customerId which coerces the string "101" to number 101
    expect(capturedBody.customerId).toBe(101);
    // createdByUserId comes from user.userId (MANAGER = 401)
    expect(capturedBody.createdByUserId).toBe(401);
  });

  it('shows login error message when credentials are rejected by server', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () => {
        return new HttpResponse(null, { status: 401 });
      })
    );

    const user = userEvent.setup();
    render(<App />);

    await user.type(screen.getByPlaceholderText('Email'), 'wrong@fixture.test');
    await user.type(screen.getByPlaceholderText('Password'), 'wrong-password-not-real');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('Login failed. Check your email/password.')).toBeInTheDocument();
    });

    // Must remain on login page
    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});
