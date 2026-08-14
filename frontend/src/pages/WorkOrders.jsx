import { useEffect, useState } from "react";

import {
  createWorkOrder,
  getCustomers,
  getSites,
  getTechnicians,
  getWorkOrders,
  updateWorkOrderStatus,
  updateWorkOrder,
  deleteWorkOrder,
  assignTechnician,
  getWorkOrderHistory
} from "../services/commonService";

import StatusBadge from "../components/StatusBadge";
import StatusHistoryTable from "../components/StatusHistoryTable";
import SlaBadge from "../components/SlaBadge";

export default function WorkOrders({ user }) {

  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState("");
  const [historyWorkOrder, setHistoryWorkOrder] = useState(null);
  const [history, setHistory] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [sites, setSites] = useState([]);
  const [techs, setTechs] = useState([]);
  const [editingId, setEditingId] = useState(null);

  const [f, setF] = useState({
    title: "",
    description: "",
    customerId: "",
    siteId: "",
    assignedTechnicianId: "",
    priority: "MEDIUM"
  });

  const resetForm = () => {
    setF({
      title: "",
      description: "",
      customerId: "",
      siteId: "",
      assignedTechnicianId: "",
      priority: "MEDIUM"
    });

    setEditingId(null);
  };

  const load = () => {

   getWorkOrders().then((r) => {

     console.log("Logged In User:", user);
     console.log("All Work Orders:", r.data);

     if (user?.role === "CUSTOMER") {

       setRows(
         r.data.filter((w) => {
           const creatorId = w.createdBy?.id || w.createdByUserId;
           const uid = user?.userId || user?.id;
           return uid != null && creatorId != null && String(creatorId) === String(uid);
         })
       );

     } else if (user?.role === "TECHNICIAN") {

       const technicianJobs = r.data.filter(
         (w) =>
           w.assignedTechnician?.id === user.userId ||
           w.assignedTechnicianId === user.userId
       );

       console.log("Technician Jobs:", technicianJobs);

       setRows(technicianJobs);

     } else {

       setRows(r.data);

     }
   });

    getCustomers().then((r) => {
      setCustomers(r.data);
    });

    getSites().then((r) => {

      if (user?.role === "CUSTOMER") {

        setSites(
          r.data.filter(
            (s) =>
              s.customer?.id === user.customerId ||
              s.customerId === user.customerId
          )
        );

      } else {

        setSites(r.data);

      }

    });

    getTechnicians().then((r) => {
      setTechs(r.data);
    });
  };

  useEffect(() => {
    load();
  }, [user]);

  const editWorkOrder = (w) => {

    setEditingId(w.id);

    setF({
      title: w.title || "",
      description: w.description || "",
      customerId: w.customer?.id || "",
      siteId: w.site?.id || "",
      assignedTechnicianId: w.assignedTechnician?.id || "",
      priority: w.priority || "MEDIUM"
    });
  };

    const removeWorkOrder = async (id) => {

    const ok = window.confirm(
      "Are you sure you want to delete this work order?"
    );

    if (!ok) {
      return;
    }

    try {
      await deleteWorkOrder(id);
      load();
    } catch (err) {
      alert(err.response?.data?.message || "Could not delete work order.");
    }
  };

  const save = async (e) => {

    e.preventDefault();

    try {
    if (editingId) {

      await updateWorkOrder(
        editingId,
        {
          title: f.title,
          description: f.description,
          customerId:
            user?.role === "CUSTOMER"
              ? user.customerId
              : Number(f.customerId),

          siteId: Number(f.siteId),

          assignedTechnicianId:
            user?.role === "CUSTOMER"
              ? null
              : (
                  f.assignedTechnicianId
                    ? Number(f.assignedTechnicianId)
                    : null
                ),

          priority: f.priority,
          createdByUserId: user?.userId
        }
      );

    } else {

      await createWorkOrder({
        title: f.title,
        description: f.description,

        customerId:
          user?.role === "CUSTOMER"
            ? user.customerId
            : Number(f.customerId),

        siteId: Number(f.siteId),

        assignedTechnicianId:
          user?.role === "CUSTOMER"
            ? null
            : (
                f.assignedTechnicianId
                  ? Number(f.assignedTechnicianId)
                  : null
              ),

        priority: f.priority,
        createdByUserId: user?.userId
      });

    }

    resetForm();

    load();
    } catch (err) {
      alert(err.response?.data?.message || "Could not save work order.");
    }
  };

  const locked = (status) => status === "CLOSED" || status === "CANCELLED";

  const setStatus = async (id, status) => {

    await updateWorkOrderStatus(id, {
      status,
      changedByUserId: user?.userId,
      comment: `Status changed to ${status}`
    }).catch((err) => {
      alert(err.response?.data?.message || "Status change rejected");
    });

    load();
  };
  const changeTechnician = async (workOrderId, technicianId) => {

    await assignTechnician(workOrderId, {
      technicianId: Number(technicianId)
    });

    load();
  };

  const openHistory = async (w) => {
    const r = await getWorkOrderHistory(w.id);
    setHistoryWorkOrder(w);
    setHistory(r.data || []);
  };

  const selectedCustomerId =
    user?.role === "CUSTOMER"
      ? String(user.customerId || user.customer?.id || "")
      : String(f.customerId || "");

  const customerSites = sites.filter((s) => {
    if (!selectedCustomerId) {
      return false;
    }
    const siteCustomerId = String(s.customer?.id || s.customerId || "");
    return siteCustomerId === selectedCustomerId;
  });

  const visible = rows.filter((w) => {
    if (!query.trim()) return true;
    const q = query.toLowerCase();
    return (
      String(w.workOrderNumber || "").toLowerCase().includes(q) ||
      String(w.title || "").toLowerCase().includes(q) ||
      String(w.status || "").toLowerCase().includes(q)
    );
  });

  return (
    <div className="page">

      {user?.role !== "TECHNICIAN" && (
      <form className="panel wo-form" onSubmit={save}>


        <h2>
          {editingId ? "Edit Work Order" : "Create Work Order"}
        </h2>

        <input
          placeholder="Title"
          value={f.title}
          onChange={(e) =>
            setF({
              ...f,
              title: e.target.value
            })
          }
        />

        <input
          placeholder="Description"
          value={f.description}
          onChange={(e) =>
            setF({
              ...f,
              description: e.target.value
            })
          }
        />

        {user?.role !== "CUSTOMER" && (
          <select
            value={f.customerId}
            onChange={(e) =>
              setF({
                ...f,
                customerId: e.target.value,
                siteId: ""
              })
            }
          >
            <option value="">Select Customer</option>

            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        )}

        <select
          value={f.siteId}
          onChange={(e) =>
            setF({
              ...f,
              siteId: e.target.value
            })
          }
        >
          <option value="">
            {selectedCustomerId ? "Select Site" : "Select a customer first"}
          </option>

          {customerSites.map((s) => (
            <option key={s.id} value={s.id}>
              {s.siteName}
            </option>
          ))}
        </select>

        {user?.role !== "CUSTOMER" && (
          <select
            value={f.assignedTechnicianId}
            onChange={(e) =>
              setF({
                ...f,
                assignedTechnicianId: e.target.value
              })
            }
          >
            <option value="">Assign Technician</option>

            {techs.map((t) => (
              <option key={t.id} value={t.id}>
                {t.fullName}
              </option>
            ))}
          </select>
        )}

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

        <button className="primary" type="submit">
          {editingId ? "Update Work Order" : "Create Work Order"}
        </button>

        {editingId && (
          <button
            type="button"
            className="cancel-btn"
            onClick={resetForm}
          >
            Cancel
          </button>
        )}

      </form>
      )}


      <section className="panel">

        <h2>Work Orders</h2>
        <input
          placeholder="Search by code, title, or status"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />

        <table>
          <thead>
            <tr>
              <th>WO</th>
              <th>Title</th>
              <th>Customer</th>
              <th>Technician</th>
              <th>Status</th>
              <th>SLA</th>
              <th>Action</th>
              {(user?.role === "MANAGER" || user?.role === "DISPATCHER") && (
                <th>Manage</th>
              )}
            </tr>
          </thead>

         <tbody>

           {visible.length === 0 && (
             <tr>
               <td
                 colSpan={user?.role === "MANAGER" || user?.role === "DISPATCHER" ? 8 : 7}
                 style={{
                   textAlign: "center",
                   padding: "20px"
                 }}
               >
                 No Work Orders Found
               </td>
             </tr>
           )}

           {visible.map((w) => (
              <tr key={w.id}>

                <td>{w.workOrderNumber}</td>

                <td>{w.title}</td>

                <td>{w.customer?.name}</td>
                <td>
                  {(user?.role === "MANAGER" || user?.role === "DISPATCHER") ? (
                      <select
                          value={w.assignedTechnician?.id || ""}
                          onChange={(e) =>
                              changeTechnician(w.id, e.target.value)
                          }
                      >
                        <option value="">Assign Technician</option>

                        {techs.map((t) => (
                            <option key={t.id} value={t.id}>
                              {t.fullName}
                            </option>
                        ))}
                      </select>
                  ) : (
                      w.assignedTechnician?.fullName || "-"
                  )}
                </td>

                <td>
                  <StatusBadge status={w.status} />
                </td>

                <td>
                  <SlaBadge workOrder={w} />
                </td>

                <td>
                  {user?.role === "TECHNICIAN" ? (
                    <div className="tech-actions">
                      {w.status === "ASSIGNED" && (
                        <button type="button" className="action-btn edit-btn" onClick={() => setStatus(w.id, "IN_PROGRESS")}>Start</button>
                      )}
                      {w.status === "IN_PROGRESS" && (
                        <>
                          <button type="button" className="action-btn" onClick={() => setStatus(w.id, "ON_HOLD")}>Hold</button>
                          <button type="button" className="action-btn edit-btn" onClick={() => setStatus(w.id, "COMPLETED")}>Complete</button>
                        </>
                      )}
                      {w.status === "ON_HOLD" && (
                        <button type="button" className="action-btn edit-btn" onClick={() => setStatus(w.id, "IN_PROGRESS")}>Resume</button>
                      )}
                    </div>
                  ) : (
                    <select
                      value={w.status === "CREATED" ? "NEW" : w.status}
                      disabled={user?.role === "CUSTOMER"}
                      onChange={(e) =>
                        setStatus(w.id, e.target.value)
                      }
                    >
                      <option value="NEW">NEW</option>
                      <option value="ASSIGNED">ASSIGNED</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="ON_HOLD">ON_HOLD</option>
                      <option value="COMPLETED">COMPLETED</option>
                      <option value="CLOSED">CLOSED</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  )}
                  <button type="button" className="action-btn" onClick={() => openHistory(w)}>History</button>
                </td>

                {(user?.role === "MANAGER" || user?.role === "DISPATCHER") && (
                  <td>
                    <button
                        type="button"
                        className="action-btn edit-btn"
                        onClick={() => editWorkOrder(w)}
                    >
                      Edit
                    </button>

                    <button
                        type="button"
                        className="action-btn delete-btn"
                        onClick={() => removeWorkOrder(w.id)}
                    >
                      Delete
                    </button>
                  </td>
                )}

              </tr>
            ))}

          </tbody>
        </table>

        {historyWorkOrder && (
          <StatusHistoryTable
            rows={history}
            workOrderNumber={historyWorkOrder.workOrderNumber}
            title={historyWorkOrder.title}
          />
        )}

      </section>

    </div>
  );
}