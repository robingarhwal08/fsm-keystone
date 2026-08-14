export function DonutChart({ title, slices }) {
  const total = slices.reduce((sum, s) => sum + (Number(s.value) || 0), 0);
  const size = 180;
  const stroke = 22;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  let offset = 0;

  if (total === 0) {
    return (
      <div className="chart-block">
        {title && <h2>{title}</h2>}
        <p className="chart-empty">No data yet.</p>
      </div>
    );
  }

  return (
    <div className="chart-block">
      {title && <h2>{title}</h2>}
      <div className="donut-wrap">
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          {slices.map((s) => {
            const value = Number(s.value) || 0;
            if (value <= 0) {
              return null;
            }
            const len = (value / total) * c;
            const dash = `${len} ${c - len}`;
            const el = (
              <circle
                key={s.label}
                cx={size / 2}
                cy={size / 2}
                r={r}
                fill="none"
                stroke={s.color}
                strokeWidth={stroke}
                strokeDasharray={dash}
                strokeDashoffset={-offset}
                transform={`rotate(-90 ${size / 2} ${size / 2})`}
              />
            );
            offset += len;
            return el;
          })}
          <text
            x="50%"
            y="48%"
            textAnchor="middle"
            className="donut-total"
          >
            {total}
          </text>
          <text
            x="50%"
            y="62%"
            textAnchor="middle"
            className="donut-caption"
          >
            total
          </text>
        </svg>
        <ul className="chart-legend">
          {slices.map((s) => (
            <li key={s.label}>
              <span className="swatch" style={{ background: s.color }} />
              {s.label}
              <b>{s.value || 0}</b>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export function SlaGauge({ percent = 0 }) {
  const value = Math.max(0, Math.min(100, Number(percent) || 0));
  const size = 180;
  const stroke = 16;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const filled = (value / 100) * c;
  const color = value >= 90 ? "#7d8f6e" : value >= 70 ? "#d4a574" : "#c4785a";

  return (
    <div className="chart-block">
      <h2>SLA compliance</h2>
      <div className="gauge-wrap">
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="#e5e7eb"
            strokeWidth={stroke}
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke={color}
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

export function BarChart({ title, data = {} }) {
  const entries = Object.entries(data);
  const max = Math.max(1, ...entries.map(([, n]) => Number(n) || 0));

  return (
    <div className="chart-block">
      {title && <h2>{title}</h2>}
      {entries.length === 0 ? (
        <p className="chart-empty">No data yet.</p>
      ) : (
        <div className="bar-list">
          {entries.map(([label, count]) => (
            <div key={label} className="bar-row">
              <span className="bar-label">{label}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{ width: `${((Number(count) || 0) / max) * 100}%` }}
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

export function AlertPills({ items }) {
  return (
    <div className="alert-pills">
      {items.map((item) => (
        <div key={item.label} className={`alert-pill ${item.tone || ""}`}>
          <span>{item.label}</span>
          <strong>{item.value ?? 0}</strong>
        </div>
      ))}
    </div>
  );
}
