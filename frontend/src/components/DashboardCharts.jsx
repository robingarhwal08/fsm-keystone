import { BAR_GRADIENTS } from "../utils/chartPalette";

function gradientId(label, index) {
  return `chart-grad-${index}-${label.replace(/\s+/g, "-").toLowerCase()}`;
}

export function DonutChart({ title, slices, subtitle }) {
  const total = slices.reduce((sum, s) => sum + (Number(s.value) || 0), 0);
  const size = 200;
  const stroke = 24;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  let offset = 0;

  if (total === 0) {
    return (
      <div className="chart-block">
        {title && <h2>{title}</h2>}
        {subtitle && <p className="chart-subtitle">{subtitle}</p>}
        <div className="chart-empty-state">
          <span className="chart-empty-ring" />
          <p className="chart-empty">Nothing to show yet</p>
          <small>Data will appear as work orders are created.</small>
        </div>
      </div>
    );
  }

  const activeSlices = slices.filter((s) => Number(s.value) > 0);

  return (
    <div className="chart-block">
      {title && <h2>{title}</h2>}
      {subtitle && <p className="chart-subtitle">{subtitle}</p>}
      <div className="donut-wrap">
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="donut-svg">
          <defs>
            {activeSlices.map((s, index) => {
              const id = gradientId(s.label, index);
              const [start, end] = s.gradient || [s.color, s.color];
              return (
                <linearGradient key={id} id={id} x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor={start} />
                  <stop offset="100%" stopColor={end} />
                </linearGradient>
              );
            })}
          </defs>
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--dash-chart-track, #eef2f7)"
            strokeWidth={stroke}
          />
          {activeSlices.map((s, index) => {
            const value = Number(s.value) || 0;
            const len = (value / total) * c;
            const dash = `${len} ${c - len}`;
            const id = gradientId(s.label, index);
            const el = (
              <circle
                key={s.label}
                cx={size / 2}
                cy={size / 2}
                r={r}
                fill="none"
                stroke={`url(#${id})`}
                strokeWidth={stroke}
                strokeDasharray={dash}
                strokeDashoffset={-offset}
                strokeLinecap="round"
                transform={`rotate(-90 ${size / 2} ${size / 2})`}
                className="donut-segment"
              />
            );
            offset += len;
            return el;
          })}
          <circle cx={size / 2} cy={size / 2} r={r - stroke / 2 - 4} className="donut-center-hole" />
          <text x="50%" y="47%" textAnchor="middle" className="donut-total">
            {total}
          </text>
          <text x="50%" y="61%" textAnchor="middle" className="donut-caption">
            total
          </text>
        </svg>
        <ul className="chart-legend chart-legend-colorful">
          {slices.map((s) => {
            const value = Number(s.value) || 0;
            return (
              <li key={s.label} className={value === 0 ? "is-zero" : ""}>
                <span
                  className="swatch swatch-lg"
                  style={{ background: s.gradient ? `linear-gradient(135deg, ${s.gradient[0]}, ${s.gradient[1]})` : s.color }}
                />
                <span className="legend-label">{s.label}</span>
                <b>{value}</b>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

export function SlaGauge({ percent = 0 }) {
  const value = Math.max(0, Math.min(100, Number(percent) || 0));
  const size = 200;
  const stroke = 18;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const filled = (value / 100) * c;
  const gradient =
    value >= 90
      ? ["#4ade80", "#16a34a"]
      : value >= 70
        ? ["#fcd34d", "#d97706"]
        : ["#fb7185", "#dc2626"];

  return (
    <div className="chart-block">
      <h2>SLA compliance</h2>
      <p className="chart-subtitle">On-time completion across active jobs</p>
      <div className="gauge-wrap">
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          <defs>
            <linearGradient id="sla-gauge-grad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor={gradient[0]} />
              <stop offset="100%" stopColor={gradient[1]} />
            </linearGradient>
          </defs>
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--dash-chart-track, #eef2f7)"
            strokeWidth={stroke}
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="url(#sla-gauge-grad)"
            strokeWidth={stroke}
            strokeLinecap="round"
            strokeDasharray={`${filled} ${c - filled}`}
            transform={`rotate(-90 ${size / 2} ${size / 2})`}
          />
          <text x="50%" y="50%" textAnchor="middle" className="gauge-value">
            {value}%
          </text>
        </svg>
      </div>
    </div>
  );
}

export function BarChart({ title, data = {}, subtitle }) {
  const entries = Object.entries(data);
  const max = Math.max(1, ...entries.map(([, n]) => Number(n) || 0));

  return (
    <div className="chart-block">
      {title && <h2>{title}</h2>}
      {subtitle && <p className="chart-subtitle">{subtitle}</p>}
      {entries.length === 0 ? (
        <div className="chart-empty-state">
          <p className="chart-empty">No data yet</p>
          <small>Breakdowns appear once assignments are made.</small>
        </div>
      ) : (
        <div className="bar-list">
          {entries.map(([label, count], index) => (
            <div key={label} className="bar-row">
              <span className="bar-label">{label}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    width: `${((Number(count) || 0) / max) * 100}%`,
                    background: BAR_GRADIENTS[index % BAR_GRADIENTS.length]
                  }}
                />
              </div>
              <span className="bar-count">{count}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function StatCards({ items }) {
  return (
    <div className="dash-stats">
      {items.map((item) => (
        <div key={item.label} className={`dash-stat ${item.tone || ""}`}>
          {item.icon && (
            <span
              className="dash-stat-icon"
              style={{
                color: item.iconColor || undefined,
                background: item.iconBg || undefined
              }}
            >
              {item.icon}
            </span>
          )}
          <div className="dash-stat-body">
            <span className="dash-stat-label">{item.label}</span>
            <strong className="dash-stat-value">{item.value ?? 0}</strong>
            {item.hint && <small className="dash-stat-hint">{item.hint}</small>}
          </div>
        </div>
      ))}
    </div>
  );
}

export function AlertPills({ items }) {
  return <StatCards items={items} />;
}

export function DashboardPanel({ title, subtitle, action, children, className = "" }) {
  return (
    <section className={`panel dash-panel ${className}`.trim()}>
      {(title || action) && (
        <div className="panel-head">
          <div>
            {title && <h2>{title}</h2>}
            {subtitle && <p className="dash-panel-sub">{subtitle}</p>}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}
