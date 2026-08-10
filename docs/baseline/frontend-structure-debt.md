# Frontend Structure Debt Baseline — FSM Keystone

**Baseline date**: 2026-08-10  
**Source directory**: `frontend/src/`  
**Related**: [layer-violation-triage.md](layer-violation-triage.md)

---

## App.jsx useState page-switch

`App.jsx` uses a `useState` string (`page`) as a client-side router. The `renderPage()` function implements a 14-case `switch` statement:

| # | Case key | Rendered component | File |
|---|---|---|---|
| 1 | `"dashboard"` | `<Dashboard user={user} setPage={setPage} />` | `pages/Dashboard.jsx` |
| 2 | `"myworkorders"` | `<MyWorkOrders user={user} />` | `pages/MyWorkOrders.jsx` (pass-through) |
| 3 | `"users"` | `<Users />` | `pages/Users.jsx` |
| 4 | `"timelogs"` | `<TimeLogs user={user} />` | `pages/TimeLogs.jsx` |
| 5 | `"partusage"` | `<PartUsage user={user} />` | `pages/PartUsage.jsx` |
| 6 | `"customers"` | `<Customers />` | `pages/Customers.jsx` |
| 7 | `"sites"` | `<Sites />` | `pages/Sites.jsx` |
| 8 | `"workorders"` | `<WorkOrders user={user} />` | `pages/WorkOrders.jsx` |
| 9 | `"parts"` | `<Parts />` | `pages/Parts.jsx` |
| 10 | `"requests"` | `<CustomerRequests user={user} />` | `pages/CustomerRequests.jsx` (pass-through to WorkOrders) |
| 11 | `"mysites"` | `<CustomerSites />` | `pages/CustomerSites.jsx` (pass-through to Sites) |
| 12 | `"reports"` | `<CustomerReports user={user} />` | `pages/CustomerReports.jsx` (pass-through to WorkOrders) |
| default | — | `<Dashboard />` | Falls back to dashboard |

**Source**: `App.jsx:78-153`

The switch has no URL binding, no history API integration, and no lazy loading. Navigation state is lost on page refresh (falls back to `"login"` if no token, `"dashboard"` if token present).

---

## Dead nav target: myrequests

`Layout.jsx` adds `["myrequests", "My Requests", ClipboardList]` to the CUSTOMER navigation array (`Layout.jsx:97`). The `App.jsx` switch has **no case for `"myrequests"`**. Clicking "My Requests" in the sidebar calls `setPage("myrequests")`, which falls through to the `default` case and renders the Dashboard — silently showing the wrong page with no error.

**Source**: `Layout.jsx:97` (nav id), `App.jsx:78-152` (no matching case)  
**Confirmed defect**: See comment at `Layout.jsx:95-98` added in WO-101.  
**Fix-in**: Frontend routing work order.

---

## Pass-through re-export pages

Three pages are thin wrappers that render another page component with no additional logic:

| File | Exports function | Renders | Serves case |
|---|---|---|---|
| `pages/CustomerReports.jsx` | `CustomerRequests` (mismatched function name) | `<WorkOrders user={user} />` | `case "reports":` |
| `pages/CustomerSites.jsx` | `CustomerSites` | `<Sites />` | `case "mysites":` |
| `pages/MyWorkOrders.jsx` | `MyWorkOrders` | `<WorkOrders user={user} />` | `case "myworkorders":` |

**Source**: `CustomerReports.jsx:1-7`, `CustomerSites.jsx:1-6`, `MyWorkOrders.jsx:1-7`

**Additional naming defect**: `pages/CustomerReports.jsx` exports a function named `CustomerRequests` (not `CustomerReports`). The file and function names are swapped. This is a copy-paste error; neither the file name nor the function name reflects the intended role of the component. The corresponding full-implementation page is in `pages/CustomerRequests.jsx` (which exports `CustomerReports`). The intended mapping is:
- `CustomerRequests.jsx` → customer request submission and listing (the substantive page)
- `CustomerReports.jsx` → alias pointing to WorkOrders (the pass-through)

This naming inversion makes navigation logic opaque and should be corrected in the frontend routing work order.

---

## Duplicated auth helpers: authService.js vs commonService.js

Both service modules export `login` and `signup` functions that call the same API endpoints:

**`frontend/src/services/authService.js`** (lines 1-3):
```js
export const login  = (data) => api.post("/auth/login",  data);
export const signup = (data) => api.post("/auth/signup", data);
```

**`frontend/src/services/commonService.js`** (lines 1-5):
```js
export const login  = (data) => api.post("/auth/login",  data);
export const signup = (data) => api.post("/auth/signup", data);
```

