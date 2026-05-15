import React, { useState } from 'react';
import {
    LayoutDashboard,
    Calendar,
    Clock,
    FileText,
    BarChart3,
    UserCheck,
    Layers,
    LogOut,
    Menu,
    Users,
    Briefcase
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../../api/axiosConfig'; // Ensure this path matches axios config

const ManagerLayout = ({ children, role = 'EMPLOYEE' }) => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const location = useLocation();
    const navigate = useNavigate();

    // --- 1. Define Menu Items per Role ---
    const menuItems = {
        ADMIN: [
            { name: 'Dashboard', icon: LayoutDashboard, path: '/admin/dashboard' },
            { name: 'User Management', icon: Users, path: '/admin/users' },
            { name: 'Settings', icon: Briefcase, path: '/admin/settings' },
        ],
        MANAGER: [
            { name: 'Dashboard', icon: LayoutDashboard, path: '/manager/dashboard' },
            { name: 'Schedule Builder', icon: Calendar, path: '/manager/schedule' },
            { name: 'Employees', icon: Users, path: '/manager/employees' },
            { name: 'Reports', icon: BarChart3, path: '/manager/reports' },
        ],
        EMPLOYEE: [
            { name: 'Dashboard', icon: Clock, path: '/employee/dashboard' },
            { name: 'My Schedule', icon: Calendar, path: '/employee/schedule' },
            { name: 'Availability', icon: UserCheck, path: '/employee/availability' },
            { name: 'Requests', icon: FileText, path: '/employee/requests' },
            { name: 'Open Shifts', icon: Layers, path: '/employee/open-shifts' },
        ]
    };

    // Select the menu based on the prop passed to this layout
    const currentMenu = menuItems[role.toUpperCase()] || menuItems['EMPLOYEE'];

    // --- 2. Logout Handler ---
    const handleLogout = async () => {
        try {
            // Optional: Call backend to blacklist token
            // await api.post('/auth/logout');
        } catch (error) {
            console.error("Logout failed", error);
        } finally {
            localStorage.removeItem('token');
            localStorage.removeItem('role');
            localStorage.removeItem('username');
            navigate('/login');
        }
    };

    return (
        <div className="flex h-screen bg-gray-50 overflow-hidden">

            {/* --- SIDEBAR (Dark Theme Integrated) --- */}
            <aside
                className={`bg-gray-900 border-r border-gray-800 transition-all duration-300 ease-in-out flex flex-col z-20
          ${isSidebarOpen ? 'w-64' : 'w-20'}
        `}
            >
                {/* Sidebar Header (Logo) */}
                <div className="h-16 flex items-center justify-center border-b border-gray-800 shrink-0">
                    <div className="flex items-center gap-2 font-bold text-xl text-white">
                        <div className="bg-orange-600 text-white p-2 rounded-lg shadow-lg shadow-orange-900/20">
                            <Calendar size={20} strokeWidth={2.5} />
                        </div>
                        {isSidebarOpen && <span className="tracking-tight text-white">Shifter</span>}
                    </div>
                </div>

                {/* Navigation Links */}
                <nav className="flex-1 overflow-y-auto py-4">
                    <ul className="space-y-1 px-3">
                        {currentMenu.map((item) => {
                            const isActive = location.pathname === item.path;
                            return (
                                <li key={item.path}>
                                    <Link
                                        to={item.path}
                                        className={`
                      flex items-center gap-3 px-3 py-3 rounded-lg transition-colors group
                      ${isActive
                                            ? 'bg-gray-800 text-orange-500 font-medium border border-gray-700'
                                            : 'text-gray-400 hover:bg-gray-800 hover:text-white'}
                    `}
                                    >
                                        <item.icon
                                            size={20}
                                            className={isActive ? 'text-orange-500' : 'text-gray-500 group-hover:text-gray-300'}
                                        />
                                        {isSidebarOpen && <span>{item.name}</span>}
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>
                </nav>

                {/* User Profile / Logout Section */}
                <div className="p-4 border-t border-gray-800 shrink-0">
                    <div className={`flex items-center gap-3 ${!isSidebarOpen && 'justify-center'}`}>
                        {/* User Avatar Initials */}
                        <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-gray-300 font-bold text-sm border border-gray-600">
                            {role.charAt(0).toUpperCase()}
                        </div>

                        {/* User Info (Hidden if collapsed) */}
                        {isSidebarOpen && (
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-white truncate capitalize">
                                    {localStorage.getItem('username') || 'User'}
                                </p>
                                <p className="text-xs text-gray-500 truncate capitalize">{role.toLowerCase()}</p>
                            </div>
                        )}

                        {/* Logout Button */}
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

            {/* --- MAIN CONTENT AREA --- */}
            <div className="flex-1 flex flex-col min-w-0">

                {/* Top Navbar */}
                <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shadow-sm z-10">
                    {/* Toggle Sidebar Button */}
                    <button
                        onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                        className="p-2 rounded-lg hover:bg-gray-100 text-gray-600 focus:outline-none"
                    >
                        <Menu size={20} />
                    </button>

                    {/* Right Side Info */}
                    <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500 hidden sm:block">
               {new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
                    </div>
                </header>

                {/* Dynamic Page Content */}
                <main className="flex-1 overflow-auto p-6 bg-gray-50">
                    {children}
                </main>
            </div>
        </div>
    );
};

export default PageLayout;