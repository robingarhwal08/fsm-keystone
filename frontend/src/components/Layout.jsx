import { useState, useEffect, useRef } from "react";

import {
  Grid2X2,
  Users,
  MapPin,
  ClipboardList,
  Package,
  LogOut,
  Radio,
  Search,
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
  logout
}) {

  const [showMenu, setShowMenu] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const profileRef = useRef(null);

  useEffect(() => {

    function handleClickOutside(event) {

      if (
          profileRef.current &&
          !profileRef.current.contains(event.target)
      ) {
        setShowMenu(false);
      }

    }

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };

  }, []);

  const role = user?.role;

  let nav = [];

  // MANAGER
  if (role === "MANAGER") {
    nav = [
      ["dashboard", "Dashboard", Grid2X2],
      ["customers", "Customers", Users],
      ["sites", "Sites", MapPin],
      ["workorders", "Work Orders", ClipboardList],
      ["parts", "Inventory", Package],
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
      ["workorders", "Work Orders", ClipboardList]
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

          <div className="search">
            <Search size={20} />
            <span>Search work orders...</span>
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