import { useState } from "react";
import { login } from "../services/authService";

export default function Login({ onLogin, goSignup }) {

  const [form, setForm] = useState({
    email: "",
    password: ""
  });

  const [err, setErr] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    setErr("");

    try {
      const { data } = await login(form);

      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data));

      onLogin(data);

    } catch (x) {
      setErr("Login failed. Check email/password.");
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

        <h1>Welcome back</h1>

        <p>Login to your Field Service management Keystone dashboard.</p>

        {err && <div className="error">{err}</div>}

        <input
          placeholder="Email"
          value={form.email}
          onChange={e =>
            setForm({
              ...form,
              email: e.target.value
            })
          }
        />

        <input
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={e =>
            setForm({
              ...form,
              password: e.target.value
            })
          }
        />

        <button className="primary">
          Login
        </button>

        <small>
          New user?
          <a onClick={goSignup}>
            Create account
          </a>
        </small>

      </form>
    </div>
  );
}