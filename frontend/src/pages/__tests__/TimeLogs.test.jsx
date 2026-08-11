/**
 * CHARACTERIZATION TESTS — TimeLogs.jsx
 *
 * Pins the current payload construction behaviour:
 * - technicianId is read from user.userId (passed as a prop from localStorage-derived user)
 * - endTime is derived from Date.now() at form submit time
 * - startTime is derived from endTime minus the entered hoursWorked × 3600000
 *
 * Uses fake timers fixed to 2026-01-15T10:00:00.000Z so timestamp assertions
 * are deterministic and timezone-independent.
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/msw/server';
import TimeLogs from '../TimeLogs';

const technicianUser = {
  userId: 403,
  customerId: null,
  role: 'TECHNICIAN',
  fullName: 'Scenario Technician',
  email: 'scenario.technician@example.test'
};

// Fixed instant for deterministic timestamp assertions.
const FIXED_NOW = new Date('2026-01-15T10:00:00.000Z');

describe('TimeLogs — payload characterization', () => {
  beforeEach(() => {
    // Fake the Date constructor and Date.now so new Date() === FIXED_NOW.
    vi.useFakeTimers();
    vi.setSystemTime(FIXED_NOW);
    // Mock window.alert to avoid jsdom not-implemented warnings.
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('CHARACTERIZATION: submitted payload uses technicianId from user.userId (not from server)', async () => {
    // CHARACTERIZATION: technicianId comes from user.userId (localStorage-derived).
    // Target behaviour: server derives the actor from the authenticated JWT principal.
    let capturedBody;
    server.use(
      http.post('http://localhost:8080/api/time-logs', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({ id: 1, ...capturedBody });
      })
    );

    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(<TimeLogs user={technicianUser} />);

    // Wait for work-order select options to populate (fetches /work-orders).
    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0002 - ACME Pump Replacement')).toBeInTheDocument();
    });

    // Select work order 302 (assigned to technician 403)
    await user.selectOptions(
      screen.getByRole('combobox'),
      '302'
    );

    // Enter 2 hours worked
    await user.type(screen.getByPlaceholderText('Hours Worked'), '2');

    // Enter work notes
    await user.type(screen.getByPlaceholderText('Work Notes'), 'Replaced pump seals and tested');

    // Submit
    await user.click(screen.getByRole('button', { name: /save time log/i }));

    await waitFor(() => expect(capturedBody).toBeDefined());

    // CHARACTERIZATION: technicianId from user.userId prop (not JWT-derived server actor).
    expect(capturedBody.technicianId).toBe(403);
  });

  it('CHARACTERIZATION: startTime and endTime derived from clock minus entered hours', async () => {
    // Fixed clock: now = 2026-01-15T10:00:00.000Z
    // hoursWorked = 2 → startTime = 2026-01-15T08:00:00.000Z
    // CHARACTERIZATION: timestamps computed client-side from Date.now(). Target: server timestamps.
    let capturedBody;
    server.use(
      http.post('http://localhost:8080/api/time-logs', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({ id: 1, ...capturedBody });
      })
    );

    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(<TimeLogs user={technicianUser} />);

    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0002 - ACME Pump Replacement')).toBeInTheDocument();
    });

    await user.selectOptions(screen.getByRole('combobox'), '302');
    await user.type(screen.getByPlaceholderText('Hours Worked'), '2');
    await user.type(screen.getByPlaceholderText('Work Notes'), 'Calibration complete');
    await user.click(screen.getByRole('button', { name: /save time log/i }));

    await waitFor(() => expect(capturedBody).toBeDefined());

    // endTime: fixed clock at 2026-01-15T10:00:00.000Z
    expect(capturedBody.endTime).toBe('2026-01-15T10:00:00.000Z');
    // startTime: endTime - 2h = 2026-01-15T08:00:00.000Z
    expect(capturedBody.startTime).toBe('2026-01-15T08:00:00.000Z');
    expect(capturedBody.workOrderId).toBe(302);
    expect(capturedBody.workDescription).toBe('Calibration complete');
  });

  it('CHARACTERIZATION: zero hoursWorked produces equal startTime and endTime', async () => {
    // Edge case: entering 0 hours results in startTime === endTime.
    // CHARACTERIZATION: no validation prevents this degenerate input.
    let capturedBody;
    server.use(
      http.post('http://localhost:8080/api/time-logs', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({ id: 1, ...capturedBody });
      })
    );

    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(<TimeLogs user={technicianUser} />);

    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0002 - ACME Pump Replacement')).toBeInTheDocument();
    });

    await user.selectOptions(screen.getByRole('combobox'), '302');
    await user.type(screen.getByPlaceholderText('Hours Worked'), '0');
    await user.click(screen.getByRole('button', { name: /save time log/i }));

    await waitFor(() => expect(capturedBody).toBeDefined());

    // 0 hours: startTime = endTime - 0 = endTime
    expect(capturedBody.startTime).toBe(capturedBody.endTime);
  });

  it('renders work orders assigned to the technician in the select', async () => {
    render(<TimeLogs user={technicianUser} />);

    // Technician 403 is assigned to WOs 302, 303 (ACME) and 305 (GLOBEX)
    await waitFor(() => {
      expect(screen.getByText('WO-ACME-0002 - ACME Pump Replacement')).toBeInTheDocument();
      expect(screen.getByText('WO-ACME-0003 - ACME Electrical Inspection')).toBeInTheDocument();
      expect(screen.getByText('WO-GLOBEX-0002 - GLOBEX CNC Calibration')).toBeInTheDocument();
    });

    // WOs not assigned to this technician must not appear.
    expect(screen.queryByText(/WO-ACME-0001/)).not.toBeInTheDocument();
  });
});
