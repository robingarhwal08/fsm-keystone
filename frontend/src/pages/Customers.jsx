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

    const [form, setForm] = useState({
        name: "",
        email: "",
        phone: "",
        billingAddress: ""
    });

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

            setForm({
                name: "",
                email: "",
                phone: "",
                billingAddress: ""
            });

            setEditingId(null);

            loadCustomers();

        } catch (error) {

            console.error(
                "Customer save failed",
                error
            );

        }
    };

    const editCustomer = (customer) => {

        setEditingId(customer.id);

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

            console.error(
                "Delete Failed:",
                error.response || error
            );

        }
    };

   return (

       <div
           className="page"
           style={{
               display: "flex",
               flexDirection: "column",
               gap: "24px"
           }}
       >

            {/* CUSTOMER FORM */}

            <form
                className="panel"
                onSubmit={saveCustomer}
            >

                <h2>
                    {
                        editingId
                            ? "Edit Customer"
                            : "Create Customer"
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

                {
                    editingId && (
                       <button
                           type="button"
                           style={{marginLeft: "10px"}}
                           className="action-btn cancel-btn"
                           onClick={() => {
                               setEditingId(null);

                               setForm({
                                   name: "",
                                   email: "",
                                   phone: "",
                                   billingAddress: ""
                               });
                           }}
                       >
                           Cancel
                       </button>
                    )
                }

            </form>

            {/* CUSTOMER LIST */}

            <section className="panel">

                <h2>
                    Customer List
                </h2>

                <table>

                    <thead>

                        <tr>

                            <th>Name</th>

                            <th>Email</th>

                            <th>Phone</th>

                            <th>Billing Address</th>

                            <th>Actions</th>

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

                                    <td>

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

                                    </td>

                                </tr>

                            ))

                        )}

                    </tbody>

                </table>

            </section>

        </div>

    );
}