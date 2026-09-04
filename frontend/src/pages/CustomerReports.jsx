import { useEffect, useState } from "react";
import { getWorkOrders, getWorkOrderHistory } from "../services/commonService";
import StatusBadge from "../components/StatusBadge";
import StatusTimeline from "../components/StatusTimeline";
import SlaBadge from "../components/SlaBadge";
import { downloadRequestReport } from "../utils/requestReport";
import { filterWorkOrdersForCustomer } from "../utils/customerScope";

export default function CustomerReports({ user }) {
  const [rows, setRows] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyWorkOrder, setHistoryWorkOrder] = useState(null);

  const load = () => {
    getWorkOrders().then((r) => {
      setRows(filterWorkOrdersForCustomer(r.data || [], user));
    });
  };

  useEffect(() => {
    load();
  }, [user]);

  return (
    <div className="page">
      <section className="panel">
        <div className="panel-head">
          <h2>My Requests</h2>
          <button
            type="button"
            className="primary"
            onClick={() => downloadRequestReport(rows, user)}
            disabled={rows.length === 0}
          >
            Download PDF report
          </button>
        </div>
        <p>History of service requests you have created.</p>
        <table>
          <thead>
            <tr>
              <th>WO</th>
              <th>Title</th>
              <th>Site</th>
              <th>Priority</th>
              <th>Status</th>
              <th>SLA</th>
              <th>History</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan="7">No work order requests found.</td>
              </tr>
            )}
            {rows.map((w) => (
              <tr key={w.id}>
                <td>{w.workOrderNumber || "-"}</td>
                <td>{w.title}</td>
                <td>{w.site?.siteName || "-"}</td>
                <td>{w.priority}</td>
                <td>
                  <StatusBadge status={w.status} />
                </td>
                <td>
                  <SlaBadge workOrder={w} />
                </td>
                <td>
                  <button
                    type="button"
                    className="action-btn"
                    onClick={async () => {
                      try {
                        const r = await getWorkOrderHistory(w.id);
                        setHistoryWorkOrder(w);
                        setHistory(r.data || []);
                      } catch (err) {
                        alert(err.response?.data?.message || "Could not load history.");
                      }
                    }}
                  >
                    History
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {historyWorkOrder && (
          <StatusTimeline
            currentStatus={historyWorkOrder.status}
            history={history}
            workOrderNumber={historyWorkOrder.workOrderNumber}
            title={historyWorkOrder.title}
          />
        )}
      </section>
    </div>
  );
}
