-- Two-tenant fixture seed for FixturesIT and any @Sql-annotated integration test.
-- Produces the identical scenario described in TenantScenario.java:
--   ACME Corp (id 101):  2 sites, 3 work orders, 1 manager, 1 customer user
--   GLOBEX Ltd (id 102): 1 site,  2 work orders, 1 customer user
--   Shared: 1 technician (id 403) assigned across both tenants
--   Parts:  1 normal-stock (id 501, qty=100), 1 low-stock (id 502, qty=1),
--           1 zero-stock (id 503, qty=0)
--
-- IDs start at 101+ to avoid collision with the authorization-matrix seed (ids 1-25).
-- Passwords are BCrypt-format placeholders — not real encoded values.
-- All emails use the example.test domain (synthetic, per PII policy).
-- Idempotent: DELETE removes prior state; explicit IDs avoid sequence conflicts.

-- ── Clean up in FK-safe order ────────────────────────────────────────────────
DELETE FROM time_logs;
DELETE FROM part_usage;
DELETE FROM status_history;
DELETE FROM work_orders;
DELETE FROM parts;
DELETE FROM users;
DELETE FROM sites;
DELETE FROM customers;

-- ── Customers (2) ────────────────────────────────────────────────────────────
INSERT INTO customers (id, name, email, phone, billing_address, created_at, updated_at)
VALUES
  (101, 'ACME Corp',  'acme@example.test',   '555-0101', '101 ACME Way, Springfield, IL 62701',
        '2026-01-15 09:00:00', '2026-01-15 09:00:00'),
  (102, 'GLOBEX Ltd', 'globex@example.test', '555-0102', '102 Globex Avenue, Capital City, IL 62702',
        '2026-01-15 09:00:00', '2026-01-15 09:00:00');

-- ── Sites (3) ─────────────────────────────────────────────────────────────────
INSERT INTO sites (id, site_name, address, city, state, pincode, contact_person, contact_phone, customer_id)
VALUES
  (201, 'ACME HQ',         '10 Main Street',  'Springfield',  'IL', '62701', 'Alice Manager',      '555-2001', 101),
  (202, 'ACME Warehouse',  '20 Park Avenue',  'Shelbyville',  'IL', '62702', 'Bob Supervisor',     '555-2002', 101),
  (203, 'GLOBEX Plant',    '30 Oak Road',     'Capital City', 'IL', '62703', 'Carol Plant Manager','555-2003', 102);

-- ── Users (5) ─────────────────────────────────────────────────────────────────
-- Passwords: BCrypt-format placeholder — not a real encoded value.
-- Test authentication uses JWT tokens from TestJwtFactory, not passwords.
INSERT INTO users (id, full_name, email, password, phone, role, active, created_at, customer_id)
VALUES
  (401, 'Scenario Manager',      'scenario.manager@example.test',      '$2a$10$test.fixture.password.placeholder.not.for.production.00', '555-4001', 'MANAGER',    TRUE, '2026-01-15 09:00:00', NULL),
  (402, 'Scenario Dispatcher',   'scenario.dispatcher@example.test',   '$2a$10$test.fixture.password.placeholder.not.for.production.00', '555-4002', 'DISPATCHER', TRUE, '2026-01-15 09:00:00', NULL),
  (403, 'Scenario Technician',   'scenario.technician@example.test',   '$2a$10$test.fixture.password.placeholder.not.for.production.00', '555-4003', 'TECHNICIAN', TRUE, '2026-01-15 09:00:00', NULL),
  (404, 'Scenario ACME User',    'scenario.acme.user@example.test',    '$2a$10$test.fixture.password.placeholder.not.for.production.00', '555-4004', 'CUSTOMER',   TRUE, '2026-01-15 09:00:00', 101),
  (405, 'Scenario GLOBEX User',  'scenario.globex.user@example.test',  '$2a$10$test.fixture.password.placeholder.not.for.production.00', '555-4005', 'CUSTOMER',   TRUE, '2026-01-15 09:00:00', 102);

