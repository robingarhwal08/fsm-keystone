import { formatDateTime } from "./StatusHistoryTable";

const PIPELINE = ["NEW", "ASSIGNED", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CLOSED"];

function canonical(status) {
  if (!status) {
    return "NEW";
  }
  return status === "CREATED" ? "NEW" : status;
}

export default function StatusTimeline({
  currentStatus,
  history = [],
  workOrderNumber = "",
  title = ""
}) {
  const current = canonical(currentStatus);
  const cancelled = current === "CANCELLED";
  const includeHold =
    current === "ON_HOLD" ||
    history.some((h) => canonical(h.newStatus) === "ON_HOLD");
  const steps = (cancelled ? [...PIPELINE, "CANCELLED"] : PIPELINE).filter(
    (step) => step !== "ON_HOLD" || includeHold
  );

  const reachedAt = {};
  history.forEach((h) => {
    const status = canonical(h.newStatus);
    reachedAt[status] = h.changedAt;
  });
  if (!reachedAt[current] && history.length === 0) {
    reachedAt[current] = null;
  }

  const currentIndex = steps.indexOf(current);

  return (
    <div className="status-timeline">
      <h3>Status progress</h3>
      <p>
        <strong>{workOrderNumber || "-"}</strong>
        {title ? ` — ${title}` : ""}
      </p>
      <div className="timeline-track">
        {steps.map((step, index) => {
          const isCurrent = step === current;
          const isPast = currentIndex >= 0 && index < currentIndex && !cancelled;
          const isDoneCancelled = cancelled && step !== "CANCELLED" && reachedAt[step];
          const showDot = isCurrent;
          const filled = isPast || isDoneCancelled || isCurrent;
          return (
            <div key={step} className="timeline-step">
              {index > 0 && (
                <div className={`timeline-line ${filled ? "filled" : ""}`} />
              )}
              <div
                className={`timeline-node ${filled ? "filled" : ""} ${showDot ? "current" : ""}`}
              />
              <span className={`timeline-label ${showDot ? "current" : ""}`}>
                {step.replaceAll("_", " ")}
              </span>
              <span className="timeline-time">
                {reachedAt[step] ? formatDateTime(reachedAt[step]) : isCurrent ? "Current" : ""}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
