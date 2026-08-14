export function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value).replace("T", " ").slice(0, 19);
  }
  return date.toLocaleString();
}

export default function StatusHistoryTable({
  rows = [],
  workOrderNumber = "",
  title = ""
}) {
  return (
    <div className="history-box">
      <h3>Status history</h3>
      <p>
        <strong>{workOrderNumber || "-"}</strong>
        {title ? ` — ${title}` : ""}
      </p>
      <table>
        <thead>
          <tr>
            <th>Work order</th>
            <th>Title</th>
            <th>Changed by</th>
            <th>What changed</th>
            <th>Updated time</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td colSpan="5">No history yet for this work order.</td>
            </tr>
          )}
          {rows.map((h) => (
            <tr key={h.id}>
              <td>{workOrderNumber || h.workOrderNumber || "-"}</td>
              <td>{title || h.title || "-"}</td>
              <td>{h.changedBy || "System"}</td>
              <td>
                {h.oldStatus || "—"} → {h.newStatus || "—"}
                {h.comment ? ` (${h.comment})` : ""}
              </td>
              <td>{formatDateTime(h.changedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