`Login.jsx` and `Signup.jsx` import from `authService`; no page currently imports the duplicate helpers from `commonService`. The duplicates increase maintenance surface: a change to the login endpoint (e.g., adding a CSRF token or adjusting the path) must be applied to both files.

**Fix-in**: Frontend routing / auth cleanup work order. Delete the duplicate definitions from `commonService.js`.

---

## Client-side .filter() calls compensating for unbounded API responses

The following files fetch the full, unscoped server response and discard irrelevant data in the browser. Each `.filter()` call is a cross-tenant data leak: all tenants' data crosses the network before being dropped.

### Dashboard.jsx

| Line | Expression | Data exposed to browser | Notes |
|---|---|---|---|
| 51–55 | `woRes.data.filter(w => w.customer?.id === customerId \|\| w.customerId === customerId)` | Entire `work_orders` table | CUSTOMER branch — scopes work orders to this customer's ID |
| 57–61 | `siteRes.data.filter(s => s.customer?.id === customerId \|\| s.customerId === customerId)` | Entire `sites` table | CUSTOMER branch — scopes sites to this customer's ID |
| 64–68 | `workOrders.filter(w => w.status === "CREATED" \|\| w.status === "ASSIGNED")` | Already-filtered subset | Secondary filter: counts open work orders |
| 70–72 | `workOrders.filter(w => w.status === "IN_PROGRESS")` | Already-filtered subset | Secondary filter: counts in-progress |
| 74–78 | `workOrders.filter(w => w.status === "COMPLETED" \|\| w.status === "CLOSED")` | Already-filtered subset | Secondary filter: counts completed |
| 90–95 | `woRes.data.filter(w => w.assignedTechnician?.id === technicianId \|\| ...)` | Entire `work_orders` table | TECHNICIAN branch — scopes to assigned technician |
| 104–106 | `technicianJobs.filter(w => w.status === "IN_PROGRESS")` | Already-filtered subset | Secondary filter |
| 108–112 | `technicianJobs.filter(w => w.status === "COMPLETED" \|\| w.status === "CLOSED")` | Already-filtered subset | Secondary filter |

**Source**: `Dashboard.jsx:51-112`

### CustomerRequests.jsx (the full-implementation page)

| Line | Expression | Data exposed to browser | Notes |
|---|---|---|---|
| 30–32 | `r.data.filter(s => s.customer?.id === customerId \|\| s.customerId === customerId)` | Entire `sites` table | Scopes site picker to this customer |
| 44–46 | `r.data.filter(w => w.customer?.id === customerId \|\| w.customerId === customerId)` | Entire `work_orders` table | Scopes work-order list to this customer |
| 57 | `r.data.filter(...)` | Entire `work_orders` table | TECHNICIAN branch in same file |
| 66 | `r.data.filter(...)` | Entire `work_orders` table | Secondary technician filter |
| 92 | `r.data.filter(...)` | Already-filtered subset | Tertiary filter |

**Source**: `CustomerRequests.jsx:30-92`

### WorkOrders.jsx

| Line | Expression | Data exposed to browser | Notes |
|---|---|---|---|
| 57 | `r.data.filter(w => w.assignedTechnician?.id === technicianId \|\| ...)` | Entire `work_orders` table | TECHNICIAN view scoping |
| 66 | `r.data.filter(...)` | Already-filtered subset | Secondary technician filter |
| 92 | `r.data.filter(...)` | Already-filtered subset | Tertiary status filter |

**Source**: `WorkOrders.jsx:57-92`

---

## Summary of structural debt

| Category | Count / Severity | Fix-in epic |
|---|---|---|
| `useState` router (no URL binding) | Medium — state lost on refresh, no deep links | Frontend router work order |
| Dead nav target (`myrequests`) | Low — CUSTOMER sees "My Requests" → silently navigates to Dashboard | Frontend routing fix |
| Pass-through re-export pages | Low — unnecessary indirection; `CustomerReports.jsx` has mismatched function name | Frontend routing work order |
| Duplicated auth helpers | Low — dual maintenance surface for login/signup functions | Frontend auth cleanup |
| Client-side `.filter()` calls (14 sites) | **High** — entire work_orders and sites tables sent to browser on every load; cross-tenant data exposure | Tenancy-scoping work order (Phase 3) |

> The client-side `.filter()` calls are the highest-risk item: they are the primary symptom of the missing server-side tenancy scoping. Removing them is the **last step** of the tenancy-scoping work order (after server-side scoping is confirmed correct), not the first.
