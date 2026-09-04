import { useEffect, useState } from "react";

import {
  createSite,
  getCustomers,
  getSites,
  updateSite,
  deleteSite
} from "../services/commonService";

export default function Sites() {

  const [rows, setRows] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const emptyForm = {
    siteName: "",
    address: "",
    city: "",
    state: "",
    pincode: "",
    contactPerson: "",
    contactPhone: "",
    customer: { id: "" }
  };

  const [f, setF] = useState(emptyForm);

  const load = () => {

    getSites().then((r) =>
      setRows(r.data || [])
    );

    getCustomers().then((r) =>
      setCustomers(r.data || [])
    );
  };

  useEffect(() => {
    load();
  }, []);

  const save = async (e) => {

    e.preventDefault();

    const payload = {
      ...f,
      customer: {
        id: +f.customer.id
      }
    };

    try {

      if (editingId) {

        await updateSite(
          editingId,
          payload
        );

      } else {

        await createSite(
          payload
        );

      }

      setEditingId(null);
      setShowForm(false);

      setF(emptyForm);

      load();

    } catch (error) {
      alert(error.response?.data?.message || "Could not save site.");
    }
  };

  const editSite = (site) => {

    setEditingId(site.id);
    setShowForm(true);

    setF({
      siteName: site.siteName || "",
      address: site.address || "",
      city: site.city || "",
      state: site.state || "",
      pincode: site.pincode || "",
      contactPerson:
        site.contactPerson || "",
      contactPhone:
        site.contactPhone || "",
      customer: {
        id:
          site.customer?.id || ""
      }
    });
  };

  const removeSite = async (id) => {

    const confirmDelete =
      window.confirm(
        "Are you sure you want to delete this site?"
      );

    if (!confirmDelete) return;

    try {

      await deleteSite(id);

      load();

    } catch (error) {
      alert(error.response?.data?.message || "Could not delete site.");
    }
  };

  const resetForm = () => {

    setEditingId(null);
    setShowForm(false);

    setF(emptyForm);
  };

  return (

    <div className="page page-stack">

      {(showForm || editingId) && (
      <form
        className="panel"
        onSubmit={save}
      >

        <h2>
          {
            editingId
              ? "Edit Site"
              : "Add Site"
          }
        </h2>

        <select
          value={f.customer.id}
          onChange={(e) =>
            setF({
              ...f,
              customer: {
                id: e.target.value
              }
            })
          }
          required
        >
          <option value="">
            Select Customer
          </option>

          {
            customers.map((c) => (
              <option
                key={c.id}
                value={c.id}
              >
                {c.name}
              </option>
            ))
          }

        </select>

        <input
          placeholder="Site Name"
          value={f.siteName}
          onChange={(e) =>
            setF({
              ...f,
              siteName:
                e.target.value
            })
          }
          required
        />

        <input
          placeholder="Address"
          value={f.address}
          onChange={(e) =>
            setF({
              ...f,
              address:
                e.target.value
            })
          }
        />

        <input
          placeholder="City"
          value={f.city}
          onChange={(e) =>
            setF({
              ...f,
              city:
                e.target.value
            })
          }
        />

        <input
          placeholder="State"
          value={f.state}
          onChange={(e) =>
            setF({
              ...f,
              state:
                e.target.value
            })
          }
        />

        <input
          placeholder="Pincode"
          value={f.pincode}
          onChange={(e) =>
            setF({
              ...f,
              pincode:
                e.target.value
            })
          }
        />

        <input
          placeholder="Contact Person"
          value={f.contactPerson}
          onChange={(e) =>
            setF({
              ...f,
              contactPerson:
                e.target.value
            })
          }
        />

        <input
          placeholder="Contact Phone"
          value={f.contactPhone}
          onChange={(e) =>
            setF({
              ...f,
              contactPhone:
                e.target.value
            })
          }
        />

        <div
          style={{
            display: "flex",
            gap: "10px"
          }}
        >

          <button
            type="submit"
            className="primary"
          >
            {
              editingId
                ? "Update Site"
                : "Save Site"
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

      <section className="panel">

        <div className="panel-head">
        <h2>Sites</h2>
        {!showForm && !editingId && (
          <button
            type="button"
            className="primary"
            onClick={() => setShowForm(true)}
          >
            New Site
          </button>
        )}
        </div>

        <div className="list-table-wrap">
        <table className="list-table">

          <thead>

            <tr>
              <th className="col-site">Site</th>
              <th className="col-customer">Customer</th>
              <th className="col-city">City</th>
              <th className="col-contact">Contact</th>
              <th className="col-actions">Actions</th>
            </tr>

          </thead>

          <tbody>

            {
              rows.map((s) => (

                <tr key={s.id}>

                  <td>
                    {s.siteName}
                  </td>

                  <td>
                    {s.customer?.name}
                  </td>

                  <td>
                    {s.city}
                  </td>

                  <td>
                    {s.contactPerson}
                  </td>

                  <td className="table-actions-cell">
                    <div className="table-actions">
                    <button
                      type="button"
                      className="action-btn edit-btn"
                      onClick={() =>
                        editSite(s)
                      }
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      className="action-btn delete-btn"
                      onClick={() =>
                        removeSite(s.id)
                      }
                    >
                      Delete
                    </button>
                    </div>
                  </td>

                </tr>

              ))
            }

          </tbody>

        </table>
        </div>

      </section>

    </div>
  );
}