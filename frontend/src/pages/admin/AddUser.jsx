import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../../services/api";

export default function AddUser() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        fullName: "",
        email: "",
        username: "",
        role: "EMPLOYEE",
        password: "", // ✅ NEW
    });

    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    const onChange = (key) => (e) => {
        setForm((p) => ({ ...p, [key]: e.target.value }));
        if (error) setError("");
    };

    const submit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError("");

        try {
            const payload = {
                fullName: form.fullName.trim(),
                email: form.email.trim(),
                username: form.username.trim(),
                roles: [form.role],
                password: form.password.trim() || null, // ✅ key logic
            };

            await API.post("/admin/users", payload);

            alert("User created successfully");
            navigate("/admin/users");
        } catch (err) {
            console.error(err);
            setError(
                err?.response?.data?.message ||
                "Failed to create user. Check validation or permissions."
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="max-w-xl bg-white rounded-xl shadow-sm p-8 border border-gray-100">
            <h1 className="text-2xl font-bold mb-6">Add User</h1>

            {error && (
                <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
                    {error}
                </div>
            )}

            <form onSubmit={submit} className="space-y-5">
                {/* Full Name */}
                <div>
                    <label className="block text-sm font-semibold mb-1">Full Name</label>
                    <input
                        type="text"
                        value={form.fullName}
                        onChange={onChange("fullName")}
                        className="w-full px-4 py-3 rounded-lg border focus:ring-2 focus:ring-orange-200"
                        required
                    />
                </div>

                {/* Email */}
                <div>
                    <label className="block text-sm font-semibold mb-1">Email</label>
                    <input
                        type="email"
                        value={form.email}
                        onChange={onChange("email")}
                        className="w-full px-4 py-3 rounded-lg border focus:ring-2 focus:ring-orange-200"
                        required
                    />
                </div>

                {/* Username */}
                <div>
                    <label className="block text-sm font-semibold mb-1">Username</label>
                    <input
                        type="text"
                        value={form.username}
                        onChange={onChange("username")}
                        className="w-full px-4 py-3 rounded-lg border focus:ring-2 focus:ring-orange-200"
                        required
                    />
                </div>

                {/* Password (Optional) */}
                <div>
                    <label className="block text-sm font-semibold mb-1">
                        Password <span className="text-gray-400">(optional)</span>
                    </label>
                    <input
                        type="password"
                        value={form.password}
                        onChange={onChange("password")}
                        placeholder="Leave blank to use email as password"
                        className="w-full px-4 py-3 rounded-lg border focus:ring-2 focus:ring-orange-200"
                    />
                </div>

                {/* Role */}
                <div>
                    <label className="block text-sm font-semibold mb-1">Role</label>
                    <select
                        value={form.role}
                        onChange={onChange("role")}
                        className="w-full px-4 py-3 rounded-lg border bg-white focus:ring-2 focus:ring-orange-200"
                    >
                        <option value="EMPLOYEE">Employee</option>
                        <option value="MANAGER">Manager</option>
                    </select>
                </div>

                <button
                    type="submit"
                    disabled={saving}
                    className="w-full bg-orange-600 hover:bg-orange-700 text-white font-bold py-3 rounded-lg disabled:opacity-60"
                >
                    {saving ? "Creating..." : "Create User"}
                </button>
            </form>
        </div>
    );
}