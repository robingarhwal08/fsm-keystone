import { useEffect, useState } from "react";
import {
  createWorkOrder,
  getSites,
  getWorkOrders
} from "../services/commonService";

import StatusBadge from "../components/StatusBadge";

export default function CustomerReports({ user }) {
  const [rows, setRows] = useState([]);
  const [sites, setSites] = useState([]);

  const [f, setF] = useState({
    title: "",
    description: "",
    siteId: "",
    priority: "MEDIUM"
  });

  const getCustomerId = () => {
    return user?.customerId || user?.customer?.id || null;
  };

  const load = () => {
    getSites().then((r) => {
      const customerId = getCustomerId();

      if (customerId) {
        const filteredSites = r.data.filter(
          (s) => s.customer?.id === customerId || s.customerId === customerId
        );

        setSites(filteredSites);
      } else {
        setSites(r.data);
      }
    });

    getWorkOrders().then((r) => {
      const customerId = getCustomerId();

      if (customerId) {
        const filteredWorkOrders = r.data.filter(
          (w) => w.customer?.id === customerId || w.customerId === customerId
        );

        setRows(filteredWorkOrders);
      } else {
        setRows(r.data);
      }
    });
  };

  useEffect(() => {
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();

    const customerId = getCustomerId();

    if (!customerId) {
      alert("Customer id is missing for this logged-in user.");
      return;
    }

    if (!f.title || !f.description || !f.siteId) {
      alert("Please fill all required fields.");
      return;
    }

    await createWorkOrder({
      title: f.title,
      description: f.description,
      customerId: customerId,
      siteId: +f.siteId,
      assignedTechnicianId: null,
      priority: f.priority,
      createdByUserId: user?.userId
    });

    setF({
      title: "",
      description: "",
      siteId: "",
      priority: "MEDIUM"
    });

    load();

    alert("Work order request created successfully.");
  };

  return (
    <div className="page">

      <form className="panel wo-form" onSubmit={save}>

        <h2>Create Customer Request</h2>

        <p>
          Submit a new service request. Your request will be reviewed and assigned
          to a technician.
        </p>

        <input
          placeholder="Request Title"
          value={f.title}
          onChange={(e) =>
            setF({
              ...f,
              title: e.target.value
            })
          }
        />

        <input
          placeholder="Request Description"
          value={f.description}
          onChange={(e) =>
            setF({
              ...f,
              description: e.target.value
            })
          }
        />

        <select
          value={f.siteId}
          onChange={(e) =>
            setF({
              ...f,
              siteId: e.target.value
            })
          }
        >
          <option value="">Select Site</option>

          {sites.map((s) => (
            <option key={s.id} value={s.id}>
              {s.siteName}
            </option>
          ))}
        </select>

        <select
          value={f.priority}
          onChange={(e) =>
            setF({
              ...f,
              priority: e.target.value
            })
          }
        >
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>

        <button className="primary">
          Submit Request
        </button>

      </form>

      <section className="panel">

        <h2>My Work Order Requests</h2>

        <table>
          <thead>
            <tr>
              <th>WO</th>
              <th>Title</th>
              <th>Site</th>
              <th>Priority</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>

            {rows.length === 0 && (
              <tr>
                <td colSpan="5">
                  No work order requests found.
                </td>
              </tr>
            )}

            {rows.map((w) => (
              <tr key={w.id}>

                <td>{w.workOrderNumber || "-"}</td>

                <td>{w.title}</td>

                <td>
                  {w.site?.siteName || "-"}
                </td>

                <td>{w.priority}</td>

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