import { useEffect, useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { getMyProfile, updateProfile } from "../services/authService";

const formatRole = (role) => {
  if (!role) return "";
  return role.charAt(0) + role.slice(1).toLowerCase();
};

export default function Settings({ user, onProfileUpdated }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({
    fullName: "",
    phone: "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  useEffect(() => {
    getMyProfile()
      .then(({ data }) => {
        setProfile(data);
        setForm((prev) => ({
          ...prev,
          fullName: data.fullName || "",
          phone: data.phone || ""
        }));
      })
      .catch(() => {
        setProfile({
          fullName: user?.fullName,
          email: user?.email,
          role: user?.role,
          phone: user?.phone || "",
          customerName: user?.customerName
        });
        setForm((prev) => ({
          ...prev,
          fullName: user?.fullName || "",
          phone: user?.phone || ""
        }));
      })
      .finally(() => setLoading(false));
  }, [user]);

  const save = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");

    const changingPassword =
      form.newPassword || form.confirmPassword || form.currentPassword;

    if (changingPassword) {
      if (!form.currentPassword) {
        setError("Enter your current password to change it.");
        return;
      }
      if (form.newPassword.length < 6) {
        setError("New password must be at least 6 characters.");
        return;
      }
      if (form.newPassword !== form.confirmPassword) {
        setError("New password and confirmation do not match.");
        return;
      }
    }

    setSaving(true);
    try {
      const payload = {
        fullName: form.fullName.trim(),
        phone: form.phone.trim() || null,
        currentPassword: changingPassword ? form.currentPassword : null,
        newPassword: changingPassword ? form.newPassword : null
      };

      const { data } = await updateProfile(payload);
      setProfile(data);

      onProfileUpdated?.({
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        phone: data.phone,
        customerId: data.customerId,
        customerName: data.customerName
      });

      setForm((prev) => ({
        ...prev,
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
      }));
      setMessage("Profile updated successfully.");
    } catch (err) {
      setError(err.response?.data?.message || "Could not update profile.");
    } finally {
      setSaving(false);
    }
  };

  const renderPasswordField = (name, label, visible, setVisible) => (
    <div className="settings-field">
      <label className="field-label">{label}</label>
      <div className="password-field">
        <input
          type={visible ? "text" : "password"}
          placeholder={label}
          value={form[name]}
          onChange={(e) => setForm({ ...form, [name]: e.target.value })}
        />
        <button
          type="button"
          className="eye-btn"
          onClick={() => setVisible(!visible)}
          aria-label={visible ? "Hide password" : "Show password"}
        >
          {visible ? <FaEyeSlash /> : <FaEye />}
        </button>
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="page settings-page">
        <div className="settings-layout">
          <section className="panel settings-panel">
            <p className="settings-sub">Loading profile…</p>
          </section>
        </div>
      </div>
    );
  }

  const displayProfile = profile || user;

  return (
    <div className="page settings-page">
      <div className="settings-layout">
        <form className="panel settings-panel" onSubmit={save}>
          <div className="settings-header">
            <div>
              <h2>Account Settings</h2>
              <p className="settings-sub">
                Update your profile details and password.
              </p>
            </div>
          </div>

          {message && <div className="settings-message ok">{message}</div>}
          {error && <div className="settings-message error">{error}</div>}

          <section className="settings-section">
            <h3 className="settings-section-title">Profile details</h3>

            <div className="settings-grid">
              <div className="settings-field">
                <label className="field-label">Email</label>
                <input value={displayProfile?.email || ""} disabled />
              </div>

              <div className="settings-field">
                <label className="field-label">Role</label>
                <input value={formatRole(displayProfile?.role)} disabled />
              </div>

              <div className="settings-field">
                <label className="field-label">Full name</label>
                <input
                  placeholder="Full name"
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  required
                />
              </div>

              <div className="settings-field">
                <label className="field-label">Phone number</label>
                <input
                  type="tel"
                  placeholder="Phone number"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>

              {displayProfile?.customerName && (
                <div className="settings-field settings-field-wide">
                  <label className="field-label">Organisation</label>
                  <input value={displayProfile.customerName} disabled />
                </div>
              )}

              {displayProfile?.createdAt && (
                <div className="settings-field settings-field-wide">
                  <label className="field-label">Member since</label>
                  <input
                    value={new Date(displayProfile.createdAt).toLocaleDateString()}
                    disabled
                  />
                </div>
              )}
            </div>
          </section>

          <section className="settings-section">
            <h3 className="settings-section-title">Change password</h3>
            <p className="settings-sub">
              Leave blank to keep your current password.
            </p>

            <div className="settings-grid settings-grid-single">
              {renderPasswordField(
                "currentPassword",
                "Current password",
                showCurrentPassword,
                setShowCurrentPassword
              )}
              {renderPasswordField(
                "newPassword",
                "New password",
                showNewPassword,
                setShowNewPassword
              )}
              {renderPasswordField(
                "confirmPassword",
                "Confirm new password",
                showConfirmPassword,
                setShowConfirmPassword
              )}
            </div>
          </section>

          <div className="settings-actions">
            <button type="submit" className="primary settings-save" disabled={saving}>
              {saving ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
