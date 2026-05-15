import React, { useEffect, useMemo, useState } from "react";
import { NavLink } from "react-router-dom";
import { Users, UserPlus, ShieldCheck, Briefcase } from "lucide-react";
import API from "../../services/api";

function MetricCard({ label, value, sub, Icon, iconBg, iconText, loading }) {
    return (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="flex justify-between items-start">
                <div>
                    <p className="text-sm font-medium text-gray-500">{label}</p>
                    <h3 className="text-3xl font-bold text-gray-800 mt-2">
                        {loading ? "—" : value}
                    </h3>
                </div>
                <div className={`p-2 rounded-lg ${iconBg} ${iconText}`}>
                    <Icon size={20} />
                </div>
            </div>
            <p className="text-xs text-gray-400 mt-4">{sub}</p>
        </div>
    );
}

function ActionItem({ to, title, icon, subtitle }) {
    return (
        <NavLink
            to={to}
            end
            className={({ isActive }) =>
                `
        flex items-center justify-between
        w-full
        px-5 py-4
        rounded-xl
        border
        transition-all duration-200
        ${
                    isActive
                        ? "bg-white border-orange-500 ring-2 ring-orange-200"
                        : "bg-white border-gray-100 hover:bg-gray-50 hover:border-gray-200"
                }
      `
            }
        >
            <div className="flex items-center gap-4">
                <div
                    className={`
            w-10 h-10 rounded-lg flex items-center justify-center text-lg
            ${to.includes("/admin") ? "bg-orange-50 text-orange-600" : "bg-gray-50 text-gray-700"}
          `}
                >
                    {icon}
                </div>

                <div>
                    <p className="text-base font-semibold text-gray-900">{title}</p>
                    {subtitle && <p className="text-xs text-gray-500">{subtitle}</p>}
                </div>
            </div>

            <span className="text-gray-300">›</span>
        </NavLink>
    );
}

function hasRole(user, roleName) {
    const roles = Array.isArray(user?.roles) ? user.roles : [];
    return roles.map((r) => String(r).toUpperCase()).includes(roleName);
}

export default function Dashboard() {
    const username = localStorage.getItem("username") || "Admin";

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setError("");
            try {
                const res = await API.get("/admin/users");
                // backend returns List<UserResponse> => array
                const list = Array.isArray(res.data) ? res.data : res.data?.data;
                setUsers(Array.isArray(list) ? list : []);
            } catch (err) {
                console.error(err);
                setUsers([]);
                setError(
                    err?.response?.data?.message ||
                    err?.message ||
                    "Failed to load dashboard data"
                );
            } finally {
                setLoading(false);
            }
        };

        load();
    }, []);

    const stats = useMemo(() => {
        const totalUsers = users.length;
        const admins = users.filter((u) => hasRole(u, "ADMIN")).length;
        const managers = users.filter((u) => hasRole(u, "MANAGER")).length;
        const employees = users.filter((u) => hasRole(u, "EMPLOYEE")).length;

        return { totalUsers, admins, managers, employees };
    }, [users]);

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Welcome, {username}!</h1>
                <p className="text-gray-500">Manage users and admin settings from here.</p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-100 text-red-700 p-4 rounded-xl text-sm">
                    <div className="font-semibold mb-1">Dashboard error</div>
                    <div>{error}</div>
                </div>
            )}

            {/* Metric cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <MetricCard
                    label="Total Users"
                    value={stats.totalUsers}
                    sub="registered accounts"
                    Icon={Users}
                    iconBg="bg-blue-50"
                    iconText="text-blue-600"
                    loading={loading}
                />

                {/* Replaces “Active Users” (not available yet from backend) */}
                <MetricCard
                    label="Managers"
                    value={stats.managers}
                    sub="users with MANAGER role"
                    Icon={Briefcase}
                    iconBg="bg-green-50"
                    iconText="text-green-600"
                    loading={loading}
                />

                <MetricCard
                    label="Employees"
                    value={stats.employees}
                    sub="users with EMPLOYEE role"
                    Icon={Briefcase}
                    iconBg="bg-orange-50"
                    iconText="text-orange-600"
                    loading={loading}
                />

                <MetricCard
                    label="Admins"
                    value={stats.admins}
                    sub="users with ADMIN role"
                    Icon={ShieldCheck}
                    iconBg="bg-orange-50"
                    iconText="text-orange-600"
                    loading={loading}
                />
            </div>

            {/* Actions + panel */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Quick Actions */}
                <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <h2 className="text-lg font-bold text-gray-800 mb-4">Quick Actions</h2>

                    <div className="flex flex-col gap-4">
                        <ActionItem
                            title="Dashboard"
                            subtitle="Overview of admin activity"
                            icon="▦"
                            to="/admin/dashboard"
                        />
                        <ActionItem
                            title="Manage Users"
                            subtitle="View users and update roles"
                            icon="🗓"
                            to="/admin/users"
                        />
                        <ActionItem
                            title="Add User"
                            subtitle="Create a new employee account"
                            icon="🔔"
                            to="/admin/add-user"
                        />
                    </div>
                </div>

                {/* Right panel */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <h2 className="text-lg font-bold text-gray-800 mb-4">Summary</h2>

                    <div className="space-y-3 text-sm text-gray-600">
                        <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                            Employees: <span className="font-semibold text-gray-900">{loading ? "—" : stats.employees}</span>
                        </div>
                        <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                            Managers: <span className="font-semibold text-gray-900">{loading ? "—" : stats.managers}</span>
                        </div>
                        <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                            Admins: <span className="font-semibold text-gray-900">{loading ? "—" : stats.admins}</span>
                        </div>

                        <div className="text-xs text-gray-400 pt-2">
                            “Active/Disabled” will appear once the backend adds an enabled flag.
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
