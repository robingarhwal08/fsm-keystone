-- Test seed fixture for authorization baseline and payload measurements.
-- Creates: 2 customers, 3 sites, 4 role-distinct users, 25 work orders.
-- Passwords are BCrypt-encoded placeholder values (not real production passwords).
-- This fixture is idempotent when run after a schema truncation.

-- Clean up in FK-safe order
DELETE FROM time_logs;
DELETE FROM part_usage;
DELETE FROM status_history;
DELETE FROM work_orders;
DELETE FROM users;
DELETE FROM sites;
DELETE FROM customers;

-- ─── Customers (2) ──────────────────────────────────────────────────────────
INSERT INTO customers (id, name, email, phone, billing_address, created_at, updated_at)
VALUES
  (1, 'Acme Corp',    'acme@example.test',    '555-0001', '1 Acme Way',   NOW(), NOW()),
  (2, 'Globex Ltd',   'globex@example.test',  '555-0002', '2 Globex Ave',  NOW(), NOW());

-- ─── Sites (3 across both customers) ────────────────────────────────────────
INSERT INTO sites (id, site_name, address, city, state, pincode, customer_id)
VALUES
  (1, 'Acme HQ',         '10 Main St',   'Springfield', 'IL', '62701', 1),
  (2, 'Acme Warehouse',  '20 Park Ave',  'Shelbyville',  'IL', '62702', 1),
  (3, 'Globex Plant',    '30 Oak Rd',    'Capital City', 'IL', '62703', 2);

-- ─── Users (4 — one per role) ────────────────────────────────────────────────
-- BCrypt of "TestPass123!" for all test users (placeholder, not a production secret)
INSERT INTO users (id, full_name, email, password, phone, role, active, created_at, customer_id)
VALUES
  (1, 'Test Manager',    'manager@example.test',    '$2a$10$placeholder.hash.manager.000000000000000000', '555-1001', 'MANAGER',    TRUE, NOW(), NULL),
  (2, 'Test Dispatcher', 'dispatcher@example.test', '$2a$10$placeholder.hash.dispatch.00000000000000000', '555-1002', 'DISPATCHER', TRUE, NOW(), NULL),
  (3, 'Test Technician', 'technician@example.test', '$2a$10$placeholder.hash.technici.00000000000000000', '555-1003', 'TECHNICIAN', TRUE, NOW(), NULL),
  (4, 'Test Customer',   'customer@example.test',   '$2a$10$placeholder.hash.customer.00000000000000000', '555-1004', 'CUSTOMER',   TRUE, NOW(), 1);

