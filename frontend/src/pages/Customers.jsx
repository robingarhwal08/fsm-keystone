import { useEffect, useState } from "react";

import {
    createCustomer,
    getCustomers,
    updateCustomer,
    deleteCustomer
} from "../services/commonService";

export default function Customers() {

    const [rows, setRows] = useState([]);

    const [editingId, setEditingId] = useState(null);
    const [showForm, setShowForm] = useState(false);

    const emptyForm = {
        name: "",
        email: "",
        phone: "",
        billingAddress: ""
    };

    const [form, setForm] = useState(emptyForm);

    const loadCustomers = async () => {

        try {

            const response = await getCustomers();

            setRows(response.data || []);

        } catch (error) {

            console.error(
                "Failed to load customers",
                error
            );

        }
    };

    useEffect(() => {

        loadCustomers();

    }, []);

    const saveCustomer = async (e) => {

        e.preventDefault();

        try {

            if (editingId) {

                await updateCustomer(
                    editingId,
                    form
                );

            } else {

                await createCustomer(form);

            }

            setForm(emptyForm);

            setEditingId(null);
            setShowForm(false);

            loadCustomers();

        } catch (error) {
            alert(error.response?.data?.message || "Could not save customer.");
        }
    };

    const editCustomer = (customer) => {

        setEditingId(customer.id);
        setShowForm(true);

        setForm({
            name: customer.name || "",
            email: customer.email || "",
            phone: customer.phone || "",
            billingAddress:
                customer.billingAddress || ""
        });

    };

    const removeCustomer = async (id) => {

        console.log("Deleting Customer ID:", id);

        const confirmDelete = window.confirm(
            "Are you sure you want to delete this customer?"
        );

        if (!confirmDelete) return;

        try {

            const response =
                await deleteCustomer(id);

            console.log(
                "Delete Success:",
                response
            );

            await loadCustomers();

        } catch (error) {
            alert(error.response?.data?.message || "Could not delete customer.");
        }
    };

    const resetForm = () => {
        setEditingId(null);
        setShowForm(false);
        setForm(emptyForm);
    };

   return (

       <div className="page page-stack">

            {/* CUSTOMER FORM */}

            {(showForm || editingId) && (
            <form
                className="panel"
                onSubmit={saveCustomer}
            >

                <h2>
                    {
                        editingId
                            ? "Edit Customer"
                            : "Add Customer"
                    }
                </h2>

                <input
                    placeholder="Customer Name"
                    value={form.name}
                    onChange={(e) =>
                        setForm({
                            ...form,
                            name: e.target.value
                        })
                    }
                    required
                />

                <input
                    placeholder="Email"
                    type="email"
                    value={form.email}
                    onChange={(e) =>
                        setForm({
                            ...form,
                            email: e.target.value
                        })
                    }
                    required
                />

                <input
                    placeholder="Phone Number"
                    value={form.phone}
                    onChange={(e) =>
                        setForm({
                            ...form,
                            phone: e.target.value
                        })
                    }
                />

                <textarea
                    placeholder="Billing Address"
                    value={form.billingAddress}
                    onChange={(e) =>
                        setForm({
                            ...form,
                            billingAddress:
                                e.target.value
                        })
                    }
                />

                <div style={{ display: "flex", gap: "10px" }}>
                <button
                    type="submit"
                    className="primary"
                >
                    {
                        editingId
                            ? "Update Customer"
                            : "Save Customer"
                    }
                </button>

                <button
                    type="button"
                    className="action-btn cancel-btn"
                    onClick={resetForm}
                >
                    Cancel
                </button>
                </div>

            </form>
            )}

            {/* CUSTOMER LIST */}

            <section className="panel">

                <div className="panel-head">
                <h2>
                    Customer List
                </h2>
                {!showForm && !editingId && (
                    <button
                        type="button"
                        className="primary"
                        onClick={() => setShowForm(true)}
                    >
                        New Customer
                    </button>
                )}
                </div>

                <div className="list-table-wrap">
                <table className="list-table">

                    <thead>

                        <tr>

                            <th className="col-name">Name</th>

                            <th className="col-email">Email</th>

                            <th className="col-phone">Phone</th>

                            <th>Billing Address</th>

                            <th className="col-actions">Actions</th>

                        </tr>

                    </thead>

                    <tbody>

                        {rows.length === 0 ? (

                            <tr>

                                <td
                                    colSpan="5"
                                    style={{
                                        textAlign:
                                            "center"
                                    }}
                                >
                                    No customers found
                                </td>

                            </tr>

                        ) : (

                            rows.map((customer) => (

                                <tr
                                    key={customer.id}
                                >

                                    <td>
                                        {customer.name}
                                    </td>

                                    <td>
                                        {customer.email}
                                    </td>

                                    <td>
                                        {customer.phone}
                                    </td>

                                    <td>
                                        {
                                            customer.billingAddress
                                        }
                                    </td>

                                    <td className="table-actions-cell">
                                        <div className="table-actions">
                                        <button
                                            type="button"
                                            className="action-btn edit-btn"
                                            onClick={() => editCustomer(customer)}
                                        >
                                            Edit
                                        </button>

                                       <button
                                           type="button"
                                           className="action-btn delete-btn"
                                           onClick={() => removeCustomer(customer.id)}
                                       >
                                           Delete
                                       </button>
                                        </div>
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