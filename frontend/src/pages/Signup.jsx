import { useState, useEffect } from "react";
import { signup, getCustomers } from "../services/commonService";

export default function Signup({ onSignup, goLogin }) {

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    phone: "",
    role: "MANAGER",
    customerId: ""
  });

  const [err, setErr] = useState("");
  const [customers, setCustomers] = useState([]);
  useEffect(() => {
    getCustomers()
      .then((r) => {
        setCustomers(r.data);
      })
      .catch((err) => {
        console.error(err);
      });
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setErr("");

    try {
      const { data } = await signup(form);

      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data));

      onSignup(data);

    } catch (x) {
      setErr("Signup failed. Email may already exist.");
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
          Create your FSM Keystone account to manage work orders, sites, and customers.
        </p>


        {err && <div className="error">{err}</div>}

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

        <input
          type="password"
          placeholder="Password"
          onChange={e => setForm({
            ...form,
            password: e.target.value
          })}
        />

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
          <option>MANAGER</option>
          <option>DISPATCHER</option>
          <option>TECHNICIAN</option>
          <option>CUSTOMER</option>
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
                Select Customer
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