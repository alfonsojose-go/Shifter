import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { LayoutDashboard, Users, UserPlus, Calendar, LogOut } from "lucide-react";

export default function AdminSidebar({ isSidebarOpen = true }) {
    const navigate = useNavigate();

    const role = (localStorage.getItem("role") || "ADMIN").toUpperCase();
    const username = localStorage.getItem("username") || "Admin";

    const menu = [
        { name: "Dashboard", icon: LayoutDashboard, path: "/admin/dashboard" },
        { name: "User Management", icon: Users, path: "/admin/users" },
        { name: "Add User", icon: UserPlus, path: "/admin/add-user" },
    ];

    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("username");
        navigate("/login");
    };

    return (
        <aside
            className={`bg-gray-900 border-r border-gray-800 transition-all duration-300 ease-in-out flex flex-col z-20 h-screen
        ${isSidebarOpen ? "w-64" : "w-20"}
      `}
        >
            {/* Header / Logo */}
            <div className="h-16 flex items-center justify-center border-b border-gray-800 shrink-0">
                <div className="flex items-center gap-2 font-bold text-xl text-white">
                    <div className="bg-orange-600 text-white p-2 rounded-lg shadow-lg shadow-orange-900/20">
                        <Calendar size={20} strokeWidth={2.5} />
                    </div>
                    {isSidebarOpen && <span className="tracking-tight">Shifter</span>}
                </div>
            </div>

            {/* Nav */}
            <nav className="flex-1 overflow-y-auto py-4">
                <ul className="space-y-1 px-3">
                    {menu.map((item) => (
                        <li key={item.path}>
                            <NavLink
                                to={item.path}
                                end
                                className={({ isActive }) => `
                  flex items-center gap-3 px-3 py-3 rounded-lg transition-colors group
                  ${
                                    isActive
                                        ? "bg-gray-800 text-orange-500 font-medium border border-gray-700"
                                        : "text-gray-400 hover:bg-gray-800 hover:text-white"
                                }
                `}
                            >
                                {({ isActive }) => (
                                    <>
                                        <item.icon
                                            size={20}
                                            className={
                                                isActive
                                                    ? "text-orange-500"
                                                    : "text-gray-500 group-hover:text-gray-300"
                                            }
                                        />
                                        {isSidebarOpen && <span>{item.name}</span>}
                                    </>
                                )}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>

            {/* Footer / Profile */}
            <div className="p-4 border-t border-gray-800 shrink-0">
                <div className={`flex items-center gap-3 ${!isSidebarOpen ? "justify-center" : ""}`}>
                    <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-gray-300 font-bold text-sm border border-gray-600">
                        {username.charAt(0).toUpperCase()}
                    </div>

                    {isSidebarOpen && (
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-white truncate capitalize">
                                {username}
                            </p>
                            <p className="text-xs text-gray-500 truncate capitalize">
                                {role.toLowerCase()}
                            </p>
                        </div>
                    )}

                    {isSidebarOpen && (
                        <button
                            onClick={handleLogout}
                            className="text-gray-500 hover:text-red-400 transition-colors p-2 rounded-md hover:bg-gray-800"
                            title="Sign Out"
                        >
                            <LogOut size={18} />
                        </button>
                    )}
                </div>
            </div>
        </aside>
    );
}
