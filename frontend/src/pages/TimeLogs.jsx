import { useEffect, useState } from "react";
import {
  getWorkOrders,
  createTimeLog,
  getTimeLogs
} from "../services/commonService";
import { formatDateTime } from "../components/StatusHistoryTable";

export default function TimeLogs({ user }) {

  const isTech = user?.role === "TECHNICIAN";
  const [jobs, setJobs] = useState([]);
  const [logs, setLogs] = useState([]);
  const [form, setForm] = useState({
    workOrderId: "",
    startTime: "",
    endTime: "",
    notes: ""
  });

  const loadLogs = () => {
    getTimeLogs()
      .then((r) => setLogs(r.data || []))
      .catch(() => setLogs([]));
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
    loadLogs();
  }, [user]);

  const save = async (e) => {
    e.preventDefault();
    if (!form.workOrderId || !form.startTime || !form.endTime) {
      alert("Please select a work order, start time, and end time.");
      return;
    }
    const start = new Date(form.startTime);
    const end = new Date(form.endTime);
    if (end <= start) {
      alert("End time must be after start time.");
      return;
    }
    try {
      await createTimeLog({
        workOrderId: Number(form.workOrderId),
        technicianId: user.userId,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        workDescription: form.notes
      });
      alert("Time log saved.");
      setForm({
        workOrderId: "",
        startTime: "",
        endTime: "",
        notes: ""
      });
      loadLogs();
    } catch (err) {
      alert(err.response?.data?.message || "Could not save time log.");
    }
  };

  return (
    <div className="page">
      {(isTech) && (
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
          required
        >
          <option value="">Select Work Order</option>
          {jobs.map((w) => (
            <option key={w.id} value={w.id}>
              {w.workOrderNumber} - {w.title}
            </option>
          ))}
        </select>

        <label>Start time</label>
        <input
          type="datetime-local"
          value={form.startTime}
          onChange={(e) =>
            setForm({
              ...form,
              startTime: e.target.value
            })
          }
          required
        />

        <label>End time</label>
        <input
          type="datetime-local"
          value={form.endTime}
          onChange={(e) =>
            setForm({
              ...form,
              endTime: e.target.value
            })
          }
          required
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

        <button className="primary" type="submit">
          Save Time Log
        </button>
      </form>
      )}

      <section className="panel">
        <h2>Time log history</h2>
        <table>
          <thead>
            <tr>
              <th>Work order</th>
              <th>Title</th>
              <th>Technician</th>
              <th>Start time</th>
              <th>End time</th>
              <th>Hours</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && (
              <tr>
                <td colSpan="7">No time logs yet.</td>
              </tr>
            )}
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{log.workOrderNumber || "-"}</td>
                <td>{log.workOrderTitle || "-"}</td>
                <td>{log.technicianName || "-"}</td>
                <td>{formatDateTime(log.startTime)}</td>
                <td>{formatDateTime(log.endTime)}</td>
                <td>{log.hoursSpent ?? "-"}</td>
                <td>{log.workDescription || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
