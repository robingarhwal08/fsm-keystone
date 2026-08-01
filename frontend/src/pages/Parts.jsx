import { useEffect, useState } from "react";
import {
    getParts,
    createPart,
    updatePart,
    deletePart
} from "../services/commonService";

export default function Parts() {

  const [rows, setRows] = useState([]);
  const [editingId, setEditingId] = useState(null);

  const [f, setF] = useState({
    partName: "",
    partNumber: "",
    description: "",
    unitPrice: 0,
    stockQuantity: 0,
    active: true
  });

  const load = () =>
    getParts().then(r => setRows(r.data));

  useEffect(load, []);

  const save = async e => {

      e.preventDefault();

      if (editingId) {

          await updatePart(
              editingId,
              f
          );

      } else {

          await createPart(f);

      }

      setEditingId(null);

      setF({
          partName: "",
          partNumber: "",
          description: "",
          unitPrice: 0,
          stockQuantity: 0,
          active: true
      });

      load();
  };
const editPart = (part) => {

    setEditingId(part.id);

    setF({
        partName: part.partName || "",
        partNumber: part.partNumber || "",
        description: part.description || "",
        unitPrice: part.unitPrice || 0,
        stockQuantity: part.stockQuantity || 0,
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

        console.error(err);

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

      <form className="panel" onSubmit={save}>
        <h2>
            {editingId ? "Edit Part" : "Add Part"}
        </h2>

        <input
          placeholder="Part Name"
          value={f.partName}
          onChange={e => setF({ ...f, partName: e.target.value })}
        />

        <input
          placeholder="Part Number"
          value={f.partNumber}
          onChange={e => setF({ ...f, partNumber: e.target.value })}
        />

        <input
          placeholder="Description"
          value={f.description}
          onChange={e => setF({ ...f, description: e.target.value })}
        />

        <input
          type="number"
          placeholder="Unit Price"
          value={f.unitPrice}
          onChange={e => setF({ ...f, unitPrice: e.target.value })}
        />

        <input
          type="number"
          placeholder="Stock Qty"
          value={f.stockQuantity}
          onChange={e => setF({ ...f, stockQuantity: e.target.value })}
        />

        <button
            className="primary"
            type="submit"
        >
            {editingId ? "Update Part" : "Save Part"}
        </button>

      </form>

      <section className="panel">

        <h2>Parts Inventory</h2>

        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>No</th>
              <th>Price</th>
              <th>Stock</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {rows.map(p =>
              <tr key={p.id}>

                  <td>{p.partName}</td>

                  <td>{p.partNumber}</td>

                  <td>{p.unitPrice}</td>

                  <td>{p.stockQuantity}</td>

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

              </tr>
            )}
          </tbody>

        </table>

      </section>

    </div>
  );
}