-- ── Parts (3) ─────────────────────────────────────────────────────────────────
INSERT INTO parts (id, part_name, part_number, description, unit_price, stock_quantity, active)
VALUES
  (501, 'HVAC Filter',      'HVAC-FILTER-01', 'Standard HVAC air filter 16x20x1',              12.99, 100, TRUE),
  (502, 'Pump Seal Kit',    'PUMP-SEAL-01',   'Replacement seal kit for centrifugal pumps',    89.50,   1, TRUE),
  (503, 'Compressor Valve', 'COMP-VALVE-01',  'High-pressure compressor valve — out of stock', 145.00,  0, TRUE);

-- ── Work Orders (5) ──────────────────────────────────────────────────────────
-- ACME WO-1: CREATED, no technician (dispatcher unassigned list)
-- ACME WO-2: ASSIGNED to shared technician
-- ACME WO-3: IN_PROGRESS with shared technician
-- GLOBEX WO-1: CREATED, no technician
-- GLOBEX WO-2: IN_PROGRESS with shared technician (cross-tenant assignment)
INSERT INTO work_orders (
  id, work_order_number, title, description, status, priority,
  scheduled_start, scheduled_end, actual_start, actual_end,
  created_at, updated_at,
  customer_id, site_id, created_by_user_id, assigned_technician_id
) VALUES
  (301, 'WO-ACME-0001', 'ACME HVAC Repair',
        'HVAC system not cooling properly in building A',
        'CREATED', 'HIGH',
        '2026-01-15 09:00:00', '2026-01-15 17:00:00', NULL, NULL,
        '2026-01-15 09:00:00', '2026-01-15 09:00:00',
        101, 201, 401, NULL),

  (302, 'WO-ACME-0002', 'ACME Pump Replacement',
        'Replace failing pump in warehouse B',
        'ASSIGNED', 'CRITICAL',
        '2026-01-15 09:00:00', '2026-01-15 17:00:00', NULL, NULL,
        '2026-01-15 09:00:00', '2026-01-15 09:00:00',
        101, 202, 401, 403),

  (303, 'WO-ACME-0003', 'ACME Electrical Inspection',
        'Annual electrical safety inspection',
        'IN_PROGRESS', 'MEDIUM',
        '2026-01-15 09:00:00', '2026-01-15 17:00:00', '2026-01-15 09:00:00', NULL,
        '2026-01-15 09:00:00', '2026-01-15 09:00:00',
        101, 201, 401, 403),

  (304, 'WO-GLOBEX-0001', 'GLOBEX Assembly Line Repair',
        'Conveyor belt broken on line 3',
        'CREATED', 'CRITICAL',
        '2026-01-15 09:00:00', '2026-01-15 17:00:00', NULL, NULL,
        '2026-01-15 09:00:00', '2026-01-15 09:00:00',
        102, 203, 401, NULL),

  (305, 'WO-GLOBEX-0002', 'GLOBEX CNC Calibration',
        'Monthly CNC machine calibration PM service',
        'IN_PROGRESS', 'HIGH',
        '2026-01-15 09:00:00', '2026-01-15 17:00:00', '2026-01-15 09:00:00', NULL,
        '2026-01-15 09:00:00', '2026-01-15 09:00:00',
        102, 203, 401, 403);

-- ── Reset sequences so auto-generated IDs start above the fixture range ───────
DO $$
BEGIN
  PERFORM setval(pg_get_serial_sequence('customers',   'id'), (SELECT COALESCE(MAX(id), 100) FROM customers)   + 1);
  PERFORM setval(pg_get_serial_sequence('sites',       'id'), (SELECT COALESCE(MAX(id), 200) FROM sites)       + 1);
  PERFORM setval(pg_get_serial_sequence('users',       'id'), (SELECT COALESCE(MAX(id), 400) FROM users)       + 1);
  PERFORM setval(pg_get_serial_sequence('parts',       'id'), (SELECT COALESCE(MAX(id), 500) FROM parts)       + 1);
  PERFORM setval(pg_get_serial_sequence('work_orders', 'id'), (SELECT COALESCE(MAX(id), 300) FROM work_orders) + 1);
EXCEPTION WHEN OTHERS THEN
  -- pg_get_serial_sequence returns NULL for generated-always columns on some Hibernate DDL variants;
  -- sequence reset is best-effort and safe to skip in test-container contexts.
  NULL;
END;
$$;