-- ─── Work Orders (25 split across both tenants) ──────────────────────────────
INSERT INTO work_orders (id, work_order_number, title, description, status, priority, created_at, updated_at, customer_id, site_id, created_by_user_id, assigned_technician_id)
VALUES
  -- Acme Corp work orders (15)
  ( 1, 'WO-TEST-0001', 'Fix HVAC Unit A',           'HVAC not cooling', 'CREATED',     'HIGH',     NOW(), NOW(), 1, 1, 1, NULL),
  ( 2, 'WO-TEST-0002', 'Replace Water Pump',        'Leaking pump',     'ASSIGNED',    'CRITICAL', NOW(), NOW(), 1, 1, 1, 3),
  ( 3, 'WO-TEST-0003', 'Electrical Inspection',     'Annual check',     'IN_PROGRESS', 'MEDIUM',   NOW(), NOW(), 1, 2, 1, 3),
  ( 4, 'WO-TEST-0004', 'Roof Repair',               'Storm damage',     'CREATED',     'HIGH',     NOW(), NOW(), 1, 2, 2, NULL),
  ( 5, 'WO-TEST-0005', 'Generator Maintenance',     'PM service',       'COMPLETED',   'MEDIUM',   NOW(), NOW(), 1, 1, 1, 3),
  ( 6, 'WO-TEST-0006', 'Network Cabling',           'New wing',         'CREATED',     'LOW',      NOW(), NOW(), 1, 2, 2, NULL),
  ( 7, 'WO-TEST-0007', 'Security Camera Install',   'Lobby cameras',    'ASSIGNED',    'MEDIUM',   NOW(), NOW(), 1, 1, 1, 3),
  ( 8, 'WO-TEST-0008', 'HVAC Filter Replacement',   'Quarterly PM',     'COMPLETED',   'LOW',      NOW(), NOW(), 1, 1, 1, 3),
  ( 9, 'WO-TEST-0009', 'Fire Suppression Test',     'Annual test',      'CREATED',     'HIGH',     NOW(), NOW(), 1, 2, 2, NULL),
  (10, 'WO-TEST-0010', 'Elevator Inspection',       'DOT compliance',   'IN_PROGRESS', 'CRITICAL', NOW(), NOW(), 1, 1, 1, 3),
  (11, 'WO-TEST-0011', 'Parking Lot Lights',        'Bulb replacement', 'CREATED',     'LOW',      NOW(), NOW(), 1, 2, 2, NULL),
  (12, 'WO-TEST-0012', 'Plumbing Inspection',       'Quarterly',        'COMPLETED',   'MEDIUM',   NOW(), NOW(), 1, 1, 1, 3),
  (13, 'WO-TEST-0013', 'Boiler Service',            'Annual PM',        'ASSIGNED',    'HIGH',     NOW(), NOW(), 1, 2, 1, 3),
  (14, 'WO-TEST-0014', 'Compressor Replacement',    'Unit failed',      'CREATED',     'CRITICAL', NOW(), NOW(), 1, 1, 2, NULL),
  (15, 'WO-TEST-0015', 'Landscaping',               'Spring trim',      'COMPLETED',   'LOW',      NOW(), NOW(), 1, 2, 2, NULL),
  -- Globex Ltd work orders (10)
  (16, 'WO-TEST-0016', 'Assembly Line Repair',      'Belt snapped',     'CREATED',     'CRITICAL', NOW(), NOW(), 2, 3, 2, NULL),
  (17, 'WO-TEST-0017', 'CNC Machine Calibration',   'Monthly PM',       'ASSIGNED',    'HIGH',     NOW(), NOW(), 2, 3, 2, 3),
  (18, 'WO-TEST-0018', 'Coolant System Flush',      'Quarterly PM',     'IN_PROGRESS', 'MEDIUM',   NOW(), NOW(), 2, 3, 2, 3),
  (19, 'WO-TEST-0019', 'Robot Arm Maintenance',     'Greasing',         'COMPLETED',   'LOW',      NOW(), NOW(), 2, 3, 2, 3),
  (20, 'WO-TEST-0020', 'Dust Collection Service',   'Filter change',    'CREATED',     'MEDIUM',   NOW(), NOW(), 2, 3, 2, NULL),
  (21, 'WO-TEST-0021', 'Conveyor Belt Replace',     'Worn belt',        'ASSIGNED',    'HIGH',     NOW(), NOW(), 2, 3, 2, 3),
  (22, 'WO-TEST-0022', 'Electrical Panel Upgrade',  'Capacity',         'CREATED',     'CRITICAL', NOW(), NOW(), 2, 3, 2, NULL),
  (23, 'WO-TEST-0023', 'Overhead Crane Inspection', 'Annual',           'COMPLETED',   'HIGH',     NOW(), NOW(), 2, 3, 2, 3),
  (24, 'WO-TEST-0024', 'Compressed Air System',     'Leak repair',      'IN_PROGRESS', 'MEDIUM',   NOW(), NOW(), 2, 3, 2, 3),
  (25, 'WO-TEST-0025', 'Sprinkler System Test',     'Annual test',      'CREATED',     'HIGH',     NOW(), NOW(), 2, 3, 2, NULL);
