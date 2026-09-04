import { useEffect, useState } from "react";
import api from "../api/axiosConfig";

export default function TimeLogPhotoThumb({ photo, onOpen }) {
  const [src, setSrc] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let objectUrl;

    api
      .get(`/time-logs/photos/${photo.id}`, { responseType: "blob" })
      .then((response) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(response.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [photo.id]);

  if (failed) {
    return <span className="time-log-photo-missing">Unavailable</span>;
  }

  if (!src) {
    return <span className="time-log-photo-loading">…</span>;
  }

  return (
    <button
      type="button"
      className="time-log-photo-thumb"
      onClick={() => onOpen?.(photo, src)}
      title={photo.fileName || "Work photo"}
    >
      <img src={src} alt={photo.fileName || "Work photo"} />
    </button>
  );
}
