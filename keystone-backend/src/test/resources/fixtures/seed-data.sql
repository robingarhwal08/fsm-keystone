-- seed-data.sql
-- Deterministic multi-tenant fixture for @DataJpaTest and @SpringBootTest integration tests.
-- Satisfies WO-025 AC 9: 2 customers, 4 users (one per role), 2 sites, 3 work orders,
-- 1 time log, 1 part usage.
--
-- IDs start at 601+ to avoid collision with:
--   seed-two-tenants.sql (101-599)
--   db/test-seed.sql (1-25)
--
-- All emails use the example.test domain (synthetic, per PII policy).
-- Passwords are BCrypt-format placeholders — not valid encoded values.
-- Tests use JWT tokens from TestJwtFactory, not password authentication.
--
-- Idempotent: DELETE in FK-safe order before INSERT.

-- ── Clean up in FK-safe order ────────────────────────────────────────────────
DELETE FROM part_usage   WHERE id >= 901 AND id <= 950;
DELETE FROM time_logs    WHERE id >= 851 AND id <= 900;
DELETE FROM status_history WHERE work_order_id IN (SELECT id FROM work_orders WHERE id >= 801 AND id <= 850);
DELETE FROM work_orders  WHERE id >= 801 AND id <= 850;
DELETE FROM parts        WHERE id >= 751 AND id <= 800;
DELETE FROM users        WHERE id >= 651 AND id <= 700;
DELETE FROM sites        WHERE id >= 701 AND id <= 750;
DELETE FROM customers    WHERE id >= 601 AND id <= 650;

-- ── 2 Customers ──────────────────────────────────────────────────────────────
INSERT INTO customers (id, name, email, phone, billing_address, created_at, updated_at)
VALUES
  (601, 'Alpha Industries',  'alpha@example.test',  '555-6001', '1 Alpha Way, Springfield, IL 62701',
        '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
  (602, 'Beta Systems Ltd',  'beta@example.test',   '555-6002', '2 Beta Road, Shelbyville, IL 62702',
        '2026-03-01 08:00:00', '2026-03-01 08:00:00');

-- ── 2 Sites ──────────────────────────────────────────────────────────────────
INSERT INTO sites (id, site_name, address, city, state, pincode, contact_person, contact_phone, customer_id)
VALUES
  (701, 'Alpha HQ',       '10 Alpha Street', 'Springfield',  'IL', '62701', 'Alice Contact',   '555-7001', 601),
  (702, 'Beta Main Plant','20 Beta Avenue',  'Shelbyville',  'IL', '62702', 'Bob Site Manager','555-7002', 602);

-- ── 4 Users (one per role) ────────────────────────────────────────────────────
-- Passwords are BCrypt-format placeholders; tests use JWT tokens.
INSERT INTO users (id, full_name, email, password, phone, role, active, created_at, customer_id)
VALUES
  (651, 'Seed Manager',     'seed.manager@example.test',     '$2a$10$seed.fixture.password.placeholder.not.for.production.00',
        '555-6510', 'MANAGER',    TRUE, '2026-03-01 08:00:00', NULL),
  (652, 'Seed Dispatcher',  'seed.dispatcher@example.test',  '$2a$10$seed.fixture.password.placeholder.not.for.production.00',
        '555-6520', 'DISPATCHER', TRUE, '2026-03-01 08:00:00', NULL),
  (653, 'Seed Technician',  'seed.technician@example.test',  '$2a$10$seed.fixture.password.placeholder.not.for.production.00',
        '555-6530', 'TECHNICIAN', TRUE, '2026-03-01 08:00:00', NULL),
  (654, 'Seed Customer',    'seed.customer@example.test',    '$2a$10$seed.fixture.password.placeholder.not.for.production.00',
        '555-6540', 'CUSTOMER',   TRUE, '2026-03-01 08:00:00', 601);

-- ── 2 Parts ───────────────────────────────────────────────────────────────────
INSERT INTO parts (id, part_name, part_number, description, unit_price, stock_quantity, active)
VALUES
  (751, 'Bearing Kit',  'SEED-BEARING-01', 'Standard roller bearing replacement kit', 34.99, 50, TRUE),
  (752, 'Gasket Set',   'SEED-GASKET-01',  'Engine gasket set for industrial pump',   22.50,  5, TRUE);

-- ── 3 Work Orders ─────────────────────────────────────────────────────────────
INSERT INTO work_orders (
    id, work_order_number, title, description, status, priority,
    scheduled_start, scheduled_end, actual_start, actual_end,
    created_at, updated_at,
    customer_id, site_id, created_by_user_id, assigned_technician_id
) VALUES
  (801, 'WO-SEED-0001', 'Alpha HVAC Inspection',
       'Annual HVAC inspection at Alpha HQ building 1',
       'CREATED', 'MEDIUM',
       '2026-03-10 09:00:00', '2026-03-10 17:00:00', NULL, NULL,
       '2026-03-01 08:00:00', '2026-03-01 08:00:00',
       601, 701, 651, NULL),

  (802, 'WO-SEED-0002', 'Beta Pump Maintenance',
       'Scheduled preventive maintenance on production pump',
       'ASSIGNED', 'HIGH',
       '2026-03-11 09:00:00', '2026-03-11 17:00:00', '2026-03-11 09:15:00', NULL,
       '2026-03-01 08:00:00', '2026-03-02 10:00:00',
       602, 702, 651, 653),

  (803, 'WO-SEED-0003', 'Alpha Electrical Repair',
       'Replace faulty circuit breaker panel in warehouse section B',
       'IN_PROGRESS', 'CRITICAL',
       '2026-03-12 08:00:00', '2026-03-12 16:00:00', '2026-03-12 08:30:00', NULL,
       '2026-03-01 08:00:00', '2026-03-03 09:00:00',
       601, 701, 652, 653);

-- ── 1 Time Log ────────────────────────────────────────────────────────────────
INSERT INTO time_logs (id, start_time, end_time, hours_spent, work_description, work_order_id, technician_id)
VALUES
  (851, '2026-03-11 09:15:00', '2026-03-11 13:15:00', 4.00,
        'Completed pump seal replacement; tested pressure to spec',
        802, 653);

-- ── 1 Part Usage ─────────────────────────────────────────────────────────────
INSERT INTO part_usage (id, quantity_used, unit_price_at_usage, total_cost, used_at, work_order_id, part_id, used_by_user_id)
VALUES
  (901, 2, 34.99, 69.98, '2026-03-11 11:00:00', 802, 751, 653);
