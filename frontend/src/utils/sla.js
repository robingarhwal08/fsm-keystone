export function getSlaView(workOrder) {
  const dueRaw = workOrder?.slaDueAt;
  const due = dueRaw ? new Date(dueRaw) : null;
  const status = (workOrder?.status || "").toUpperCase();
  const terminal = ["COMPLETED", "CLOSED", "CANCELLED"].includes(status);
  const endTime = workOrder?.actualEnd ? new Date(workOrder.actualEnd) : new Date();

  if (!due || Number.isNaN(due.getTime())) {
    return {
      slaStatus: "NO_SLA",
      dueLabel: "Due not set",
      remainLabel: "-",
      className: "ON_TRACK"
    };
  }

  const compare = terminal ? endTime : new Date();
  const remainingMs = due.getTime() - compare.getTime();
  const remainingMinutes = Math.round(remainingMs / 60000);
  let slaStatus = workOrder?.slaStatus || "ON_TRACK";

  if (remainingMs <= 0) {
    slaStatus = "BREACHED";
  } else if (!terminal) {
    const created = workOrder?.createdAt ? new Date(workOrder.createdAt) : null;
    const windowMs = created && !Number.isNaN(created.getTime())
      ? due.getTime() - created.getTime()
      : 24 * 3600 * 1000;
    if (remainingMs <= Math.max(60 * 60000, windowMs / 4)) {
      slaStatus = "AT_RISK";
    } else {
      slaStatus = "ON_TRACK";
    }
  } else if (remainingMs > 0) {
    slaStatus = "ON_TRACK";
  }

  const absMin = Math.abs(remainingMinutes);
  const hours = Math.floor(absMin / 60);
  const mins = absMin % 60;
  const clock = hours > 0 ? `${hours}h ${mins}m` : `${mins}m`;
  const remainLabel = remainingMinutes >= 0
    ? (terminal ? `Met with ${clock} left` : `${clock} left`)
    : `${clock} overdue`;

  return {
    slaStatus,
    dueLabel: due.toLocaleString(),
    remainLabel,
    className: slaStatus
  };
}
