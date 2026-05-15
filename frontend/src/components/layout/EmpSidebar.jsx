import React from "react";
import {
    LayoutDashboard,
    Calendar,
    UserCheck,
    FileText,
    Repeat,
    LogOut,
} from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";

const EmpSidebar = () => {
    const navigate = useNavigate();

    const role = (localStorage.getItem("role") || "EMPLOYEE").toUpperCase();
    const username = localStorage.getItem("username") || "User";

    const menu = [
        { name: "Dashboard", icon: LayoutDashboard, path: "/emp/dashboard" },
        { name: "My Schedule", icon: Calendar, path: "/emp/schedule" },
        { name: "Availability", icon: UserCheck, path: "/emp/availability" },
        { name: "Shift Swap", icon: Repeat, path: "/emp/shiftswap" },
        { name: "Requests", icon: FileText, path: "/emp/requests" },
    ];

    const handleLogout = async () => {
        try {
            // await api.post('/auth/logout');
        } catch (error) {
            console.error("Logout failed", error);
        } finally {
            localStorage.removeItem("token");
            localStorage.removeItem("role");
            localStorage.removeItem("username");
            navigate("/login");
        }
    };

    return (
        <aside className="bg-gray-900 border-r border-gray-800 w-64 flex flex-col h-screen">

            {/* Header */}
            <div className="h-16 flex items-center justify-center border-b border-gray-800">
                <div className="flex items-center gap-2 font-bold text-xl text-white">
                    <div className="bg-orange-600 text-white p-2 rounded-lg shadow-lg shadow-orange-900/20">
                        <Calendar size={20} strokeWidth={2.5} />
                    </div>
                    <span className="tracking-tight">Shifter</span>
                </div>
            </div>

            {/* Navigation */}
            <nav className="flex-1 overflow-y-auto py-4">
                <ul className="space-y-1 px-3">
                    {menu.map((item) => (
                        <li key={item.path}>
                            <NavLink
                                to={item.path}
                                end
                                className={({ isActive }) =>
                                    `flex items-center gap-3 px-3 py-3 rounded-lg transition-colors group ${
                                        isActive
                                            ? "bg-gray-800 text-orange-500 font-medium border border-gray-700"
                                            : "text-gray-400 hover:bg-gray-800 hover:text-white"
                                    }`
                                }
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
                                        <span>{item.name}</span>
                                    </>
                                )}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>

            {/* Footer */}
            <div className="p-4 border-t border-gray-800">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-gray-300 font-bold text-sm border border-gray-600">
                        {username.charAt(0).toUpperCase()}
                    </div>

                    <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-white truncate capitalize">
                            {username}
                        </p>
                        <p className="text-xs text-gray-500 truncate capitalize">
                            {role.toLowerCase()}
                        </p>
                    </div>

                    <button
                        onClick={handleLogout}
                        className="text-gray-500 hover:text-red-400 transition-colors p-2 rounded-md hover:bg-gray-800"
                        title="Sign Out"
                    >
                        <LogOut size={18} />
                    </button>
                </div>
            </div>

        </aside>
    );
};

export default EmpSidebar;