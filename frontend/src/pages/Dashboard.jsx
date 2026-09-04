import { useEffect, useState } from "react";
import {
  AlertTriangle,
  Building2,
  CalendarClock,
  ClipboardList,
  Clock3,
  FileText,
  LayoutDashboard,
  MapPin,
  Package,
  Users,
  Wrench
} from "lucide-react";

import {
  dashboardSummary,
  getWorkOrders,
  getSites
} from "../services/commonService";

import StatusBadge from "../components/StatusBadge";
import SlaBadge from "../components/SlaBadge";
import DashboardShell from "../components/DashboardShell";
import {
  BarChart,
  DashboardPanel,
  DonutChart,
  SlaGauge,
  StatCards
} from "../components/DashboardCharts";
import {
  filterSitesForCustomer,
  filterWorkOrdersForCustomer
} from "../utils/customerScope";
import {
  JOB_BREAKDOWN_SLICES,
  STATUS_COLORS,
  STATUS_GRADIENTS,
  WORK_ORDER_MIX,
  sliceWithGradient,
  withStatTheme
} from "../utils/chartPalette";

function buildMixSlices(summary, includeOnHold = false) {
  return WORK_ORDER_MIX.filter((item) => includeOnHold || item.key !== "onHoldWorkOrders").map(
    (item) => ({
      label: item.label,
      value: summary[item.key] || 0,
      color: item.color,
      gradient: item.gradient
    })
  );
}

