import { useState, useEffect, useRef } from "react";
import { getNotifications, markNotificationRead } from "../services/commonService";
import ThemeToggle from "./ThemeToggle";

import {
  Grid2X2,
  Users,
  MapPin,
  ClipboardList,
  Package,
  LogOut,
  Radio,
  Bell,
  UserCog,
  FileText,
  Clock,
  Wrench,
  Menu,
  X
} from "lucide-react";

export default function Layout({
  children,
  page,
  setPage,
  user,
  logout,
  theme,
  toggleTheme
}) {

  const [showMenu, setShowMenu] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notes, setNotes] = useState([]);
  const [showNotes, setShowNotes] = useState(false);
  const profileRef = useRef(null);

  const notesRef = useRef(null);

  const loadNotes = () => {
    getNotifications()
      .then((r) => setNotes(Array.isArray(r.data) ? r.data : []))
      .catch(() => setNotes([]));
  };

  useEffect(() => {

    function handleClickOutside(event) {

      if (
          profileRef.current &&
          !profileRef.current.contains(event.target)
      ) {
        setShowMenu(false);
      }

      if (
          notesRef.current &&
          !notesRef.current.contains(event.target)
      ) {
        setShowNotes(false);
      }

    }

    document.addEventListener("mousedown", handleClickOutside);

    loadNotes();
    const timer = setInterval(loadNotes, 10000);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      clearInterval(timer);
    };

  }, [user]);

  const role = user?.role;

  let nav = [];

  // MANAGER
  if (role === "MANAGER") {
    nav = [
      ["dashboard", "Dashboard", Grid2X2],
      ["customers", "Customers", Users],
      ["sites", "Sites", MapPin],
      ["workorders", "Work Orders", ClipboardList],
      ["board", "Board", ClipboardList],
      ["parts", "Inventory", Package],
      ["partusage", "Part Usage", Wrench],
      ["timelogs", "Time Logs", Clock],
      ["users", "Users", UserCog],
//       ["reports", "Reports", FileText]
    ];
  }

  // DISPATCHER
  else if (role === "DISPATCHER") {
    nav = [
      ["dashboard", "Dashboard", Grid2X2],
//       ["customers", "Customers", Users],
      ["sites", "Sites", MapPin],
      ["workorders", "Work Orders", ClipboardList],
      ["board", "Board", ClipboardList],
      ["parts", "Inventory", Package],
      ["partusage", "Part Usage", Wrench],
      ["timelogs", "Time Logs", Clock]
    ];
  }

  // TECHNICIAN
  else if (role === "TECHNICIAN") {
    nav = [
      ["dashboard", "Dashboard", Grid2X2],
      ["myworkorders", "My Jobs", ClipboardList],
      ["timelogs", "Time Logs", Clock],
      ["partusage", "Part Usage", Wrench]
    ];
  }

  // CUSTOMER
  else {
    nav = [
      ["dashboard", "Dashboard", Grid2X2],
      ["requests", "Create Request", ClipboardList],
      ["myrequests", "My Requests", ClipboardList]
//       ["mysites", "My Sites", MapPin],
//       ["reports", "Reports", FileText]
    ];
  }

  return (
    <div className="app-shell">

      {/* Sidebar */}

      <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>

        <div className="brand">
          <span className="brand-mark">FSM</span>
          <b>
            KEY <span>STONE</span>
          </b>
        </div>

        {
          nav.map(([id, label, Icon]) => (
            <button
              key={id}
              onClick={() => {
                setPage(id);
                setSidebarOpen(false);
              }}
              className={page === id ? "active" : ""}
            >
              <Icon size={22} />
              <span>{label}</span>
            </button>
          ))
        }

        <button
            onClick={()=>{
              logout();
              setSidebarOpen(false);
            }}
          className="logout"
        >
          <LogOut size={22} />
          <span>Logout</span>
        </button>

      </aside>
      {sidebarOpen && (
          <div
              className="overlay"
              onClick={() => {
                setSidebarOpen(!sidebarOpen);
                setShowMenu(false);
              }}
          />
      )}

      {/* Main */}

      <main className="main">

        <header className="topbar">
          <button
              className="menu-btn"
              onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            {
              sidebarOpen
                  ? <X size={24}/>
                  : <Menu size={24}/>
            }
          </button>

          <div>
            <h2>Field Service Management System</h2>

           <p>
             Connecting customers, teams, and field operations through one intelligent service platform.
           </p>
          </div>

          <ThemeToggle theme={theme} onToggle={toggleTheme} />

          <div className="notes-wrap" ref={notesRef}>
            <button
              type="button"
              className="notes-btn"
              onClick={() => setShowNotes(!showNotes)}
            >
              <Bell size={18} />
              {notes.filter((n) => !n.readFlag).length > 0 && (
                <em>{notes.filter((n) => !n.readFlag).length}</em>
              )}
            </button>
            {showNotes && (
              <div className="notes-menu">
                <strong className="notes-title">Notifications</strong>
                {notes.length === 0 && <p>No notifications</p>}
                {notes.slice(0, 12).map((n) => (
                  <button
                    key={n.id}
                    type="button"
                    className={n.readFlag ? "read" : ""}
                    onClick={async () => {
                      await markNotificationRead(n.id);
                      setNotes((prev) => prev.map((x) => x.id === n.id ? { ...x, readFlag: true } : x));
                    }}
                  >
                    <span>{n.message}</span>
                    {n.workOrderNumber && <small>{n.workOrderNumber} · {n.type}</small>}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="live-pill">
            <Radio size={18} />
            Live Ops
          </div>

          {/* Profile Section */}

          <div
              className="avatar-container"
              ref={profileRef}
          >

            <div
              className="avatar"
              onClick={() => setShowMenu(!showMenu)}
            >
              {user?.fullName?.[0] || "U"}
            </div>

            {
              showMenu &&
              (
                <div className="profile-menu">

                  <div className="profile-info">

                    <strong>
                      {user?.fullName}
                    </strong>

                    <small>
                      {user?.email}
                    </small>

                    <small>
                      Role : {user?.role}
                    </small>

                  </div>

                  <button
                      onClick={() => {
                        logout();
                        setSidebarOpen(false);
                        setShowMenu(false);
                      }}
                  >
                    Sign Out
                  </button>

                </div>
              )
            }

          </div>

        </header>

        {children}

      </main>

    </div>
  );
}