export function downloadRequestReport(rows = [], user) {
  const when = new Date().toLocaleString();
  const body = (rows || [])
    .map(
      (w) => `
      <tr>
        <td>${esc(w.workOrderNumber)}</td>
        <td>${esc(w.title)}</td>
        <td>${esc(w.site?.siteName)}</td>
        <td>${esc(w.priority)}</td>
        <td>${esc(w.status)}</td>
        <td>${esc(w.slaStatus || w.slaDueAt || "")}</td>
        <td>${esc(formatDate(w.createdAt))}</td>
      </tr>`
    )
    .join("");

  const html = `<!DOCTYPE html>
<html>
<head>
  <title>My Requests Report</title>
  <style>
    body { font-family: Arial, sans-serif; padding: 24px; color: #111; }
    h1 { margin: 0 0 8px; }
    p { color: #555; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th, td { border: 1px solid #ddd; padding: 8px; font-size: 12px; text-align: left; }
    th { background: #f3f4f6; }
  </style>
</head>
<body>
  <h1>FSM Keystone — My Requests Report</h1>
  <p>Customer: ${esc(user?.fullName || user?.email || "Customer")} · Generated: ${esc(when)}</p>
  <table>
    <thead>
      <tr>
        <th>Work order</th>
        <th>Title</th>
        <th>Site</th>
        <th>Priority</th>
        <th>Status</th>
        <th>SLA</th>
        <th>Created</th>
      </tr>
    </thead>
    <tbody>${body || `<tr><td colspan="7">No requests</td></tr>`}</tbody>
  </table>
  <script>window.onload = function () { window.print(); }</script>
</body>
</html>`;

  const win = window.open("", "_blank");
  if (!win) {
    alert("Please allow pop-ups to download the PDF report.");
    return;
  }
  win.document.write(html);
  win.document.close();
}

function esc(value) {
  return String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString();
}
