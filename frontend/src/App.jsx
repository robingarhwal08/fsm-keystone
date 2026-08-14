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
export default function App() {

    const [page, setPage] = useState(
        localStorage.getItem("token")
            ? "dashboard"
            : "login"
    );

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

    const toggleTheme = () => {
        setTheme((prev) => applyTheme(prev === "dark" ? "light" : "dark"));
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

                    setPage("dashboard");

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
                return <Users />;

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

            case "reports":
                return (
                    <CustomerReports
                        user={user}
                    />
                );

            default:
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