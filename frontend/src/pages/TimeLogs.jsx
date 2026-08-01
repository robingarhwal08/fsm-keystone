import { useEffect, useState } from "react";
import {
  getWorkOrders,
  createTimeLog
} from "../services/commonService";

export default function TimeLogs({ user }) {

  const [jobs, setJobs] = useState([]);
  const [form, setForm] = useState({
    workOrderId: "",
    hoursWorked: "",
    notes: ""
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
  }, [user]);

  const save = async (e) => {

    e.preventDefault();

    const endTime = new Date();

    const startTime = new Date(
      endTime.getTime() -
      Number(form.hoursWorked) * 60 * 60 * 1000
    );

    await createTimeLog({
      workOrderId: Number(form.workOrderId),
      technicianId: user.userId,

      startTime: startTime.toISOString(),
      endTime: endTime.toISOString(),

      workDescription: form.notes
    });

    alert("Time Log Added");

    setForm({
      workOrderId: "",
      hoursWorked: "",
      notes: ""
    });
  };

  return (
    <div className="page">
      <form className="panel" onSubmit={save}>
        <h2>Add Time Log</h2>

        <select
          value={form.workOrderId}
          onChange={(e) =>
            setForm({
              ...form,
              workOrderId: e.target.value
            })
          }
        >
          <option value="">Select Work Order</option>

          {jobs.map((w) => (
            <option
              key={w.id}
              value={w.id}
            >
              {w.workOrderNumber} - {w.title}
            </option>
          ))}
        </select>

        <input
          type="number"
          placeholder="Hours Worked"
          value={form.hoursWorked}
          onChange={(e) =>
            setForm({
              ...form,
              hoursWorked: e.target.value
            })
          }
        />

        <textarea
          placeholder="Work Notes"
          value={form.notes}
          onChange={(e) =>
            setForm({
              ...form,
              notes: e.target.value
            })
          }
        />

        <button
          className="primary"
          type="submit"
        >
          Save Time Log
        </button>
      </form>
    </div>
  );
}