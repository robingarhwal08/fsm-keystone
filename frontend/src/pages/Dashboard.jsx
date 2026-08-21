import { useEffect, useState } from "react";

import {
  dashboardSummary,
  getWorkOrders,
  getSites
} from "../services/commonService";

import StatusBadge from "../components/StatusBadge";
import SlaBadge from "../components/SlaBadge";
import { AlertPills, BarChart, DonutChart, SlaGauge } from "../components/DashboardCharts";

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

    if (
      user?.role === "MANAGER" ||
      user?.role === "DISPATCHER"
    ) {
      dashboardSummary()
        .then((r) => setS(r.data))
        .catch(() => {});
    }

    if (user?.role === "CUSTOMER") {
      Promise.all([
        getWorkOrders(),
        getSites()
      ]).then(([woRes, siteRes]) => {
        const workOrders = woRes.data || [];
        const sites = siteRes.data || [];

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
      }).catch(() => {});
    }

    if (user?.role === "TECHNICIAN") {
      getWorkOrders()
        .then((woRes) => {
          const technicianId = user.userId;

          const technicianJobs = woRes.data.filter(
            (w) =>
              w.assignedTechnician?.id === technicianId ||
              w.assignedTechnicianId === technicianId ||
              w.technicianId === technicianId
          );

          console.log("Logged In Technician:", user);
          console.log("All Work Orders:", woRes.data);
          console.log("Technician Jobs:", technicianJobs);

          setTechnicianStats({
            jobs: technicianJobs.length,

            inProgress: technicianJobs.filter(
              (w) => w.status === "IN_PROGRESS"
            ).length,

            completed: technicianJobs.filter(
              (w) =>
                w.status === "COMPLETED" ||
                w.status === "CLOSED"
            ).length,

            hoursLogged: technicianJobs.reduce((sum, w) => sum + (w.totalMinutes || 0), 0) / 60,

            recentJobs: technicianJobs
          });
        })
        .catch(() => {});
    }
  }, [user]);

  const role = user?.role;

  // ======================================================
  // TECHNICIAN DASHBOARD
  // ======================================================

  if (role === "TECHNICIAN") {
    return (
      <div className="page">
        <section className="hero">
          <div className="radio-box">
            🔧
          </div>

          <div>
            <p className="live">
              TECHNICIAN WORKSPACE
            </p>

            <h1>
              Manage your assigned jobs
            </h1>

            <p>
              View assigned work orders, update status,
              add time logs and track completed jobs.
            </p>
          </div>


        </section>

        <div className="chart-grid">
          <section className="panel">
            <DonutChart
              title="My jobs"
              slices={[
                { label: "In progress", value: technicianStats.inProgress, color: "#c4785a" },
                { label: "Completed", value: technicianStats.completed, color: "#5c7a5e" },
                {
                  label: "Other",
                  value: Math.max(
                    0,
                    technicianStats.jobs - technicianStats.inProgress - technicianStats.completed
                  ),
                  color: "#7d8f6e"
                }
              ]}
            />
          </section>
          <section className="panel">
            <h2>Hours logged</h2>
            <p className="chart-empty">Total hours on assigned jobs</p>
            <h2 style={{ fontSize: 42, margin: "18px 0 0" }}>
              {Number(technicianStats.hoursLogged || 0).toFixed(1)}
            </h2>
          </section>
        </div>


      </div>
    );
  }

  // ======================================================
  // CUSTOMER DASHBOARD
  // ======================================================

  if (role === "CUSTOMER") {
    const statusColors = {
      NEW: "#a8a29a",
      CREATED: "#a8a29a",
      ASSIGNED: "#c4785a",
      IN_PROGRESS: "#6b7f5e",
      ON_HOLD: "#d4a574",
      COMPLETED: "#5c7a5e",
      CLOSED: "#4a5d4e",
      CANCELLED: "#9a4f3f"
    };
    const statusSlices = Object.entries(customerStats.byStatus || {}).map(
      ([label, value]) => ({
        label,
        value,
        color: statusColors[label] || "#7d8f6e"
      })
    );

    return (
      <div className="page">
        <section className="hero">
          <div className="radio-box">
            📋
          </div>

          <div>
            <p className="live">
              CUSTOMER PORTAL
            </p>

            <h1>
              Track your service requests
            </h1>

            <p>
              Create service requests and monitor work order progress.
            </p>
          </div>
        </section>

        <AlertPills
          items={[
            { label: "Total requests created", value: customerStats.total },
            { label: "My sites", value: customerStats.sites },
            {
              label: "Assigned",
              value: customerStats.requests.filter((w) => w.assignedTechnician).length
            },
            {
              label: "Unassigned",
              value: customerStats.requests.filter((w) => !w.assignedTechnician).length
            }
          ]}
        />

        <div className="chart-grid">
          <section className="panel">
            <DonutChart title="Request status" slices={statusSlices} />
          </section>
          <section className="panel">
            <BarChart title="Assigned technician" data={customerStats.byAssignee} />
          </section>
        </div>

        <section className="panel">
          <h2>My requests</h2>
          <table>
            <thead>
              <tr>
                <th>WO</th>
                <th>Title</th>
                <th>Assigned to</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {customerStats.requests.length === 0 && (
                <tr>
                  <td colSpan="4">No requests created yet.</td>
                </tr>
              )}
              {customerStats.requests.map((w) => (
                <tr key={w.id}>
                  <td>{w.workOrderNumber || "-"}</td>
                  <td>{w.title}</td>
                  <td>
                    {w.assignedTechnician?.fullName ||
                      w.assignedTechnician?.name ||
                      "Unassigned"}
                  </td>
                  <td>
                    <StatusBadge status={w.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    );
  }

  // ======================================================
  // DISPATCHER DASHBOARD
  // ======================================================

  if (role === "DISPATCHER") {
    return (
      <div className="page">
        <section className="hero">
          <div className="radio-box">
            📅
          </div>

          <div>
            <p className="live">
              DISPATCH CENTER
            </p>

            <h1>
              Manage technician assignments
            </h1>

            <p>
              Assign field technicians, schedule work orders,
              and monitor ongoing jobs.
            </p>
          </div>

          <button
            className="watch"
            onClick={() => setPage("workorders")}
          >
            Dispatch Jobs
          </button>
        </section>

        <AlertPills
          items={[
            { label: "Total work orders", value: s.totalWorkOrders || 0 },
            { label: "Technicians", value: s.availableTechnicians || 0 },
            { label: "Customers", value: s.totalCustomers || 0 },
            { label: "Critical", value: s.criticalWorkOrders || 0, tone: "warn" }
          ]}
        />

        <div className="chart-grid">
          <section className="panel">
            <DonutChart
              title="Work order mix"
              slices={[
                { label: "New", value: s.createdWorkOrders || 0, color: "#a8a29a" },
                { label: "Assigned", value: s.assignedWorkOrders || 0, color: "#c4785a" },
                { label: "In progress", value: s.inProgressWorkOrders || 0, color: "#6b7f5e" },
                { label: "Completed", value: s.completedWorkOrders || 0, color: "#5c7a5e" }
              ]}
            />
          </section>
          <section className="panel">
            <BarChart title="Workload by technician" data={s.byTechnician || {}} />
          </section>
        </div>

        <section className="panel">
          <h2>
            Work Orders Requiring Assignment
          </h2>

          <table>
            <thead>
              <tr>
                <th>WO No</th>
                <th>Customer</th>
                <th>Site</th>
                <th>Status</th>
                <th>Priority</th>
                <th>SLA</th>
              </tr>
            </thead>

            <tbody>
              {(s.recentWorkOrders || []).map((w) => (
                <tr key={w.id}>
                  <td>
                    {w.workOrderNumber}
                  </td>

                  <td>
                    {w.customer?.name}
                  </td>

                  <td>
                    {w.site?.siteName}
                  </td>

                  <td>
                    <StatusBadge status={w.status} />
                  </td>

                  <td>
                    {w.priority}
                  </td>
                  <td>
                    <SlaBadge workOrder={w} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    );
  }

  // ======================================================
  // MANAGER DASHBOARD
  // ======================================================

  return (
    <div className="page">
      <section className="hero">
        <div className="radio-box">
          ⌁
        </div>

        <div>
          <p className="live">
            FIELD SERVICE CONTROL CENTER
          </p>

          <h1>
            Manage and monitor field operations
          </h1>

          <p>
            Full visibility of customers, technicians,
            work orders, inventory and service metrics.
          </p>
        </div>

        <button
          className="watch"
          onClick={() => setPage("workorders")}
        >
          Create Work Order
        </button>
      </section>

      <AlertPills
        items={[
          { label: "Technicians", value: s.availableTechnicians || 0 },
          { label: "Customers", value: s.totalCustomers || 0 },
          { label: "Critical", value: s.criticalWorkOrders || 0, tone: (s.criticalWorkOrders || 0) > 0 ? "warn" : "" },
          { label: "Overdue", value: s.overdueWorkOrders || 0, tone: (s.overdueWorkOrders || 0) > 0 ? "warn" : "ok" },
          { label: "Low stock", value: s.lowStockParts || 0, tone: (s.lowStockParts || 0) > 0 ? "warn" : "" }
        ]}
      />

      <div className="chart-grid">
        <section className="panel">
          <DonutChart
            title="Work order mix"
            slices={[
              { label: "New", value: s.createdWorkOrders || 0, color: "#a8a29a" },
              { label: "Assigned", value: s.assignedWorkOrders || 0, color: "#c4785a" },
              { label: "In progress", value: s.inProgressWorkOrders || 0, color: "#6b7f5e" },
              { label: "On hold", value: s.onHoldWorkOrders || 0, color: "#d4a574" },
              { label: "Completed", value: s.completedWorkOrders || 0, color: "#5c7a5e" }
            ]}
          />
        </section>
        <section className="panel">
          <SlaGauge percent={s.slaCompliancePercent || 0} />
        </section>
      </div>

      <div className="chart-grid">
        <section className="panel">
          <BarChart title="Workload by technician" data={s.byTechnician || {}} />
        </section>
        <section className="panel">
          <BarChart title="Workload by site" data={s.bySite || {}} />
        </section>
      </div>

      <section className="panel">
        <h2>
          Recent Work Orders
        </h2>

        <table>
          <thead>
            <tr>
              <th>WO No</th>
              <th>Title</th>
              <th>Customer</th>
              <th>Site</th>
              <th>Status</th>
              <th>Priority</th>
              <th>SLA</th>
            </tr>
          </thead>

          <tbody>
            {(s.recentWorkOrders || []).map((w) => (
              <tr key={w.id}>
                <td>
                  {w.workOrderNumber}
                </td>

                <td>
                  {w.title}
                </td>

                <td>
                  {w.customer?.name}
                </td>

                <td>
                  {w.site?.siteName}
                </td>

                <td>
                  <StatusBadge status={w.status} />
                </td>

                <td>
                  {w.priority}
                </td>
                <td>
                  <SlaBadge workOrder={w} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}