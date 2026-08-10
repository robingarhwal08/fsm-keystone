# Field Service Management Frontend

## Requirements

- **Node.js 24** (Active LTS — matches the `node:24-alpine` build image target; see [ADR-0008](../docs/adr/0008-frontend-dependency-pinning-and-node-lts.md))
- npm (bundled with Node.js)

## Pinned Dependencies

All direct dependencies are pinned to explicit versions (no `"latest"` ranges).
Current resolved versions from `package-lock.json`:

| Package | Pinned version |
|---------|---------------|
| react | 19.2.7 |
| react-dom | 19.2.7 |
| vite | 8.1.5 |
| axios | 1.18.1 |
| react-icons | ^5.7.0 |

See [ADR-0008](../docs/adr/0008-frontend-dependency-pinning-and-node-lts.md) for the dependency-pinning decision and
[docs/baseline/proposed-pins.md](../docs/baseline/proposed-pins.md) for the pinning rationale per package.

## Run

Install dependencies using the lockfile (reproducible, no upgrades):

```bash
npm ci
```

Start the development server:

```bash
npm run dev
```

Open the Vite local URL shown in the terminal (typically `http://localhost:5173`).

The backend base URL is configured in `.env` (`VITE_API_BASE_URL`).
Copy `../.env.example` to `../.env` and set the variable before starting.

## Build

```bash
npm run build
```

Production assets are written to `dist/`. The `Dockerfile` uses `node:20-alpine` as the
current build image; the target is `node:24-alpine` per ADR-0008.

## Environment

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | Backend API base URL (default: `http://localhost:8080`) |
