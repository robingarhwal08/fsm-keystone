import { getSlaView } from "../utils/sla";

export default function SlaBadge({ workOrder }) {
  const sla = getSlaView(workOrder);
  return (
    <div className="sla-cell">
      <span className={`badge ${sla.className}`}>{sla.slaStatus.replace("_", " ")}</span>
      <small>Due {sla.dueLabel}</small>
      <small>{sla.remainLabel}</small>
    </div>
  );
}
