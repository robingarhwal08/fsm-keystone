import { useEffect, useRef, useState } from "react";

const HOURS = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, "0"));
const MINUTES = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, "0"));

function toDraft(value) {
  if (!value) {
    return {
      date: "",
      hour: "12",
      minute: "00",
      period: "PM"
    };
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return {
      date: "",
      hour: "12",
      minute: "00",
      period: "PM"
    };
  }

  let hours = parsed.getHours();
  const period = hours >= 12 ? "PM" : "AM";
  hours = hours % 12 || 12;

  const pad = (n) => String(n).padStart(2, "0");

  return {
    date: `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}`,
    hour: String(hours).padStart(2, "0"),
    minute: pad(parsed.getMinutes()),
    period
  };
}

function draftToValue(draft) {
  if (!draft.date) {
    return "";
  }

  let hour = Number(draft.hour) % 12;
  if (draft.period === "PM") {
    hour += 12;
  }

  const pad = (n) => String(n).padStart(2, "0");
  const localValue = `${draft.date}T${pad(hour)}:${draft.minute}`;
  const parsed = new Date(localValue);

  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  return localValue;
}

function formatDisplay(value) {
  if (!value) {
    return "";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  return parsed.toLocaleString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export default function DateTimePicker({
  label,
  value,
  onChange,
  required = false,
  placeholder = "Select date and time"
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(() => toDraft(value));
  const rootRef = useRef(null);

  useEffect(() => {
    if (open) {
      setDraft(toDraft(value));
    }
  }, [open, value]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const apply = () => {
    const next = draftToValue(draft);
    if (!next) {
      alert("Please select a date and time.");
      return;
    }
    onChange(next);
    setOpen(false);
  };

  const clear = () => {
    onChange("");
    setDraft(toDraft(""));
    setOpen(false);
  };

  const setToday = () => {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    const localValue = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
    setDraft(toDraft(localValue));
  };

  return (
    <div className="datetime-picker" ref={rootRef}>
      {label && <label>{label}</label>}

      <button
        type="button"
        className={`datetime-picker-trigger ${!value ? "is-empty" : ""}`}
        onClick={() => setOpen((current) => !current)}
        aria-required={required}
      >
        {formatDisplay(value) || placeholder}
      </button>

      {open && (
        <div className="datetime-picker-popover">
          <input
            type="date"
            value={draft.date}
            onChange={(e) => setDraft({ ...draft, date: e.target.value })}
          />

          <div className="datetime-picker-time">
            <select
              value={draft.hour}
              onChange={(e) => setDraft({ ...draft, hour: e.target.value })}
              aria-label="Hour"
            >
              {HOURS.map((hour) => (
                <option key={hour} value={hour}>
                  {hour}
                </option>
              ))}
            </select>

            <select
              value={draft.minute}
              onChange={(e) => setDraft({ ...draft, minute: e.target.value })}
              aria-label="Minute"
            >
              {MINUTES.map((minute) => (
                <option key={minute} value={minute}>
                  {minute}
                </option>
              ))}
            </select>

            <div className="datetime-picker-period">
              {["AM", "PM"].map((period) => (
                <button
                  key={period}
                  type="button"
                  className={draft.period === period ? "active" : ""}
                  onClick={() => setDraft({ ...draft, period })}
                >
                  {period}
                </button>
              ))}
            </div>
          </div>

          <div className="datetime-picker-actions">
            <button type="button" className="action-btn cancel-btn" onClick={clear}>
              Clear
            </button>
            <button type="button" className="action-btn edit-btn" onClick={setToday}>
              Today
            </button>
            <button type="button" className="primary datetime-picker-ok" onClick={apply}>
              OK
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
