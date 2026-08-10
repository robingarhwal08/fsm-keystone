#!/usr/bin/env node
/**
 * check-unpinned-deps.js
 *
 * Parses a package.json and reports dependency specifiers that are unbounded:
 *   - "latest"       — resolves to whatever is current at install time
 *   - "*"            — any version
 *   - ""             — empty (treated as *)
 *   - "x" / "X"     — bare wildcard
 *   - ">=0.0.0"      — effectively unbounded
 *
 * Phase 0 (reporting-only): the check asserts the number of violations equals
 * KNOWN_VIOLATION_COUNT rather than zero, so the build does not break before
 * the specifiers are pinned. Flip to zero and remove this comment when the
 * dependency-pinning epic is complete.
 *
 * Usage:
 *   node frontend/scripts/check-unpinned-deps.js [path/to/package.json]
 *   # defaults to frontend/package.json if no argument given
 *
 * Exit codes:
 *   0 — violation count matches expected (or no violations)
 *   1 — violation count differs from expected, or file unreadable
 */

'use strict';

const fs = require('fs');
const path = require('path');

const UNBOUNDED_EXACT = new Set(['latest', '*', '', 'x', 'X']);

/**
 * Returns true if the specifier is unbounded (not pinned to a specific range).
 */
function isUnbounded(specifier) {
  if (!specifier || UNBOUNDED_EXACT.has(specifier)) return true;
  // ">=0.0.0" or ">=0" — effectively any version
  if (/^>=\s*0(\.\d+)*$/.test(specifier.trim())) return true;
  return false;
}

/**
 * Finds unbounded specifiers in a parsed package.json object.
 * Returns an array of { section, name, specifier } objects.
 */
function findViolations(pkg) {
  const violations = [];
  const sections = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
  for (const section of sections) {
    const deps = pkg[section];
    if (!deps || typeof deps !== 'object') continue;
    for (const [name, specifier] of Object.entries(deps)) {
      if (isUnbounded(specifier)) {
        violations.push({ section, name, specifier: String(specifier) });
      }
    }
  }
  return violations;
}

/**
 * Formats a human-readable report of violations.
 * Never prints the contents of secret values — only package names and specifiers.
 */
function formatReport(violations) {
  if (violations.length === 0) return 'No unbounded specifiers found.';
  const lines = violations.map(
    (v) => `  [${v.section}] "${v.name}": "${v.specifier}"`
  );
  return `${violations.length} unbounded specifier(s):\n${lines.join('\n')}`;
}

// ─── CLI entry point ─────────────────────────────────────────────────────────

if (require.main === module) {
  const pkgPath = process.argv[2] || path.join(__dirname, '..', 'package.json');
  const absPath = path.resolve(pkgPath);

  let pkg;
  try {
    pkg = JSON.parse(fs.readFileSync(absPath, 'utf8'));
  } catch (err) {
    process.stderr.write(`ERROR: Cannot read ${absPath}: ${err.message}\n`);
    process.exit(1);
  }

  const violations = findViolations(pkg);
  const report = formatReport(violations);

  // Phase 0 known count — matches the current frontend/package.json state (6 "latest" deps)
  const KNOWN_VIOLATION_COUNT = 6;

  process.stdout.write(`\n[check-unpinned-deps] ${absPath}\n`);
  process.stdout.write(report + '\n');

  if (violations.length === KNOWN_VIOLATION_COUNT) {
    process.stdout.write(
      `\nPhase 0 baseline: ${violations.length}/${KNOWN_VIOLATION_COUNT} known violations — OK.\n`
      + 'These must be pinned before the dependency-pinning epic gate becomes blocking.\n'
    );
    process.exit(0);
  } else if (violations.length < KNOWN_VIOLATION_COUNT) {
    process.stdout.write(
      `\nViolation count dropped to ${violations.length} (expected ${KNOWN_VIOLATION_COUNT}).\n`
      + 'Some specifiers were pinned — update KNOWN_VIOLATION_COUNT to reflect the new baseline.\n'
    );
    process.exit(0);
  } else {
    process.stderr.write(
      `\nERROR: Violation count increased to ${violations.length} (expected ${KNOWN_VIOLATION_COUNT}).\n`
      + 'A new unbounded specifier was added. Pin it before merging.\n'
    );
    process.exit(1);
  }
}

module.exports = { isUnbounded, findViolations, formatReport };
