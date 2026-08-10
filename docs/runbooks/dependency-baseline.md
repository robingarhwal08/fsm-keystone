# Runbook: Dependency Baseline and SBOM Generation

**Purpose:** Regenerate the dependency SBOMs, reproduce the unpinned-dependency check,
and verify the image baseline using the committed reproducible commands.

**Constraints:** No credentials appear in this runbook. Do not commit generated artifacts
that contain credentials or private registry tokens.

---

## 1. Regenerate the Frontend npm SBOM

### Prerequisites
- Node.js ≥ 20.19.0 (or ≥ 22.12.0)
- `npm` available
- Network access (for first-time `@cyclonedx/cyclonedx-npm` install only; the tool can run offline afterward)

### Command

```bash
cd frontend

# Install the CycloneDX npm tool (one-time):
npm install --no-save @cyclonedx/cyclonedx-npm

# Generate the SBOM:
npx @cyclonedx/cyclonedx-npm \
  --output-format json \
  --output-file ../docs/baseline/sbom/frontend-sbom.json \
  --package-lock-only \
  --spec-version 1.4

# Verify the output is non-empty and parseable:
python3 -c "import json; d = json.load(open('../docs/baseline/sbom/frontend-sbom.json')); print('Components:', len(d['components']))"
```

### Failure: optional native packages cause parse errors

Some optional `@rolldown/binding-*` packages may fail SBOM generation on platforms where
their native module is not available. Use the `--ignore-npm-errors` flag:

```bash
npx @cyclonedx/cyclonedx-npm \
  --output-format json \
  --output-file ../docs/baseline/sbom/frontend-sbom.json \
  --package-lock-only \
  --spec-version 1.4 \
  --ignore-npm-errors
```

Do not silently accept a partial SBOM — verify the component count matches the lockfile:
```bash
python3 -c "
import json
lock = json.load(open('package-lock.json'))
sbom = json.load(open('../docs/baseline/sbom/frontend-sbom.json'))
print('Lockfile packages:', len(lock.get('packages',{})) - 1)  # subtract root
print('SBOM components:', len(sbom.get('components',[])))
"
```

If counts diverge by more than the known optional packages (33 optional bindings), investigate
before committing the SBOM.

---

## 2. Regenerate the Backend Maven SBOM

### Prerequisites
- Java 21
- Maven 3.9.x
- Network access (to download CycloneDX Maven plugin on first run)

### Command

```bash
cd keystone-backend

# Generate CycloneDX SBOM (includes all transitive dependencies):
mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
  -DoutputFormat=json \
  -DoutputName=bom \
  -DschemaVersion=1.4

# The plugin writes to target/bom.json; copy to docs/baseline/sbom/:
cp target/bom.json ../docs/baseline/sbom/backend-sbom.json

# Verify:
python3 -c "import json; d = json.load(open('../docs/baseline/sbom/backend-sbom.json')); print('Components:', len(d['components']))"
```

### Generate the full dependency tree (for manual review)

```bash
cd keystone-backend
mvn dependency:tree \
  -DoutputFile=../docs/baseline/sbom/backend-dependency-tree.txt \
  -DoutputType=text
```

### Failure: SBOM generation fails due to unresolvable artifact

If Maven cannot resolve an artifact (e.g., offline environment):
```bash
mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
  -DoutputFormat=json \
  -DoutputName=bom \
  -o   # offline mode — uses local Maven cache only
```

If the artifact is not in the local cache, record the failure in `docs/baseline/sbom/backend-sbom.json`
as a metadata property rather than emitting a partial file:
```json
{"name": "sbom-generation-error", "value": "offline-mode-artifact-missing-<artifact-id>"}
```

---

## 3. Run the Unpinned-Dependency Check

### Check the live frontend/package.json (Phase 0 — reporting only)

```bash
# From repo root:
node frontend/scripts/check-unpinned-deps.js frontend/package.json

# Expected output (Phase 0):
# 6 unbounded specifier(s):
#   [dependencies] "@vitejs/plugin-react": "latest"
#   [dependencies] "axios": "latest"
#   ...
# Phase 0 baseline: 6/6 known violations — OK.
```

### Run the test suite (offline, no registry access)

```bash
node frontend/test/check-unpinned-deps.test.js

# Expected:
# 14 tests: 14 passed, 0 failed
```

### Promoting to a blocking gate (dependency-pinning epic)

When all six `latest` specifiers in `frontend/package.json` are replaced with pinned versions:
1. Update `KNOWN_VIOLATION_COUNT = 0` in `frontend/scripts/check-unpinned-deps.js`
2. Update `KNOWN_VIOLATION_COUNT = 0` in `frontend/test/check-unpinned-deps.test.js`
3. Add the check to CI so a PR that re-introduces `latest` fails the build

---

## 4. Measure Docker Image Build Duration and Size

### Prerequisites
- Docker Desktop or Docker Engine running
- Local checkout of the repository

```bash
# Frontend image
echo "=== Frontend build ===" && \
time docker build --no-cache -t fsm-frontend:baseline ./frontend 2>&1 | tee /tmp/frontend-build.log && \
docker images fsm-frontend:baseline --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

# Backend image
echo "=== Backend build ===" && \
time docker build --no-cache -t fsm-backend:baseline ./keystone-backend 2>&1 | tee /tmp/backend-build.log && \
docker images fsm-backend:baseline --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
```

Record the output in `docs/baseline/image-baseline.md` under "Image Build Measurement".

---

## 5. Verify No Production Manifest Was Modified

After running any of the above commands, confirm no production manifest was changed:

```bash
git diff -- \
  frontend/package.json \
  frontend/Dockerfile \
  keystone-backend/Dockerfile \
  docker-compose.yml \
  keystone-backend/docker-compose.yml

# Must show zero output.
```

---

## 6. Scheduled Re-run

Run the SBOM regeneration and image baseline check:
- When the Spring Boot parent version is bumped
- When any `package.json` dependency is changed
- When a Dockerfile base image tag is changed
- Every 90 days as part of the security review cadence (same calendar as the secret rotation runbook)
