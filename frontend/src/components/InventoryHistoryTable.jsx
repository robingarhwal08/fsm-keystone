import { formatDateTime } from "./StatusHistoryTable";

export default function InventoryHistoryTable({ usages = [], emptyText = "No inventory usage yet." }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Part</th>
          <th>Qty</th>
          <th>Status</th>
          <th>Work order</th>
          <th>Title</th>
          <th>Site</th>
          <th>Technician</th>
          <th>Date / time</th>
        </tr>
      </thead>
      <tbody>
        {usages.length === 0 && (
          <tr>
            <td colSpan="8">{emptyText}</td>
          </tr>
        )}
        {usages.map((row) => (
          <tr key={row.id}>
            <td>{row.partName || "-"}</td>
            <td>{row.quantityUsed ?? "-"}</td>
            <td>
              <span className={`badge ${row.usageStatus}`}>
                {row.usageStatus === "PENDING" ? "Pending" : "Part used"}
              </span>
            </td>
            <td>{row.workOrderNumber || "-"}</td>
            <td>{row.workOrderTitle || "-"}</td>
            <td>{row.siteName || "-"}</td>
            <td>{row.technicianName || row.usedBy || "-"}</td>
            <td>{formatDateTime(row.usedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
