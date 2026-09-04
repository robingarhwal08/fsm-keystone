export const STATUS_COLORS = {
  NEW: "#94a3b8",
  CREATED: "#64748b",
  ASSIGNED: "#f59e0b",
  IN_PROGRESS: "#2563eb",
  ON_HOLD: "#d97706",
  COMPLETED: "#16a34a",
  CLOSED: "#059669",
  CANCELLED: "#ef4444"
};

export const STATUS_GRADIENTS = {
  NEW: ["#cbd5e1", "#94a3b8"],
  CREATED: ["#94a3b8", "#64748b"],
  ASSIGNED: ["#fcd34d", "#d97706"],
  IN_PROGRESS: ["#60a5fa", "#2563eb"],
  ON_HOLD: ["#fdba74", "#ea580c"],
  COMPLETED: ["#4ade80", "#15803d"],
  CLOSED: ["#2dd4bf", "#0f766e"],
  CANCELLED: ["#fb7185", "#dc2626"]
};

export const WORK_ORDER_MIX = [
  { key: "createdWorkOrders", label: "New", color: "#94a3b8", gradient: ["#cbd5e1", "#64748b"] },
  { key: "assignedWorkOrders", label: "Assigned", color: "#f59e0b", gradient: ["#fcd34d", "#d97706"] },
  { key: "inProgressWorkOrders", label: "In progress", color: "#2563eb", gradient: ["#60a5fa", "#2563eb"] },
  { key: "onHoldWorkOrders", label: "On hold", color: "#8b5cf6", gradient: ["#c4b5fd", "#7c3aed"] },
  { key: "completedWorkOrders", label: "Completed", color: "#16a34a", gradient: ["#4ade80", "#15803d"] }
];

export const JOB_BREAKDOWN_SLICES = {
  inProgress: { label: "In progress", color: "#2563eb", gradient: ["#60a5fa", "#2563eb"] },
  completed: { label: "Completed", color: "#16a34a", gradient: ["#4ade80", "#15803d"] },
  pending: { label: "Pending", color: "#f59e0b", gradient: ["#fcd34d", "#d97706"] }
};

export const STAT_CARD_THEMES = [
  { iconColor: "#2563eb", iconBg: "#dbeafe" },
  { iconColor: "#0891b2", iconBg: "#cffafe" },
  { iconColor: "#16a34a", iconBg: "#dcfce7" },
  { iconColor: "#d97706", iconBg: "#fef3c7" },
  { iconColor: "#db2777", iconBg: "#fce7f3" },
  { iconColor: "#7c3aed", iconBg: "#ede9fe" }
];

export const BAR_GRADIENTS = [
  "linear-gradient(90deg, #60a5fa, #2563eb)",
  "linear-gradient(90deg, #4ade80, #16a34a)",
  "linear-gradient(90deg, #fcd34d, #f59e0b)",
  "linear-gradient(90deg, #c084fc, #9333ea)",
  "linear-gradient(90deg, #fb7185, #e11d48)",
  "linear-gradient(90deg, #2dd4bf, #0d9488)",
  "linear-gradient(90deg, #f472b6, #db2777)",
  "linear-gradient(90deg, #818cf8, #4f46e5)"
];

export function withStatTheme(items) {
  return items.map((item, index) => {
    const theme = STAT_CARD_THEMES[index % STAT_CARD_THEMES.length];
    if (item.tone === "accent") {
      return { ...item, iconColor: theme.iconColor, iconBg: theme.iconBg };
    }
    return {
      ...item,
      iconColor: item.iconColor || theme.iconColor,
      iconBg: item.iconBg || theme.iconBg
    };
  });
}

export function sliceWithGradient(config, value) {
  return {
    label: config.label,
    value,
    color: config.color,
    gradient: config.gradient
  };
}
