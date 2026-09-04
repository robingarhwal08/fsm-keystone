import { useState, useEffect } from "react";
import { signup, getSignupCustomers } from "../services/commonService";
import { FaEye, FaEyeSlash } from "react-icons/fa";

export default function Signup({ onSignup, goLogin }) {

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    phone: "",
    role: "DISPATCHER",
    customerId: ""
  });

  const [showPassword, setShowPassword] = useState(false);

  const [err, setErr] = useState("");
  const [success, setSuccess] = useState("");
  const [customers, setCustomers] = useState([]);
  useEffect(() => {
    getSignupCustomers()
      .then((r) => {
        setCustomers(r.data || []);
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    setSuccess("");

    if (form.role === "CUSTOMER" && !form.customerId) {
      setErr("Please select the customer organisation you belong to.");
      return;
    }

    try {
      const { data } = await signup(form);

      if (data.pendingApproval) {
        setSuccess(
          "Your account was submitted for admin approval. You can log in after an admin approves your request."
        );
        return;
      }

      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data));

      onSignup(data);

    } catch (x) {
      setErr(x.response?.data?.message || "Signup failed. Email may already exist.");
    }
  };

  return (
    <div className="auth-bg">
      <form className="auth-card" onSubmit={submit}>

        <div className="brand big">
          <span className="brand-mark">FSM</span>
          <b>
            KEY<span>STONE</span>
          </b>
        </div>

        <h1>Create account</h1>
        <p>
          Manager, dispatcher, and technician accounts require admin approval before login.
        </p>


        {err && <div className="error">{err}</div>}
        {success && <div className="settings-message ok">{success}</div>}

        <input
          placeholder="Full name"
          onChange={e => setForm({
            ...form,
            fullName: e.target.value
          })}
        />

        <input
          placeholder="Email"
          onChange={e => setForm({
            ...form,
            email: e.target.value
          })}
        />

        <div className="password-field">
          <input
              type={showPassword ? "text" : "password"}
              placeholder="Password"
              value={form.password}
              onChange={(e) =>
                  setForm({
                    ...form,
                    password: e.target.value,
                  })
              }
          />

          <button
              type="button"
              className="eye-btn"
              onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? <FaEyeSlash /> : <FaEye />}
          </button>
        </div>

        <input
          placeholder="Phone"
          onChange={e => setForm({
            ...form,
            phone: e.target.value
          })}
        />


        <select
          value={form.role}
          onChange={e => setForm({
            ...form,
            role: e.target.value
          })}
        >
          <option value="MANAGER">MANAGER</option>
          <option value="DISPATCHER">DISPATCHER</option>
          <option value="TECHNICIAN">TECHNICIAN</option>
          <option value="CUSTOMER">CUSTOMER</option>
        </select>
        {
          form.role === "CUSTOMER" && (
            <select
              value={form.customerId}
              onChange={(e) =>
                setForm({
                  ...form,
                  customerId: e.target.value
                })
              }
            >
              <option value="">
                SELECT CUSTOMER
              </option>

              {customers.map((c) => (
                <option
                  key={c.id}
                  value={c.id}
                >
                  {c.name}
                </option>
              ))}
            </select>
          )
        }

        <button className="primary">
          Sign up
        </button>

        <small>
          Already have account?
          <a onClick={goLogin}>
            Login
          </a>
        </small>

      </form>
    </div>
  );
}