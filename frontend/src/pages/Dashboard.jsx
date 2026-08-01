import { useEffect, useState } from "react";

import {
  dashboardSummary,
  getWorkOrders,
  getSites
} from "../services/commonService";

import StatCard from "../components/StatCard";
import StatusBadge from "../components/StatusBadge";

export default function Dashboard({ user, setPage }) {
  const [s, setS] = useState({});

  const [customerStats, setCustomerStats] = useState({
    open: 0,
    inProgress: 0,
    completed: 0,
    sites: 0
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
        const customerId = user.customerId;

        const workOrders = woRes.data.filter(
          (w) =>
            w.customer?.id === customerId ||
            w.customerId === customerId
        );

        const sites = siteRes.data.filter(
          (s) =>
            s.customer?.id === customerId ||
            s.customerId === customerId
        );

        setCustomerStats({
          open: workOrders.filter(
            (w) =>
              w.status === "CREATED" ||
              w.status === "ASSIGNED"
          ).length,

          inProgress: workOrders.filter(
            (w) => w.status === "IN_PROGRESS"
          ).length,

          completed: workOrders.filter(
            (w) =>
              w.status === "COMPLETED" ||
              w.status === "CLOSED"
          ).length,

          sites: sites.length
        });
      });
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

            hoursLogged: 0,

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

        <div className="stats-grid">
          <StatCard
            title="My Jobs"
            value={technicianStats.jobs}
            color="blue"
          />

          <StatCard
            title="In Progress"
            value={technicianStats.inProgress}
            color="orange"
          />

          <StatCard
            title="Completed"
            value={technicianStats.completed}
            color="green"
          />

          <StatCard
            title="Hours Logged"
            value={technicianStats.hoursLogged}
            color="purple"
          />
        </div>


      </div>
    );
  }

  // ======================================================
  // CUSTOMER DASHBOARD
  // ======================================================

  if (role === "CUSTOMER") {
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

        <div className="stats-grid">
          <StatCard
            title="Open Requests"
            value={customerStats.open}
            color="blue"
          />

          <StatCard
            title="In Progress"
            value={customerStats.inProgress}
            color="orange"
          />

          <StatCard
            title="Completed"
            value={customerStats.completed}
            color="green"
          />

          <StatCard
            title="My Sites"
            value={customerStats.sites}
            color="purple"
          />
        </div>
      </div>
    );
  }

  // ======================================================
  // DISPATCHER DASHBOARD
  // ======================================================

  if (role === "DISPATCHER") {
    const dispatcherCards = [
      [
        "Total Work Orders",
        s.totalWorkOrders || 0,
        "blue"
      ],
      [
        "Assigned Orders",
        s.assignedWorkOrders || 0,
        "orange"
      ],
      [
        "In Progress",
        s.inProgressWorkOrders || 0,
        "purple"
      ],
      [
        "Available Technicians",
        s.availableTechnicians || 0,
        "green"
      ],
      [
        "Critical Orders",
        s.criticalWorkOrders || 0,
        "red"
      ],
      [
        "Customers",
        s.totalCustomers || 0,
        "cyan"
      ]
    ];

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

        <div className="stats-grid">
          {dispatcherCards.map((c) => (
            <StatCard
              key={c[0]}
              title={c[0]}
              value={c[1]}
              color={c[2]}
            />
          ))}
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

  const cards = [
    [
      "Total Work Orders",
      s.totalWorkOrders || 0,
      "blue"
    ],
    [
      "Assigned",
      s.assignedWorkOrders || 0,
      "orange"
    ],
    [
      "In Progress",
      s.inProgressWorkOrders || 0,
      "purple"
    ],
    [
      "Completed",
      s.completedWorkOrders || 0,
      "green"
    ],
    [
      "Critical",
      s.criticalWorkOrders || 0,
      "red"
    ],
    [
      "Technicians",
      s.availableTechnicians || 0,
      "cyan"
    ],
    [
      "Customers",
      s.totalCustomers || 0,
      "blue"
    ],
    [
      "Low Stock Parts",
      s.lowStockParts || 0,
      "red"
    ]
  ];

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

      <div className="stats-grid">
        {cards.map((c) => (
          <StatCard
            key={c[0]}
            title={c[0]}
            value={c[1]}
            color={c[2]}
          />
        ))}
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
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}