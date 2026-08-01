import { useState } from "react";

import Layout from "./components/Layout";

import Login from "./pages/Login";
import Signup from "./pages/Signup";

import Dashboard from "./pages/Dashboard";
import Customers from "./pages/Customers";
import Sites from "./pages/Sites";
import WorkOrders from "./pages/WorkOrders";
import Parts from "./pages/Parts";

import CustomerRequests from "./pages/CustomerRequests";
import CustomerSites from "./pages/CustomerSites";
import CustomerReports from "./pages/CustomerReports";

import MyWorkOrders from "./pages/MyWorkOrders";
import TimeLogs from "./pages/TimeLogs";
import PartUsage from "./pages/PartUsage";

export default function App() {

    const [page, setPage] = useState(
        localStorage.getItem("token")
            ? "dashboard"
            : "login"
    );

    const [user, setUser] = useState(
        JSON.parse(localStorage.getItem("user") || "null")
    );

    const logout = () => {

        localStorage.clear();

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

            case "parts":
                return <Parts />;

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
        >

            {renderPage()}

        </Layout>

    );
}