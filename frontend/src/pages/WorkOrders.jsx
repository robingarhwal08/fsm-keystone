import { useEffect, useState } from "react";

import {
  createWorkOrder,
  getCustomers,
  getSites,
  getTechnicians,
  getWorkOrders,
  updateWorkOrderStatus,
  updateWorkOrder,
  deleteWorkOrder
} from "../services/commonService";

import StatusBadge from "../components/StatusBadge";

export default function WorkOrders({ user }) {

  const [rows, setRows] = useState([]);
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
         r.data.filter(
           (w) =>
             w.customer?.id === user.customerId ||
             w.customerId === user.customerId
         )
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

    await deleteWorkOrder(id);

    load();
  };

  const save = async (e) => {

    e.preventDefault();

    if (editingId) {

      await updateWorkOrder(
        editingId,
        {
          ...f,
          customerId:
            user?.role === "CUSTOMER"
              ? user.customerId
              : +f.customerId,

          siteId: +f.siteId,

          assignedTechnicianId:
            user?.role === "CUSTOMER"
              ? null
              : (
                  f.assignedTechnicianId
                    ? +f.assignedTechnicianId
                    : null
                ),

          createdByUserId: user?.userId
        }
      );

    } else {

      await createWorkOrder({
        ...f,

        customerId:
          user?.role === "CUSTOMER"
            ? user.customerId
            : +f.customerId,

        siteId: +f.siteId,

        assignedTechnicianId:
          user?.role === "CUSTOMER"
            ? null
            : (
                f.assignedTechnicianId
                  ? +f.assignedTechnicianId
                  : null
              ),

        createdByUserId: user?.userId
      });

    }

    resetForm();

    load();
  };

  const setStatus = async (id, status) => {

    await updateWorkOrderStatus(id, {
      status,
      changedByUserId: user?.userId,
      comment: `Status changed to ${status}`
    });

    load();
  };

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
                customerId: e.target.value
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
          <option value="">Select Site</option>

          {sites.map((s) => (
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

        <table>
          <thead>
            <tr>
              <th>WO</th>
              <th>Title</th>
              <th>Customer</th>
              <th>Technician</th>
              <th>Status</th>
              <th>Action</th>

              {user?.role === "MANAGER" && (
                <th>Manage</th>
              )}
            </tr>
          </thead>

         <tbody>

           {rows.length === 0 && (
             <tr>
               <td
                 colSpan={user?.role === "MANAGER" ? 7 : 6}
                 style={{
                   textAlign: "center",
                   padding: "20px"
                 }}
               >
                 No Work Orders Found
               </td>
             </tr>
           )}

           {rows.map((w) => (
              <tr key={w.id}>

                <td>{w.workOrderNumber}</td>

                <td>{w.title}</td>

                <td>{w.customer?.name}</td>

                <td>
                  {w.assignedTechnician?.fullName || "-"}
                </td>

                <td>
                  <StatusBadge status={w.status} />
                </td>

                <td>
                  {user?.role === "TECHNICIAN" ? (
                    <select
                      value={w.status}
                      onChange={(e) =>
                        setStatus(w.id, e.target.value)
                      }
                    >
                      <option value="ASSIGNED">ASSIGNED</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="COMPLETED">COMPLETED</option>
                    </select>
                  ) : (
                    <select
                      value={w.status}
                      onChange={(e) =>
                        setStatus(w.id, e.target.value)
                      }
                    >
                      <option value="CREATED">CREATED</option>
                      <option value="ASSIGNED">ASSIGNED</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="ON_HOLD">ON_HOLD</option>
                      <option value="COMPLETED">COMPLETED</option>
                      <option value="CLOSED">CLOSED</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  )}
                </td>

                {user?.role === "MANAGER" && (
                  <td>
                    <button
                      type="button"
                      className="edit-btn"
                      onClick={() => editWorkOrder(w)}
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      className="delete-btn"
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

      </section>

    </div>
  );
}