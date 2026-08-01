import { useEffect, useState } from "react";
import {
  getWorkOrders,
  getParts,
  createPartUsage
} from "../services/commonService";

export default function PartUsage({ user }) {

  const [jobs, setJobs] = useState([]);
  const [parts, setParts] = useState([]);

  const [form, setForm] = useState({
    workOrderId: "",
    partId: "",
    quantity: 1
  });

  useEffect(() => {

    getWorkOrders().then((r) => {

      const technicianJobs = r.data.filter(
        (w) =>
          w.assignedTechnician?.id === user.userId ||
          w.assignedTechnicianId === user.userId
      );

      setJobs(technicianJobs);
    });

    getParts().then((r) => {
      setParts(r.data);
    });

  }, [user]);

  const save = async (e) => {

    e.preventDefault();

  await createPartUsage({
    workOrderId: Number(form.workOrderId),
    partId: Number(form.partId),
    quantityUsed: Number(form.quantity),
    usedByUserId: user.userId
  });

    alert("Part Usage Added");

    setForm({
      workOrderId: "",
      partId: "",
      quantity: 1
    });
  };

  return (
    <div className="page">

      <form
        className="panel"
        onSubmit={save}
      >
        <h2>Record Part Usage</h2>

        <select
          value={form.workOrderId}
          onChange={(e) =>
            setForm({
              ...form,
              workOrderId: e.target.value
            })
          }
        >
          <option value="">
            Select Work Order
          </option>

          {jobs.map((w) => (
            <option
              key={w.id}
              value={w.id}
            >
              {w.workOrderNumber}
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
        >
          <option value="">
            Select Part
          </option>

          {parts.map((p) => (
            <option
              key={p.id}
              value={p.id}
            >
              {p.partName}
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

        <button
          className="primary"
          type="submit"
        >
          Record Usage
        </button>

      </form>

    </div>
  );
}