function WorkOrderTable({ rows, columns, emptyMessage }) {
  if (!rows.length) {
    return (
      <div className="chart-empty-state">
        <p className="chart-empty">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="dash-table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              {columns.map((col) => (
                <td key={col.key}>{col.render(row)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function Dashboard({ user, setPage }) {
  const [s, setS] = useState({});

  const [customerStats, setCustomerStats] = useState({
    total: 0,
    sites: 0,
    byStatus: {},
    byAssignee: {},
    requests: []
  });

  const [technicianStats, setTechnicianStats] = useState({
    jobs: 0,
    inProgress: 0,
    completed: 0,
    hoursLogged: 0,
    recentJobs: []
  });

  useEffect(() => {
    if (!user) {
      return;
    }

    if (user?.role === "MANAGER" || user?.role === "DISPATCHER") {
      dashboardSummary()
        .then((r) => setS(r.data))
        .catch(() => {});
    }

    if (user?.role === "CUSTOMER") {
      Promise.all([getWorkOrders(), getSites()])
        .then(([woRes, siteRes]) => {
          const workOrders = filterWorkOrdersForCustomer(woRes.data || [], user);
          const sites = filterSitesForCustomer(siteRes.data || [], user);

          const byStatus = {};
          const byAssignee = {};
          workOrders.forEach((w) => {
            const status = w.status || "NEW";
            byStatus[status] = (byStatus[status] || 0) + 1;
            const assignee =
              w.assignedTechnician?.fullName ||
              w.assignedTechnician?.name ||
              "Unassigned";
            byAssignee[assignee] = (byAssignee[assignee] || 0) + 1;
          });

          setCustomerStats({
            total: workOrders.length,
            sites: sites.length,
            byStatus,
            byAssignee,
            requests: workOrders
          });
        })
        .catch(() => {});
    }

    if (user?.role === "TECHNICIAN") {
      getWorkOrders()
        .then((woRes) => {
          const technicianId = user.userId;
          const technicianJobs = (woRes.data || []).filter(
            (w) =>
              w.assignedTechnician?.id === technicianId ||
              w.assignedTechnicianId === technicianId ||
              w.technicianId === technicianId
          );

          setTechnicianStats({
            jobs: technicianJobs.length,
            inProgress: technicianJobs.filter((w) => w.status === "IN_PROGRESS").length,
            completed: technicianJobs.filter(
              (w) => w.status === "COMPLETED" || w.status === "CLOSED"
            ).length,
            hoursLogged:
              technicianJobs.reduce((sum, w) => sum + (w.totalMinutes || 0), 0) / 60,
            recentJobs: technicianJobs.slice(0, 8)
          });
        })
        .catch(() => {});
    }
  }, [user]);

  const role = user?.role;
  const firstName = user?.fullName?.split(" ")?.[0] || "there";

  if (role === "TECHNICIAN") {
    const pendingJobs = Math.max(
      0,
      technicianStats.jobs - technicianStats.inProgress - technicianStats.completed
    );

    return (
      <DashboardShell
        themeKey="manager"
        eyebrow="Technician workspace"
        title={`Good to see you, ${firstName}`}
        subtitle="Track assigned jobs, update progress, and log time from one clear view."
        icon={Wrench}
        action={
          <button className="dash-btn" type="button" onClick={() => setPage("workorders")}>
            View my jobs
          </button>
        }
      >
        <StatCards
          items={withStatTheme([
            {
              label: "Assigned jobs",
              value: technicianStats.jobs,
              icon: <ClipboardList size={20} />,
              tone: "accent"
            },
            {
              label: "In progress",
              value: technicianStats.inProgress,
              icon: <Wrench size={20} />
            },
            {
              label: "Completed",
              value: technicianStats.completed,
              icon: <FileText size={20} />
            },
            {
              label: "Hours logged",
              value: Number(technicianStats.hoursLogged || 0).toFixed(1),
              icon: <Clock3 size={20} />,
              hint: "Across assigned work orders"
            }
          ])}
        />

        <div className="chart-grid">
          <section className="panel dash-panel">
            <DonutChart
              title="Job breakdown"
              subtitle="Where your workload stands today"
              slices={[
                sliceWithGradient(JOB_BREAKDOWN_SLICES.inProgress, technicianStats.inProgress),
                sliceWithGradient(JOB_BREAKDOWN_SLICES.completed, technicianStats.completed),
                sliceWithGradient(JOB_BREAKDOWN_SLICES.pending, pendingJobs)
              ]}
            />
          </section>
          <section className="panel dash-panel dash-highlight-card dash-highlight-hours">
            <h2>Field hours</h2>
            <p className="chart-subtitle">Total time recorded on your assigned jobs</p>
            <p className="dash-highlight-value">
              {Number(technicianStats.hoursLogged || 0).toFixed(1)}
            </p>
          </section>
        </div>

        <DashboardPanel
          title="Recent assignments"
          subtitle="Latest work orders assigned to you"
        >
          <WorkOrderTable
            rows={technicianStats.recentJobs}
            emptyMessage="No jobs assigned yet. Check back once dispatch assigns work."
            columns={[
              { key: "wo", label: "WO", render: (w) => w.workOrderNumber || "-" },
              { key: "title", label: "Title", render: (w) => w.title },
              { key: "site", label: "Site", render: (w) => w.site?.siteName || "-" },
              {
                key: "status",
                label: "Status",
                render: (w) => <StatusBadge status={w.status} />
              },
              {
                key: "sla",
                label: "SLA",
                render: (w) => <SlaBadge workOrder={w} />
              }
            ]}
          />
        </DashboardPanel>
      </DashboardShell>
    );
  }

  if (role === "CUSTOMER") {
    const statusSlices = Object.entries(customerStats.byStatus || {}).map(
      ([label, value]) => ({
        label,
        value,
        color: STATUS_COLORS[label] || "#64748b",
        gradient: STATUS_GRADIENTS[label] || [STATUS_COLORS[label] || "#64748b", "#475569"]
      })
    );
    const assignedCount = customerStats.requests.filter((w) => w.assignedTechnician).length;

    return (
      <DashboardShell
        themeKey="manager"
        eyebrow="Customer portal"
        title="Track your service requests"
        subtitle={
          user?.customerName
            ? `Welcome back. Here is the latest activity for ${user.customerName}.`
            : "Create requests, follow progress, and stay informed on every site visit."
        }
        icon={ClipboardList}
        action={
          <button className="dash-btn" type="button" onClick={() => setPage("requests")}>
            Create request
          </button>
        }
      >
        <StatCards
          items={withStatTheme([
            {
              label: "Total requests",
              value: customerStats.total,
              icon: <FileText size={20} />,
              tone: "accent"
            },
            {
              label: "My sites",
              value: customerStats.sites,
              icon: <MapPin size={20} />
            },
            {
              label: "Assigned",
              value: assignedCount,
              icon: <Users size={20} />
            },
            {
              label: "Awaiting assignment",
              value: customerStats.total - assignedCount,
              icon: <Clock3 size={20} />
            }
          ])}
        />

        <div className="chart-grid">
          <section className="panel dash-panel">
            <DonutChart
              title="Request status"
              subtitle="How your open and completed requests are distributed"
              slices={statusSlices}
            />
          </section>
          <section className="panel dash-panel">
            <BarChart
              title="Assigned technician"
              subtitle="Who is handling your service requests"
              data={customerStats.byAssignee}
            />
          </section>
        </div>

        <DashboardPanel
          title="My requests"
          subtitle="Live view of work orders for your organisation"
        >
          <WorkOrderTable
            rows={customerStats.requests}
            emptyMessage="No requests yet. Create your first service request to get started."
            columns={[
              { key: "wo", label: "WO", render: (w) => w.workOrderNumber || "-" },
              { key: "title", label: "Title", render: (w) => w.title },
              {
                key: "assignee",
                label: "Assigned to",
                render: (w) =>
                  w.assignedTechnician?.fullName ||
                  w.assignedTechnician?.name ||
                  "Unassigned"
              },
              {
                key: "status",
                label: "Status",
                render: (w) => <StatusBadge status={w.status} />
              }
            ]}
          />
        </DashboardPanel>
      </DashboardShell>
    );
  }

  if (role === "DISPATCHER") {
    return (
      <DashboardShell
        themeKey="manager"
        eyebrow="Dispatch center"
        title="Coordinate the field team"
        subtitle="Assign technicians, monitor workload, and keep service moving on schedule."
        icon={CalendarClock}
        action={
          <button className="dash-btn" type="button" onClick={() => setPage("workorders")}>
            Dispatch jobs
          </button>
        }
      >
        <StatCards
          items={withStatTheme([
            {
              label: "Total work orders",
              value: s.totalWorkOrders || 0,
              icon: <ClipboardList size={20} />,
              tone: "accent"
            },
            {
              label: "Technicians",
              value: s.availableTechnicians || 0,
              icon: <Users size={20} />
            },
            {
              label: "Customers",
              value: s.totalCustomers || 0,
              icon: <Building2 size={20} />
            },
            {
              label: "Critical jobs",
              value: s.criticalWorkOrders || 0,
              icon: <AlertTriangle size={20} />,
              tone: (s.criticalWorkOrders || 0) > 0 ? "warn" : "ok"
            }
          ])}
        />

        <div className="chart-grid">
          <section className="panel dash-panel">
            <DonutChart
              title="Work order mix"
              subtitle="Current pipeline across the operation"
              slices={buildMixSlices(s)}
            />
          </section>
          <section className="panel dash-panel">
            <BarChart
              title="Workload by technician"
              subtitle="Open assignments per field engineer"
              data={s.byTechnician || {}}
            />
          </section>
        </div>

        <DashboardPanel
          title="Work orders needing attention"
          subtitle="Recent jobs to review, assign, or follow up"
        >
          <WorkOrderTable
            rows={s.recentWorkOrders || []}
            emptyMessage="No recent work orders to display."
            columns={[
              { key: "wo", label: "WO", render: (w) => w.workOrderNumber },
              { key: "customer", label: "Customer", render: (w) => w.customer?.name },
              { key: "site", label: "Site", render: (w) => w.site?.siteName },
              {
                key: "status",
                label: "Status",
                render: (w) => <StatusBadge status={w.status} />
              },
              { key: "priority", label: "Priority", render: (w) => w.priority },
              {
                key: "sla",
                label: "SLA",
                render: (w) => <SlaBadge workOrder={w} />
              }
            ]}
          />
        </DashboardPanel>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell
      themeKey="manager"
      eyebrow="Operations control"
      title="Manage and monitor field operations"
      subtitle="Full visibility across customers, technicians, inventory, and service performance."
      icon={LayoutDashboard}
      action={
        <button className="dash-btn" type="button" onClick={() => setPage("workorders")}>
          Create work order
        </button>
      }
    >
      <StatCards
        items={withStatTheme([
          {
            label: "Technicians",
            value: s.availableTechnicians || 0,
            icon: <Users size={20} />,
            tone: "accent"
          },
          {
            label: "Customers",
            value: s.totalCustomers || 0,
            icon: <Building2 size={20} />
          },
          {
            label: "Critical",
            value: s.criticalWorkOrders || 0,
            icon: <AlertTriangle size={20} />,
            tone: (s.criticalWorkOrders || 0) > 0 ? "warn" : "ok"
          },
          {
            label: "Overdue",
            value: s.overdueWorkOrders || 0,
            icon: <Clock3 size={20} />,
            tone: (s.overdueWorkOrders || 0) > 0 ? "warn" : "ok"
          },
          {
            label: "Low stock",
            value: s.lowStockParts || 0,
            icon: <Package size={20} />,
            tone: (s.lowStockParts || 0) > 0 ? "warn" : "ok",
            hint:
              (s.lowStockParts || 0) > 0
                ? "Parts at or below reorder level"
                : "Inventory levels look healthy"
          }
        ])}
      />

      {(s.lowStockPartItems || []).length > 0 && (
        <DashboardPanel
          title="Low stock alerts"
          subtitle="These parts need restocking soon"
          action={
            <button className="action-btn edit-btn" type="button" onClick={() => setPage("parts")}>
              View inventory
            </button>
          }
        >
          <div className="dash-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Part</th>
                  <th>Part no.</th>
                  <th>Stock</th>
                  <th>Reorder level</th>
                </tr>
              </thead>
              <tbody>
                {(s.lowStockPartItems || []).map((part) => (
                  <tr key={part.id} className="low-stock-row">
                    <td>{part.partName}</td>
                    <td>{part.partNumber}</td>
                    <td>
                      {part.stockQuantity}
                      <span className="badge AT_RISK" style={{ marginLeft: 8 }}>
                        Low
                      </span>
                    </td>
                    <td>{part.reorderLevel}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </DashboardPanel>
      )}

      <div className="chart-grid">
        <section className="panel dash-panel">
          <DonutChart
            title="Work order mix"
            subtitle="Operational pipeline at a glance"
            slices={buildMixSlices(s, true)}
          />
        </section>
        <section className="panel dash-panel">
          <SlaGauge percent={s.slaCompliancePercent || 0} />
        </section>
      </div>

      <div className="chart-grid">
        <section className="panel dash-panel">
          <BarChart
            title="Workload by technician"
            subtitle="Distribution of active assignments"
            data={s.byTechnician || {}}
          />
        </section>
        <section className="panel dash-panel">
          <BarChart
            title="Workload by site"
            subtitle="Where service demand is concentrated"
            data={s.bySite || {}}
          />
        </section>
      </div>

      <DashboardPanel
        title="Recent work orders"
        subtitle="Latest activity across the service network"
        action={
          <button className="action-btn edit-btn" type="button" onClick={() => setPage("board")}>
            Open board
          </button>
        }
      >
        <WorkOrderTable
          rows={s.recentWorkOrders || []}
          emptyMessage="No recent work orders yet."
          columns={[
            { key: "wo", label: "WO", render: (w) => w.workOrderNumber },
            { key: "title", label: "Title", render: (w) => w.title },
            { key: "customer", label: "Customer", render: (w) => w.customer?.name },
            { key: "site", label: "Site", render: (w) => w.site?.siteName },
            {
              key: "status",
              label: "Status",
              render: (w) => <StatusBadge status={w.status} />
            },
            { key: "priority", label: "Priority", render: (w) => w.priority },
            {
              key: "sla",
              label: "SLA",
              render: (w) => <SlaBadge workOrder={w} />
            }
          ]}
        />
      </DashboardPanel>
    </DashboardShell>
  );
}
