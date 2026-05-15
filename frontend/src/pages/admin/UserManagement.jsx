import React, { useEffect, useState } from "react";
import API from "../../services/api";

function rolePill(role) {
    const r = String(role || "").toUpperCase();
    if (r.includes("ADMIN")) return "bg-red-100 text-red-700";
    if (r.includes("MANAGER")) return "bg-blue-100 text-blue-700";
    return "bg-green-100 text-green-700";
}

export default function UserManagement() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setError("");

            try {
                const res = await API.get("/admin/users");

                // ✅ Handles both response shapes:
                // A) List<UserResponse> directly: res.data = [...]
                // B) Wrapped: { success, message, data: [...] }
                const list = Array.isArray(res.data) ? res.data : res.data?.data;

                setUsers(Array.isArray(list) ? list : []);
            } catch (err) {
                console.error(err);
                const status = err?.response?.status;
                const msg =
                    err?.response?.data?.message ||
                    err?.response?.data?.error ||
                    err?.message ||
                    "Request failed";
                setError(status ? `${status} - ${msg}` : msg);
                setUsers([]);
            } finally {
                setLoading(false);
            }
        };

        load();
    }, []);

    return (
        <div className="max-w-6xl space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
                <p className="text-sm text-gray-500">View users in the system.</p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-100 text-red-700 p-4 rounded-xl text-sm">
                    <div className="font-semibold mb-1">Failed to load users</div>
                    <div>{error}</div>
                    <div className="mt-2 text-red-600/80">
                        Tip: Make sure you are logged in and your token has ADMIN permissions.
                    </div>
                </div>
            )}

            <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
                <table className="min-w-full border-collapse">
                    <thead className="bg-gray-100">
                    <tr>
                        <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                            Full Name
                        </th>
                        <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                            Username
                        </th>
                        <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                            Email
                        </th>
                        <th className="px-6 py-4 text-left text-sm font-semibold text-gray-600">
                            Roles
                        </th>
                    </tr>
                    </thead>

                    <tbody className="divide-y">
                    {loading && (
                        <tr>
                            <td colSpan={4} className="px-6 py-6 text-center text-gray-500">
                                Loading users...
                            </td>
                        </tr>
                    )}

                    {!loading && users.length === 0 && !error && (
                        <tr>
                            <td colSpan={4} className="px-6 py-6 text-center text-gray-500">
                                No users found
                            </td>
                        </tr>
                    )}

                    {!loading &&
                        users.map((user) => (
                            <tr key={user.id} className="hover:bg-gray-50 transition">
                                <td className="px-6 py-4 text-sm text-gray-800">
                                    {user.fullName}
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-800">
                                    {user.username}
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-800">
                                    {user.email}
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex flex-wrap gap-2">
                                        {(user.roles ? Array.from(user.roles) : []).map((r) => (
                                            <span
                                                key={r}
                                                className={`px-3 py-1 rounded-full text-xs font-semibold ${rolePill(
                                                    r
                                                )}`}
                                            >
                          {String(r).replace("ROLE_", "")}
                        </span>
                                        ))}
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
