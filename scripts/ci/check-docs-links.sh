#!/usr/bin/env bash
# scripts/ci/check-docs-links.sh
#
# Validate relative Markdown links in docs/ and the root READMEs.
# Exits 1 if any relative link points to a file that does not exist.
#
# Usage:
#   bash scripts/ci/check-docs-links.sh
#
# Requires: bash, grep, find — no additional tools needed.
#
# Forge Shipping integration: wire this as a pre-merge gate step.
#   step: bash scripts/ci/check-docs-links.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# ── File discovery ─────────────────────────────────────────────────────────────

# Collect all Markdown files to scan for links
MARKDOWN_FILES=()
while IFS= read -r -d '' f; do
    MARKDOWN_FILES+=("$f")
done < <(find "$REPO_ROOT/docs" "$REPO_ROOT/README.md" -name "*.md" -print0 2>/dev/null)

# Add root-level READMEs that exist
for extra in "$REPO_ROOT/keystone-backend/README.md" "$REPO_ROOT/frontend/README.md"; do
    [[ -f "$extra" ]] && MARKDOWN_FILES+=("$extra")
done

if [[ ${#MARKDOWN_FILES[@]} -eq 0 ]]; then
    echo "[check-docs-links] No Markdown files found — nothing to check."
    exit 0
fi

echo "[check-docs-links] Scanning ${#MARKDOWN_FILES[@]} Markdown file(s) for relative links..."

# ── Link extraction and validation ───────────────────────────────────────────

broken=0

for file in "${MARKDOWN_FILES[@]}"; do
    file_dir="$(dirname "$file")"

    # Extract Markdown link targets: [text](target) — capture group 1 is the target.
    # Exclude http:// https:// mailto: ftp:// and anchor-only (#...) links.
    while IFS= read -r target; do
        # Strip anchor fragment (#section) from the target
        path_part="${target%%#*}"

        # Skip empty paths (pure anchor links like [text](#section))
        [[ -z "$path_part" ]] && continue

        # Skip absolute URLs
        [[ "$path_part" =~ ^https?:// ]] && continue
        [[ "$path_part" =~ ^mailto: ]] && continue
        [[ "$path_part" =~ ^ftp:// ]] && continue

        # Resolve relative to the file's directory
        resolved="$file_dir/$path_part"
        # Normalise: remove /./  and /../ sequences
        resolved="$(realpath --no-symlinks -q "$resolved" 2>/dev/null || echo "$resolved")"

        if [[ ! -e "$resolved" ]]; then
            echo "[check-docs-links] BROKEN: $file -> $target (resolved: $resolved)"
            broken=$((broken + 1))
        fi
    done < <(grep -oE '\]\(([^)]+)\)' "$file" | sed 's/](\(.*\))/\1/')
done

# ── Result ────────────────────────────────────────────────────────────────────

echo ""
if [[ $broken -eq 0 ]]; then
    echo "[check-docs-links] PASS: all relative links resolve."
    exit 0
else
    echo "[check-docs-links] FAIL: $broken broken relative link(s) found."
    exit 1
fi
