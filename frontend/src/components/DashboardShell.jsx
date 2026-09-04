export default function DashboardShell({
  themeKey,
  eyebrow,
  title,
  subtitle,
  icon: Icon,
  action,
  children
}) {
  return (
    <div className="page dashboard-page">
      <section className={`dash-hero dash-hero--${themeKey}`}>
        <div className="dash-hero-glow" aria-hidden="true" />
        <div className="dash-hero-icon">
          <Icon size={34} strokeWidth={1.75} />
        </div>
        <div className="dash-hero-copy">
          <p className="dash-hero-eyebrow">{eyebrow}</p>
          <h1>{title}</h1>
          {subtitle && <p className="dash-hero-sub">{subtitle}</p>}
        </div>
        {action && <div className="dash-hero-action">{action}</div>}
      </section>
      {children}
    </div>
  );
}
