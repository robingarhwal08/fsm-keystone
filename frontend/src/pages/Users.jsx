import { useEffect, useState } from "react";

import {
    getUsers,
    updateUser,
    deleteUser
} from "../services/commonService";

export default function Users() {

    const [users, setUsers] = useState([]);

    const [loading, setLoading] = useState(true);

    const [editing, setEditing] = useState(false);

    const [selectedUser, setSelectedUser] = useState(null);

    const [form, setForm] = useState({
        fullName: "",
        phone: "",
        role: "",
        active: true
    });

    const loadUsers = async () => {

        try {

            const response = await getUsers();

            setUsers(response.data);

        } catch (error) {

            console.log(error);

        } finally {

            setLoading(false);
        }

    };
    useEffect(() => {

        loadUsers();

    }, []);

    const handleDelete = async (id) => {

        if (!window.confirm("Delete this user?"))
            return;

        try {

            await deleteUser(id);

            loadUsers();

        } catch (error) {

            console.log(error);

        }

    };
    const handleEdit = (user) => {

        setSelectedUser(user);

        setForm({

            fullName: user.fullName,

            phone: user.phone,

            role: user.role,

            active: user.active

        });

        setEditing(true);

    };
    const handleChange = (e) => {

        const { name, value } = e.target;

        setForm({

            ...form,

            [name]: value

        });

    };
    const handleCheckbox = (e) => {

        setForm({

            ...form,

            active: e.target.checked

        });

    };
    const handleUpdate = async () => {

        try {

            await updateUser(selectedUser.id, form);

            setEditing(false);

            loadUsers();

        } catch (error) {

            console.log(error);

        }

    };
    if (loading)
        return <h3>Loading...</h3>;


    return (
        <div
            className="page"
            style={{
                display: "flex",
                flexDirection: "column",
                gap: "24px"
            }}
        >

            {/* Edit Card */}

            {
                editing && (

                    <form
                        className="panel"
                        onSubmit={(e) => {
                            e.preventDefault();
                            handleUpdate();
                        }}
                    >

                        <h2>Edit User</h2>

                        <input
                            type="text"
                            name="fullName"
                            placeholder="Full Name"
                            value={form.fullName}
                            onChange={handleChange}
                            disabled={!editing}
                        />

                        <input
                            type="text"
                            name="phone"
                            placeholder="Phone"
                            value={form.phone}
                            onChange={handleChange}
                            disabled={!editing}
                        />

                        <select
                            name="role"
                            value={form.role}
                            onChange={handleChange}
                            disabled={!editing}
                        >
                            <option value="">Select Role</option>
                            <option value="DISPATCHER">Dispatcher</option>
                            <option value="TECHNICIAN">Technician</option>
                            <option value="CUSTOMER">Customer</option>
                        </select>

                        <label className="checkbox-row">
                            <input
                                type="checkbox"
                                checked={form.active}
                                onChange={handleCheckbox}
                                disabled={!editing}
                            />
                            <span>Active User</span>
                        </label>

                        <div
                            style={{
                                display: "flex",
                                gap: "10px"
                            }}
                        >
                            <button
                                className="primary"
                                type="submit"
                                disabled={!editing}
                            >
                                Update User
                            </button>

                            <button
                                type="button"
                                className="action-btn cancel-btn"
                                onClick={() => {
                                    setEditing(false);
                                    setSelectedUser(null);
                                    setForm({
                                        fullName: "",
                                        phone: "",
                                        role: "",
                                        active: true
                                    });
                                }}
                            >
                                Cancel
                            </button>
                        </div>

                    </form>

                )
            }

            {/* Table Card */}

            <section className="panel">

                <h2>Users</h2>

                <table>

                    <thead>

                    <tr>

                        <th>Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Actions</th>

                    </tr>

                    </thead>

                    <tbody>

                    {
                        users.length === 0 ? (

                            <tr>

                                <td
                                    colSpan="6"
                                    style={{ textAlign: "center" }}
                                >
                                    No Users Found
                                </td>

                            </tr>

                        ) : (

                            users.map((user) => (

                                <tr key={user.id}>

                                    <td>{user.fullName}</td>

                                    <td>{user.email}</td>

                                    <td>{user.phone}</td>

                                    <td>{user.role}</td>

                                    <td>
                                        {user.active ? "Active" : "Inactive"}
                                    </td>

                                    <td>

                                        <button
                                            type="button"
                                            className="action-btn edit-btn"
                                            onClick={() => handleEdit(user)}
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

                        )
                    }

                    </tbody>

                </table>

            </section>

        </div>
    );

}