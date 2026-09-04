import { useEffect, useState } from "react";
import {
  getWorkOrders,
  createTimeLog,
  getTimeLogs
} from "../services/commonService";
import { formatDateTime } from "../components/StatusHistoryTable";
import DateTimePicker from "../components/DateTimePicker";
import TimeLogPhotoThumb from "../components/TimeLogPhotoThumb";

const MAX_PHOTOS = 5;

function toApiDateTime(value) {
  if (!value) return value;
  return value.length === 16 ? `${value}:00` : value;
}

export default function TimeLogs({ user }) {

  const isTech = user?.role === "TECHNICIAN";
  const [jobs, setJobs] = useState([]);
  const [logs, setLogs] = useState([]);
  const [images, setImages] = useState([]);
  const [previewUrls, setPreviewUrls] = useState([]);
  const [lightbox, setLightbox] = useState(null);
  const [form, setForm] = useState({
    workOrderId: "",
    startTime: "",
    endTime: "",
    notes: ""
  });

  const loadLogs = () => {
    getTimeLogs()
      .then((r) => setLogs(r.data || []))
      .catch(() => setLogs([]));
  };

  useEffect(() => {
    if (isTech) {
      getWorkOrders().then((r) => {
        const data = r.data || [];
        setJobs(data.filter((w) =>
          w.assignedTechnician?.id === user.userId ||
          w.assignedTechnicianId === user.userId
        ));
      });
    }
    loadLogs();
  }, [user]);

  useEffect(() => {
    const urls = images.map((file) => URL.createObjectURL(file));
    setPreviewUrls(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [images]);

  const onImagesSelected = (event) => {
    const selected = Array.from(event.target.files || []);
    if (!selected.length) return;

    const combined = [...images, ...selected].slice(0, MAX_PHOTOS);
    if (images.length + selected.length > MAX_PHOTOS) {
      alert(`You can upload up to ${MAX_PHOTOS} images per time log.`);
    }
    setImages(combined);
    event.target.value = "";
  };

  const removeImage = (index) => {
    setImages((current) => current.filter((_, i) => i !== index));
  };

  const save = async (e) => {
    e.preventDefault();
    if (!form.workOrderId || !form.startTime || !form.endTime) {
      alert("Please select a work order, start time, and end time.");
      return;
    }
    const start = new Date(form.startTime);
    const end = new Date(form.endTime);
    if (end <= start) {
      alert("End time must be after start time.");
      return;
    }
    try {
      await createTimeLog({
        workOrderId: Number(form.workOrderId),
        technicianId: user.userId,
        startTime: toApiDateTime(form.startTime),
        endTime: toApiDateTime(form.endTime),
        workDescription: form.notes
      }, images);
      alert("Time log saved.");
      setForm({
        workOrderId: "",
        startTime: "",
        endTime: "",
        notes: ""
      });
      setImages([]);
      loadLogs();
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.statusText ||
        err.message ||
        "Could not save time log.";
      alert(message);
    }
  };

  return (
    <div className="page">
      {lightbox && (
        <div
          className="photo-lightbox"
          onClick={() => setLightbox(null)}
          role="presentation"
        >
          <div
            className="photo-lightbox-content"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              className="photo-lightbox-close"
              onClick={() => setLightbox(null)}
            >
              Close
            </button>
            <img src={lightbox.src} alt={lightbox.name || "Work photo"} />
            <p>{lightbox.name}</p>
          </div>
        </div>
      )}

      {(isTech) && (
      <form className="panel" onSubmit={save}>
        <h2>Add Time Log</h2>

        <select
          value={form.workOrderId}
          onChange={(e) =>
            setForm({
              ...form,
              workOrderId: e.target.value
            })
          }
          required
        >
          <option value="">Select Work Order</option>
          {jobs.map((w) => (
            <option key={w.id} value={w.id}>
              {w.workOrderNumber} - {w.title}
            </option>
          ))}
        </select>

        <DateTimePicker
          label="Start time"
          value={form.startTime}
          onChange={(startTime) => setForm({ ...form, startTime })}
          required
        />

        <DateTimePicker
          label="End time"
          value={form.endTime}
          onChange={(endTime) => setForm({ ...form, endTime })}
          required
        />

        <textarea
          placeholder="Work Notes"
          value={form.notes}
          onChange={(e) =>
            setForm({
              ...form,
              notes: e.target.value
            })
          }
        />

        <div className="time-log-upload">
          <label htmlFor="time-log-photos">Work photos (optional)</label>
          <input
            id="time-log-photos"
            type="file"
            accept="image/*"
            multiple
            onChange={onImagesSelected}
          />
          <p className="field-hint">
            Upload up to {MAX_PHOTOS} images showing the work completed.
          </p>
          {previewUrls.length > 0 && (
            <div className="time-log-photo-previews">
              {previewUrls.map((url, index) => (
                <div key={url} className="time-log-photo-preview">
                  <img src={url} alt={images[index]?.name || "Selected photo"} />
                  <button
                    type="button"
                    className="time-log-photo-remove"
                    onClick={() => removeImage(index)}
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <button className="primary" type="submit">
          Save Time Log
        </button>
      </form>
      )}

      <section className="panel">
        <h2>Time log history</h2>
        <table>
          <thead>
            <tr>
              <th>Work order</th>
              <th>Title</th>
              <th>Technician</th>
              <th>Start time</th>
              <th>End time</th>
              <th>Hours</th>
              <th>Notes</th>
              <th>Photos</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && (
              <tr>
                <td colSpan="8">No time logs yet.</td>
              </tr>
            )}
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{log.workOrderNumber || "-"}</td>
                <td>{log.workOrderTitle || "-"}</td>
                <td>{log.technicianName || "-"}</td>
                <td>{formatDateTime(log.startTime)}</td>
                <td>{formatDateTime(log.endTime)}</td>
                <td>{log.hoursSpent ?? "-"}</td>
                <td>{log.workDescription || "-"}</td>
                <td>
                  {log.photos?.length ? (
                    <div className="time-log-photo-row">
                      {log.photos.map((photo) => (
                        <TimeLogPhotoThumb
                          key={photo.id}
                          photo={photo}
                          onOpen={(item, src) =>
                            setLightbox({ src, name: item.fileName })
                          }
                        />
                      ))}
                    </div>
                  ) : (
                    "-"
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
