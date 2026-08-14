import { useEffect, useState } from "react";
import {
    getParts,
    createPart,
    updatePart,
    deletePart
} from "../services/commonService";

const emptyForm = {
    partName: "",
    description: "",
    unitPrice: "",
    stockQuantity: "",
    active: true
};

export default function Parts({ user }) {

  const canManage = user?.role === "MANAGER";
  const [rows, setRows] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editingNumber, setEditingNumber] = useState("");

  const [f, setF] = useState(emptyForm);

  const load = () =>
    getParts().then(r => setRows(r.data));

  useEffect(() => {
    load();
  }, []);

  const save = async e => {

      e.preventDefault();
      try {
      const payload = {
          partName: f.partName,
          description: f.description,
          unitPrice: Number(f.unitPrice),
          stockQuantity: Number(f.stockQuantity),
          active: f.active
      };
      if (editingId) {
          await updatePart(editingId, payload);
      } else {
          await createPart(payload);
      }
      setEditingId(null);
      setEditingNumber("");
      setF(emptyForm);
      load();
      } catch (err) {
          alert(err.response?.data?.message || "Could not save part.");
      }
  };
const editPart = (part) => {

    setEditingId(part.id);
    setEditingNumber(part.partNumber || "");

    setF({
        partName: part.partName || "",
        description: part.description || "",
        unitPrice: part.unitPrice ?? "",
        stockQuantity: part.stockQuantity ?? "",
        active: part.active
    });

};
const removePart = async (id) => {

    const ok = window.confirm(
        "Are you sure you want to delete this part?"
    );

    if (!ok) return;

    try {

        await deletePart(id);

        load();

    } catch (err) {
        alert(err.response?.data?.message || "Could not delete part.");
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

      {canManage && (
      <form className="panel" onSubmit={save}>
        <h2>
            {editingId ? "Edit Part" : "Add Part"}
        </h2>

        <input
          placeholder="Part name"
          value={f.partName}
          onChange={e => setF({ ...f, partName: e.target.value })}
          required
        />

        {editingId && (
          <input
            value={editingNumber}
            readOnly
            title="Part number is generated automatically"
          />
        )}

        <input
          placeholder="Description"
          value={f.description}
          onChange={e => setF({ ...f, description: e.target.value })}
        />

        <input
          type="number"
          min="0"
          step="0.01"
          placeholder="Unit price"
          value={f.unitPrice}
          onChange={e => setF({ ...f, unitPrice: e.target.value })}
          required
        />

        <input
          type="number"
          min="0"
          step="1"
          placeholder="Stock quantity"
          value={f.stockQuantity}
          onChange={e => setF({ ...f, stockQuantity: e.target.value })}
          required
        />

        <button
            className="primary"
            type="submit"
        >
            {editingId ? "Update Part" : "Save Part"}
        </button>

      </form>
      )}

      <section className="panel">

        <h2>Parts Inventory</h2>

        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>No</th>
              <th>Price</th>
              <th>Stock</th>
              {canManage && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {rows.map(p =>
              <tr key={p.id}>

                  <td>{p.partName}</td>

                  <td>{p.partNumber}</td>

                  <td>{p.unitPrice}</td>

                  <td>{p.stockQuantity}</td>

                  {canManage && (
                  <td>

                      <button
                          type="button"
                          className="action-btn edit-btn"
                          onClick={() => editPart(p)}
                      >
                          Edit
                      </button>

                      <button
                          type="button"
                          className="action-btn delete-btn"
                          onClick={() => removePart(p.id)}
                      >
                          Delete
                      </button>

                  </td>
                  )}

              </tr>
            )}
          </tbody>

        </table>

      </section>

    </div>
  );
}
