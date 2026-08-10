#!/usr/bin/env node
/**
 * check-unpinned-deps.test.js
 *
 * Tests for the check-unpinned-deps.js script using Node.js built-in assert.
 * Runs offline with no registry access — uses synthetic fixtures only.
 *
 * Usage:  node frontend/test/check-unpinned-deps.test.js
 *
 * Phase 0: realPackageJsonHasKnownViolationCount() asserts exactly
 * KNOWN_VIOLATION_COUNT violations in the live frontend/package.json.
 * When specifiers are pinned, reduce KNOWN_VIOLATION_COUNT accordingly.
 */

'use strict';

const assert = require('assert');
const path = require('path');
const fs = require('fs');

const { isUnbounded, findViolations, formatReport } = require('../scripts/check-unpinned-deps');

const FIXTURE_DIR = path.join(__dirname, 'fixtures');

// ─── Phase 0 known count ─────────────────────────────────────────────────────
// Current frontend/package.json declares 6 packages as "latest":
//   @vitejs/plugin-react, axios, lucide-react, react, react-dom, vite
const KNOWN_VIOLATION_COUNT = 6;

let passed = 0;
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log(`  PASS  ${name}`);
    passed++;
  } catch (err) {
    console.error(`  FAIL  ${name}`);
    console.error(`        ${err.message}`);
    failed++;
  }
}

// ─── isUnbounded unit tests ───────────────────────────────────────────────────
console.log('\nUnit tests for isUnbounded():');

test('flags "latest"', () => assert.strictEqual(isUnbounded('latest'), true));
test('flags "*"', () => assert.strictEqual(isUnbounded('*'), true));
test('flags empty string', () => assert.strictEqual(isUnbounded(''), true));
test('flags null', () => assert.strictEqual(isUnbounded(null), true));
test('flags "x"', () => assert.strictEqual(isUnbounded('x'), true));
test('flags ">=0.0.0"', () => assert.strictEqual(isUnbounded('>=0.0.0'), true));
test('flags ">=0"', () => assert.strictEqual(isUnbounded('>=0'), true));
test('does NOT flag "^5.7.0"', () => assert.strictEqual(isUnbounded('^5.7.0'), false));
test('does NOT flag "~1.0.0"', () => assert.strictEqual(isUnbounded('~1.0.0'), false));
test('does NOT flag "19.2.7" (exact)', () => assert.strictEqual(isUnbounded('19.2.7'), false));
test('does NOT flag ">=1.0.0" (bounded below)', () => assert.strictEqual(isUnbounded('>=1.0.0'), false));

// ─── Synthetic fixture tests ──────────────────────────────────────────────────
console.log('\nSynthetic fixture tests (no registry access):');

test('unpinned-package.json: detects "latest" and "*" violations', () => {
  const pkg = JSON.parse(fs.readFileSync(path.join(FIXTURE_DIR, 'unpinned-package.json'), 'utf8'));
  const violations = findViolations(pkg);

  assert.ok(violations.length > 0,
    `Expected violations in unpinned-package.json but found none`);

  const names = violations.map((v) => v.name);
  assert.ok(names.includes('some-library'),
    `Expected "some-library" (latest) to be flagged`);
  assert.ok(names.includes('another-library'),
    `Expected "another-library" (*) to be flagged`);
  assert.ok(names.includes('dev-tool'),
    `Expected "dev-tool" (latest) to be flagged in devDependencies`);

  // Pinned entries must NOT be flagged
  assert.ok(!names.includes('pinned-library'),
    `"pinned-library" (^2.3.4) must not be flagged`);
  assert.ok(!names.includes('pinned-dev-tool'),
    `"pinned-dev-tool" (3.1.0) must not be flagged`);

  // Violation message must not be empty
  const report = formatReport(violations);
  assert.ok(report.includes('some-library'), `Report must mention "some-library"`);
});

test('pinned-package.json: no violations detected', () => {
  const pkg = JSON.parse(fs.readFileSync(path.join(FIXTURE_DIR, 'pinned-package.json'), 'utf8'));
  const violations = findViolations(pkg);

  assert.strictEqual(violations.length, 0,
    `Expected zero violations in pinned-package.json but found ${violations.length}:\n`
    + formatReport(violations));
});

// ─── Phase 0 real package.json test ──────────────────────────────────────────
console.log('\nPhase 0 baseline test (real frontend/package.json):');

test(`frontend/package.json has exactly ${KNOWN_VIOLATION_COUNT} unbounded specifiers`, () => {
  const pkgPath = path.join(__dirname, '..', 'package.json');
  const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
  const violations = findViolations(pkg);

  assert.strictEqual(violations.length, KNOWN_VIOLATION_COUNT,
    `Default-value guard violation count changed.\n`
    + `Expected ${KNOWN_VIOLATION_COUNT} but found ${violations.length}.\n\n`
    + formatReport(violations)
    + `\n\nIf specifiers were pinned, reduce KNOWN_VIOLATION_COUNT to the new count.`
    + `\nIf new unbounded specifiers were added, pin them before merging.`
  );

  // Verify the six known violators are all present
  const names = violations.map((v) => v.name);
  const expected = ['@vitejs/plugin-react', 'axios', 'lucide-react', 'react', 'react-dom', 'vite'];
  for (const dep of expected) {
    assert.ok(names.includes(dep),
      `Expected "${dep}" to be in the violation list; found: ${names.join(', ')}`);
  }
});

// ─── Summary ─────────────────────────────────────────────────────────────────
console.log(`\n${passed + failed} tests: ${passed} passed, ${failed} failed\n`);
if (failed > 0) process.exit(1);
