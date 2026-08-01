export default function StatCard({ title, value, color = "blue" }) {
  return (
    <div className={`stat-card ${color}`}>
      <p>{title}</p>
      <h2>{value ?? 0}</h2>
    </div>
  );
}