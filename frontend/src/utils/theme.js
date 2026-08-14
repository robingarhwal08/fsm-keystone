export function getTheme() {
  return localStorage.getItem("fsm-theme") === "dark" ? "dark" : "light";
}

export function applyTheme(theme) {
  const next = theme === "dark" ? "dark" : "light";
  document.body.classList.toggle("dark", next === "dark");
  localStorage.setItem("fsm-theme", next);
  return next;
}
