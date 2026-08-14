import { useEffect, useMemo, useState } from "react";
import {
  getWorkOrders,
  getParts,
  createPartUsage,
  getPartUsage,
  confirmPartUsage
} from "../services/commonService";
import { formatDateTime } from "../components/StatusHistoryTable";

export default function PartUsage({ user }) {

  const isTech = user?.role === "TECHNICIAN";
  const canConfirm = user?.role === "MANAGER" || user?.role === "DISPATCHER";

  const [jobs, setJobs] = useState([]);
  const [parts, setParts] = useState([]);
  const [usages, setUsages] = useState([]);

  const [form, setForm] = useState({
    workOrderId: "",
    partId: "",
    quantity: 1,
    usageStatus: isTech ? "USED" : "PENDING"
  });

  const loadParts = () => {
    getParts()
      .then((r) => setParts(r.data || []))
      .catch(() => setParts([]));
  };

  const loadUsages = () => {
    getPartUsage()
      .then((r) => setUsages(r.data || []))
      .catch(() => setUsages([]));
  };

  useEffect(() => {
    if (isTech) {
      getWorkOrders().then((r) => {
        const data = r.data || [];
        setJobs(data.filter((w) =>
          w.assignedTechnician?.id === user.userId ||
          w.assignedTechnicianId === user.userId
        ));
      });
    }
    loadParts();
    loadUsages();
  }, [user]);

  const summary = useMemo(() => {
    const pendingQty = usages
      .filter((u) => u.usageStatus === "PENDING")
      .reduce((sum, u) => sum + (u.quantityUsed || 0), 0);
    const usedQty = usages
      .filter((u) => u.usageStatus !== "PENDING")
      .reduce((sum, u) => sum + (u.quantityUsed || 0), 0);
    const inventoryQty = parts.reduce(
      (sum, p) => sum + (p.stockQuantity ?? 0),
      0
    );
    return {
      pendingQty,
      usedQty,
      totalLogged: pendingQty + usedQty,
      inventoryQty
    };
  }, [usages, parts]);

  const save = async (e) => {
    e.preventDefault();
    if (!form.workOrderId || !form.partId) {
      alert("Please select a work order and a part.");
      return;
    }
    try {
      await createPartUsage({
        workOrderId: Number(form.workOrderId),
        partId: Number(form.partId),
        quantityUsed: Number(form.quantity),
        usedByUserId: user.userId,
        usageStatus: form.usageStatus
      });
      alert(form.usageStatus === "PENDING"
        ? "Part request saved as pending. Inventory is unchanged until it is marked used."
        : "Part usage recorded and inventory updated.");
      setForm({
        workOrderId: "",
        partId: "",
        quantity: 1,
        usageStatus: isTech ? "USED" : "PENDING"
      });
      loadParts();
      loadUsages();
    } catch (err) {
      alert(err.response?.data?.message || "Could not record part usage.");
    }
  };

  const markUsed = async (id) => {
    try {
      await confirmPartUsage(id);
      loadParts();
      loadUsages();
    } catch (err) {
      alert(err.response?.data?.message || "Could not confirm part usage.");
    }
  };

  return (
    <div className="page">
      <section className="panel">
        <h2>Part usage summary</h2>
        <p>
          Pending: <b>{summary.pendingQty}</b>
          {" · "}
          Part used: <b>{summary.usedQty}</b>
          {" · "}
          Part total logged: <b>{summary.totalLogged}</b>
          {" · "}
          Inventory on hand: <b>{summary.inventoryQty}</b>
        </p>
      </section>

      {isTech && (
      <form className="panel" onSubmit={save}>
        <h2>Record Part Usage</h2>

        <select
          value={form.workOrderId}
          onChange={(e) =>
            setForm({
              ...form,
              workOrderId: e.target.value
            })
          }
          required
        >
          <option value="">Select Work Order</option>
          {jobs.map((w) => (
            <option key={w.id} value={w.id}>
              {w.workOrderNumber} - {w.title}
            </option>
          ))}
        </select>

        <select
          value={form.partId}
          onChange={(e) =>
            setForm({
              ...form,
              partId: e.target.value
            })
          }
          required
        >
          <option value="">Select Part</option>
          {parts.map((p) => (
            <option key={p.id} value={p.id}>
              {p.partName} (stock: {p.stockQuantity ?? 0})
            </option>
          ))}
        </select>

        <input
          type="number"
          min="1"
          value={form.quantity}
          onChange={(e) =>
            setForm({
              ...form,
              quantity: e.target.value
            })
          }
        />

        <select
          value={form.usageStatus}
          onChange={(e) =>
            setForm({
              ...form,
              usageStatus: e.target.value
            })
          }
        >
          <option value="PENDING">Pending (hold inventory)</option>
          <option value="USED">Part used (deduct inventory)</option>
        </select>

        <button className="primary" type="submit">
          Record Usage
        </button>
      </form>
      )}

      <section className="panel">
        <h2>Part usage history</h2>
        <table>
          <thead>
            <tr>
              <th>Work order</th>
              <th>Title</th>
              <th>Site</th>
              <th>Part</th>
              <th>Qty</th>
              <th>Total</th>
              <th>Inventory left</th>
              <th>Status</th>
              <th>Used by</th>
              <th>When</th>
              {canConfirm && <th></th>}
            </tr>
          </thead>
          <tbody>
            {usages.length === 0 && (
              <tr>
                <td colSpan={canConfirm ? 11 : 10}>No part usage yet.</td>
              </tr>
            )}
            {usages.map((row) => (
              <tr key={row.id}>
                <td>{row.workOrderNumber || "-"}</td>
                <td>{row.workOrderTitle || "-"}</td>
                <td>{row.siteName || "-"}</td>
                <td>{row.partName || "-"}</td>
                <td>{row.quantityUsed ?? "-"}</td>
                <td>{row.totalCost ?? "-"}</td>
                <td>{row.remainingStock ?? "-"}</td>
                <td>
                  <span className={`badge ${row.usageStatus}`}>
                    {row.usageStatus === "PENDING" ? "Pending" : "Part used"}
                  </span>
                </td>
                <td>{row.usedBy || "-"}</td>
                <td>{formatDateTime(row.usedAt)}</td>
                {canConfirm && (
                  <td>
                    {row.usageStatus === "PENDING" && (
                      <button
                        className="primary"
                        type="button"
                        onClick={() => markUsed(row.id)}
                      >
                        Mark used
                      </button>
                    )}
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
