import { useEffect, useState } from "react";
import { createWorkOrder, getSites } from "../services/commonService";

export default function CustomerRequests({ user }) {
  const [sites, setSites] = useState([]);
  const [f, setF] = useState({
    title: "",
    description: "",
    siteId: "",
    priority: "MEDIUM"
  });

  const getCustomerId = () => user?.customerId || user?.customer?.id || null;

  useEffect(() => {
    getSites().then((r) => {
      const customerId = getCustomerId();
      const data = r.data || [];
      setSites(
        customerId
          ? data.filter((s) => s.customer?.id === customerId || s.customerId === customerId)
          : data
      );
    });
  }, [user]);

  const save = async (e) => {
    e.preventDefault();
    const selectedSite = sites.find((s) => String(s.id) === String(f.siteId));
    const customerId =
      getCustomerId() ||
      selectedSite?.customer?.id ||
      selectedSite?.customerId ||
      null;

    if (!f.title || !f.siteId) {
      alert("Please fill title and site.");
      return;
    }

    try {
      await createWorkOrder({
        title: f.title,
        description: f.description,
        customerId: customerId ? Number(customerId) : undefined,
        siteId: Number(f.siteId),
        priority: f.priority || "MEDIUM",
        createdByUserId: user?.userId
      });
      setF({
        title: "",
        description: "",
        siteId: "",
        priority: "MEDIUM"
      });
      alert("Work order request created successfully.");
    } catch (err) {
      alert(err.response?.data?.message || "Could not create request.");
    }
  };

  return (
    <div className="page">
      <form className="panel wo-form" onSubmit={save}>
        <h2>Create Customer Request</h2>
        <p>
          Submit a new service request. Your request will be reviewed and assigned
          to a technician.
        </p>
        <input
          placeholder="Request Title"
          value={f.title}
          onChange={(e) => setF({ ...f, title: e.target.value })}
        />
        <input
          placeholder="Request Description"
          value={f.description}
          onChange={(e) => setF({ ...f, description: e.target.value })}
        />
        <select
          value={f.siteId}
          onChange={(e) => setF({ ...f, siteId: e.target.value })}
        >
          <option value="">Select Site</option>
          {sites.map((s) => (
            <option key={s.id} value={s.id}>
              {s.siteName}
            </option>
          ))}
        </select>
        <select
          value={f.priority}
          onChange={(e) => setF({ ...f, priority: e.target.value })}
        >
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
        <button className="primary">Submit Request</button>
      </form>
    </div>
  );
}
