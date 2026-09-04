import { useState, useEffect } from "react";
import { applyTheme, getTheme } from "./utils/theme";

import Layout from "./components/Layout";

import Login from "./pages/Login";
import Signup from "./pages/Signup";

import Dashboard from "./pages/Dashboard";
import Customers from "./pages/Customers";
import Sites from "./pages/Sites";
import WorkOrders from "./pages/WorkOrders";
import Board from "./pages/Board";
import Parts from "./pages/Parts";

import CustomerRequests from "./pages/CustomerRequests";
import CustomerSites from "./pages/CustomerSites";
import CustomerReports from "./pages/CustomerReports";

import MyWorkOrders from "./pages/MyWorkOrders";
import TimeLogs from "./pages/TimeLogs";
import PartUsage from "./pages/PartUsage";

import Users from "./pages/Users";
import Settings from "./pages/Settings";

const getInitialPage = () => {
    if (!localStorage.getItem("token")) {
        return "login";
    }

    const storedUser = JSON.parse(localStorage.getItem("user") || "null");
    if (storedUser?.role === "ADMIN") {
        return "users";
    }

    return "dashboard";
};

const ADMIN_PAGES = new Set(["users", "settings"]);

export default function App() {

    const [page, setPage] = useState(getInitialPage);

    const [user, setUser] = useState(
        JSON.parse(localStorage.getItem("user") || "null")
    );

    const [theme, setTheme] = useState(getTheme);

    useEffect(() => {
        if (page === "login" || page === "signup") {
            document.body.classList.remove("dark");
        } else {
            applyTheme(theme);
        }
    }, [page, theme]);

    useEffect(() => {
        if (user?.role === "ADMIN" && !ADMIN_PAGES.has(page)) {
            setPage("users");
        }
    }, [user, page]);

    const toggleTheme = () => {
        setTheme((prev) => applyTheme(prev === "dark" ? "light" : "dark"));
    };

    const updateProfile = (updates) => {
        setUser((prev) => {
            const next = { ...prev, ...updates };
            localStorage.setItem("user", JSON.stringify(next));
            return next;
        });
    };

    const logout = () => {

        localStorage.removeItem("token");
        localStorage.removeItem("user");

        setUser(null);

        setPage("login");
    };

    if (page === "login") {

        return (
            <Login
                onLogin={(u) => {

                    setUser(u);

                    setPage(u.role === "ADMIN" ? "users" : "dashboard");

                }}
                goSignup={() => setPage("signup")}
            />
        );
    }

    if (page === "signup") {

        return (
            <Signup
                onSignup={(u) => {

                    setUser(u);

                    setPage("dashboard");

                }}
                goLogin={() => setPage("login")}
            />
        );
    }

    const renderPage = () => {
        if (user?.role === "ADMIN" && page === "dashboard") {
            return <Users user={user} />;
        }

        switch (page) {

            case "dashboard":
                return (
                    <Dashboard
                        user={user}
                        setPage={setPage}
                    />
                );
            case "myworkorders":
                return (
                    <MyWorkOrders
                        user={user}
                    />
                );

            case "users":
                return <Users user={user} />;

            case "timelogs":
                return (
                    <TimeLogs
                        user={user}
                    />
                );

            case "partusage":
                return (
                    <PartUsage
                        user={user}
                    />
                );

            case "customers":
                return <Customers />;

            case "sites":
                return <Sites />;

            case "workorders":
                return (
                    <WorkOrders
                        user={user}
                    />
                );

            case "board":
                return <Board />;

            case "myrequests":
                return (
                    <CustomerReports
                        user={user}
                    />
                );

            case "parts":
                return <Parts user={user} />;

            case "requests":
                return (
                    <CustomerRequests
                        user={user}
                    />
                );

            case "mysites":
                return (
                    <CustomerSites />
                );

            case "settings":
                return (
                    <Settings
                        user={user}
                        onProfileUpdated={updateProfile}
                    />
                );

            case "reports":
                return (
                    <CustomerReports
                        user={user}
                    />
                );

            default:
                if (user?.role === "ADMIN") {
                    return <Users user={user} />;
                }
                return (
                    <Dashboard
                        user={user}
                        setPage={setPage}
                    />
                );
        }
    };

    return (

        <Layout
            page={page}
            setPage={setPage}
            user={user}
            logout={logout}
            theme={theme}
            toggleTheme={toggleTheme}
        >

            {renderPage()}

        </Layout>

    );
}