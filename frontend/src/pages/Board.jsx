import { useEffect, useState } from "react";
import { getWorkOrderBoard } from "../services/commonService";
import StatusBadge from "../components/StatusBadge";
import SlaBadge from "../components/SlaBadge";

const COLUMNS = ["NEW", "ASSIGNED", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CLOSED", "CANCELLED"];

export default function Board() {
  const [board, setBoard] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getWorkOrderBoard()
      .then((r) => setBoard(r.data || {}))
      .catch(() => setBoard({}))
      .finally(() => setLoading(false));
  }, []);

  const itemsFor = (status) => {
    const current = board[status] || [];
    if (status === "NEW") {
      return [...current, ...(board.CREATED || [])];
    }
    return current;
  };

  if (loading) {
    return <div className="page"><section className="panel"><p>Loading board...</p></section></div>;
  }

  return (
    <div className="page">
      <section className="panel">
        <h2>Work Order Board</h2>
        <p>Kanban by status. SLA due dates and breach status are shown on each card.</p>
      </section>
      <div className="kanban">
        {COLUMNS.map((status) => {
          const items = itemsFor(status);
          return (
            <div key={status} className="kanban-col">
              <h3>{status} <span>{items.length}</span></h3>
              {items.length === 0 && <p className="empty">No jobs</p>}
              {items.map((w) => (
                <article key={w.id} className="kanban-card">
                  <strong>{w.workOrderNumber}</strong>
                  <p>{w.title}</p>
                  <StatusBadge status={w.status} />
                  <SlaBadge workOrder={w} />
                </article>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}
