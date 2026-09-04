import { useEffect, useMemo, useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import {
    createUser,
    getUsers,
    updateUser,
    deleteUser,
    approveUser,
    getCustomers
} from "../services/commonService";

const emptyForm = {
    fullName: "",
    email: "",
    password: "",
    phone: "",
    role: "DISPATCHER",
    active: true,
    customerId: "",
    newPassword: ""
};

const isUnlinkedCustomer = (user) =>
    user?.role === "CUSTOMER" && !user?.customerId;

export default function Users({ user: currentUser }) {
    const [users, setUsers] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [mode, setMode] = useState(null);
    const [selectedUser, setSelectedUser] = useState(null);
    const [form, setForm] = useState(emptyForm);
    const [search, setSearch] = useState("");
    const [roleFilter, setRoleFilter] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);

    const isAdmin = currentUser?.role === "ADMIN";

    const roleOptions = isAdmin
        ? [
            { value: "MANAGER", label: "Manager" },
            { value: "DISPATCHER", label: "Dispatcher" },
            { value: "TECHNICIAN", label: "Technician" },
            { value: "CUSTOMER", label: "Customer" }
        ]
        : [
            { value: "DISPATCHER", label: "Dispatcher" },
            { value: "TECHNICIAN", label: "Technician" },
            { value: "CUSTOMER", label: "Customer" }
        ];

    const loadUsers = async () => {
        try {
            const response = await getUsers();
            setUsers(response.data || []);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUsers();
        getCustomers()
            .then((response) => setCustomers(response.data || []))
            .catch(() => {});
    }, []);

    const filteredUsers = useMemo(() => {
        const query = search.trim().toLowerCase();
        return users.filter((user) => {
            const matchesSearch = !query || [
                user.fullName,
                user.email,
                user.phone,
                user.role,
                user.customerName
            ].some((value) => String(value || "").toLowerCase().includes(query));

            const matchesRole = !roleFilter || user.role === roleFilter;
            const matchesStatus =
                !statusFilter ||
                (statusFilter === "active" && user.active) ||
                ((statusFilter === "pending" || statusFilter === "inactive") && !user.active);

            return matchesSearch && matchesRole && matchesStatus;
        });
    }, [users, search, roleFilter, statusFilter]);

    const unlinkedCustomers = users.filter(isUnlinkedCustomer);
    const pendingUsers = users.filter((user) => user.active === false);

    const handleApprove = async (id) => {
        try {
            await approveUser(id);
            loadUsers();
        } catch (error) {
            alert(error.response?.data?.message || "Could not approve user.");
        }
    };

    const resetFormState = () => {
        setMode(null);
        setSelectedUser(null);
        setForm(emptyForm);
        setShowPassword(false);
        setShowNewPassword(false);
    };

    const openCreate = () => {
        setMode("create");
        setSelectedUser(null);
        setForm(emptyForm);
    };

    const openEdit = (user, focusLink = false) => {
        setSelectedUser(user);
        setMode(focusLink ? "link" : "edit");
        setForm({
            fullName: user.fullName || "",
            email: user.email || "",
            password: "",
            phone: user.phone || "",
            role: user.role || "DISPATCHER",
            active: user.active !== false,
            customerId: user.customerId ? String(user.customerId) : "",
            newPassword: ""
        });
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setForm((prev) => ({
            ...prev,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const validateCustomerLink = () => {
        if (form.role === "CUSTOMER" && !form.customerId) {
            alert("Please select the customer organisation this user belongs to.");
            return false;
        }
        return true;
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        if (!validateCustomerLink()) return;

        try {
            await createUser({
                fullName: form.fullName.trim(),
                email: form.email.trim(),
                password: form.password,
                phone: form.phone.trim() || null,
                role: form.role,
                active: form.active,
                customerId: form.customerId ? Number(form.customerId) : null
            });
            resetFormState();
            loadUsers();
        } catch (error) {
            alert(error.response?.data?.message || "Could not create user.");
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        if (!validateCustomerLink()) return;

        try {
            await updateUser(selectedUser.id, {
                fullName: form.fullName.trim(),
                phone: form.phone.trim() || null,
                role: form.role,
                active: form.active,
                customerId: form.customerId ? Number(form.customerId) : null,
                newPassword: form.newPassword || null
            });
            resetFormState();
            loadUsers();
        } catch (error) {
            alert(error.response?.data?.message || "Could not update user.");
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Delete this user?")) return;

        try {
            await deleteUser(id);
            loadUsers();
        } catch (error) {
            alert(error.response?.data?.message || "Could not delete user.");
        }
    };

    const linkingOnly = mode === "link";
    const creating = mode === "create";
    const editing = mode === "edit" || linkingOnly;

    if (loading) {
        return <div className="page"><h3>Loading users…</h3></div>;
    }

    return (
        <div className="page users-page">
            {isAdmin && pendingUsers.length > 0 && (
                <section className="panel">
                    <p className="error" style={{ margin: 0 }}>
                        {pendingUsers.length} account
                        {pendingUsers.length > 1 ? "s are" : " is"} waiting for approval.
                    </p>
                </section>
            )}

            {unlinkedCustomers.length > 0 && (
                <section className="panel">
                    <p className="error" style={{ margin: 0 }}>
                        {unlinkedCustomers.length} customer user
                        {unlinkedCustomers.length > 1 ? "s are" : " is"} not linked to an organisation.
                        Link each user to the correct customer so they can create requests and view their sites.
                    </p>
                </section>
            )}

            {(creating || editing) && (
                <form
                    className="panel"
                    onSubmit={creating ? handleCreate : handleUpdate}
                >
                    <h2>
                        {creating
                            ? "Create User"
                            : linkingOnly
                                ? `Link ${selectedUser?.fullName} to customer organisation`
                                : "Edit User"}
                    </h2>

                    {linkingOnly && (
                        <p className="users-help">
                            Choose which customer organisation this user belongs to.
                        </p>
                    )}

                    {!linkingOnly && (
                        <>
                            <label className="field-label">Full name</label>
                            <input
                                type="text"
                                name="fullName"
                                placeholder="Full name"
                                value={form.fullName}
                                onChange={handleChange}
                                required
                            />

                            {creating && (
                                <>
                                    <label className="field-label">Email</label>
                                    <input
                                        type="email"
                                        name="email"
                                        placeholder="Email"
                                        value={form.email}
                                        onChange={handleChange}
                                        required
                                    />

                                    <label className="field-label">Password</label>
                                    <div className="password-field">
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            name="password"
                                            placeholder="Password"
                                            value={form.password}
                                            onChange={handleChange}
                                            required
                                            minLength={6}
                                        />
                                        <button
                                            type="button"
                                            className="eye-btn"
                                            onClick={() => setShowPassword(!showPassword)}
                                        >
                                            {showPassword ? <FaEyeSlash /> : <FaEye />}
                                        </button>
                                    </div>
                                </>
                            )}

                            <label className="field-label">Phone</label>
                            <input
                                type="tel"
                                name="phone"
                                placeholder="Phone"
                                value={form.phone}
                                onChange={handleChange}
                            />

                            <label className="field-label">Role</label>
                            <select
                                name="role"
                                value={form.role}
                                onChange={handleChange}
                                required
                            >
                                {roleOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>

                            {!creating && (
                                <>
                                    <label className="field-label">Reset password (optional)</label>
                                    <div className="password-field">
                                        <input
                                            type={showNewPassword ? "text" : "password"}
                                            name="newPassword"
                                            placeholder="Leave blank to keep current password"
                                            value={form.newPassword}
                                            onChange={handleChange}
                                            minLength={6}
                                        />
                                        <button
                                            type="button"
                                            className="eye-btn"
                                            onClick={() => setShowNewPassword(!showNewPassword)}
                                        >
                                            {showNewPassword ? <FaEyeSlash /> : <FaEye />}
                                        </button>
                                    </div>
                                </>
                            )}

                            <label className="checkbox-row">
                                <input
                                    type="checkbox"
                                    name="active"
                                    checked={form.active}
                                    onChange={handleChange}
                                />
                                <span>Active user</span>
                            </label>
                        </>
                    )}

                    {form.role === "CUSTOMER" && (
                        <>
                            <label className="field-label" htmlFor="customerId">
                                Customer organisation {linkingOnly ? "(required)" : ""}
                            </label>
                            <select
                                id="customerId"
                                name="customerId"
                                value={form.customerId}
                                onChange={handleChange}
                                required
                            >
                                <option value="">Select customer organisation</option>
                                {customers.map((customer) => (
                                    <option key={customer.id} value={customer.id}>
                                        {customer.name}
                                    </option>
                                ))}
                            </select>
                        </>
                    )}

                    <div className="users-form-actions">
                        <button className="primary" type="submit">
                            {creating
                                ? "Create user"
                                : linkingOnly
                                    ? "Link organisation"
                                    : "Update user"}
                        </button>
                        <button
                            type="button"
                            className="action-btn cancel-btn"
                            onClick={resetFormState}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            <section className="panel">
                <div className="panel-head">
                    <h2>Users</h2>
                    {!creating && !editing && (
                        <button type="button" className="primary" onClick={openCreate}>
                            New user
                        </button>
                    )}
                </div>

                <div className="users-toolbar">
                    <input
                        className="users-search"
                        type="search"
                        placeholder="Search by name, email, phone, or role"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                    <select
                        value={roleFilter}
                        onChange={(e) => setRoleFilter(e.target.value)}
                    >
                        <option value="">All roles</option>
                        {roleOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                    >
                        <option value="">All statuses</option>
                        <option value="pending">Pending approval</option>
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                    </select>
                </div>

                <p className="users-count">
                    Showing {filteredUsers.length} of {users.length} users
                </p>

                <div className="users-table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Role</th>
                                <th>Customer organisation</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredUsers.length === 0 ? (
                                <tr>
                                    <td colSpan="7" style={{ textAlign: "center" }}>
                                        No users match your filters
                                    </td>
                                </tr>
                            ) : (
                                filteredUsers.map((user) => (
                                    <tr key={user.id}>
                                        <td>{user.fullName}</td>
                                        <td>{user.email}</td>
                                        <td>{user.phone || "-"}</td>
                                        <td>{user.role}</td>
                                        <td>
                                            {user.customerName ? (
                                                user.customerName
                                            ) : isUnlinkedCustomer(user) ? (
                                                <span className="badge AT_RISK">Not linked</span>
                                            ) : (
                                                "-"
                                            )}
                                        </td>
                                        <td>
                                            {user.active ? (
                                                "Active"
                                            ) : (
                                                <span className="badge PENDING">Pending approval</span>
                                            )}
                                        </td>
                                        <td className="users-actions">
                                            {isAdmin && !user.active && (
                                                <button
                                                    type="button"
                                                    className="action-btn edit-btn"
                                                    onClick={() => handleApprove(user.id)}
                                                >
                                                    Approve
                                                </button>
                                            )}
                                            {isUnlinkedCustomer(user) && (
                                                <button
                                                    type="button"
                                                    className="action-btn edit-btn"
                                                    onClick={() => openEdit(user, true)}
                                                >
                                                    Link
                                                </button>
                                            )}
                                            <button
                                                type="button"
                                                className="action-btn edit-btn"
                                                onClick={() => openEdit(user, false)}
                                            >
                                                Edit
                                            </button>
                                            <button
                                                type="button"
                                                className="action-btn delete-btn"
                                                onClick={() => handleDelete(user.id)}
                                            >
                                                Delete
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
