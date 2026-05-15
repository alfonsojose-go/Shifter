import React, { useState } from 'react';
import {
    LayoutDashboard,
    Calendar,
    FileText,
    BarChart3,
    LogOut,
    Users,
    Briefcase,
    Settings,
    DollarSign,
    CheckSquare,
    ChevronLeft
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

const ManagerLayout = ({ children, role = 'EMPLOYEE' }) => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const location = useLocation();
    const navigate = useNavigate();

    // --- 1. Define Menu Items per Role ---
    const menuItems = {
        MANAGER: [
            { name: 'Dashboard', icon: LayoutDashboard, path: '/manager/dashboard' },
            { name: 'Schedule Builder', icon: Calendar, path: '/manager/schedule' },
            { name: 'Published Schedule', icon: CheckSquare, path: '/manager/published-schedule' },
            { name: 'Requests', icon: FileText, path: '/manager/leave-requests' },
            { name: 'Set Rules', icon: Briefcase, path: '/manager/business-rules' },
            { name: 'Employees', icon: Users, path: '/manager/employees' },
            { name: 'Budget', icon: DollarSign, path: '/manager/budget' },
            { name: 'Reports', icon: BarChart3, path: '/manager/reports' },
            { name: 'Settings', icon: Settings, path: '/manager/settings' },
        ],
    };

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
            {/* Elevated z-index to 30 so tooltips float above main content */}
            <aside
                className={`bg-gray-900 border-r border-gray-800 transition-all duration-300 ease-in-out flex flex-col z-30 relative
          ${isSidebarOpen ? 'w-64' : 'w-20'}
        `}
            >
                {/* Sidebar Header (Logo & Toggle) */}
                <div className={`h-16 flex items-center border-b border-gray-800 shrink-0 px-4 relative ${isSidebarOpen ? 'justify-between' : 'justify-center'}`}>

                    {/* Logo & Text */}
                    <div className="flex items-center gap-3 overflow-visible group">
                        {/* When collapsed, the logo itself becomes the expand button! */}
                        <button
                            onClick={() => !isSidebarOpen && setIsSidebarOpen(true)}
                            className={`bg-orange-600 text-white p-1.5 rounded-lg shadow-lg shadow-orange-900/20 shrink-0 transition-all ${!isSidebarOpen ? 'hover:bg-orange-500 cursor-pointer scale-110 relative' : 'cursor-default'}`}
                        >
                            <Calendar size={20} strokeWidth={2.5} />

                            {/* Expand Sidebar Tooltip */}
                            {!isSidebarOpen && (
                                <div className="absolute left-full top-1/2 -translate-y-1/2 ml-5 bg-gray-800 text-white text-xs font-medium px-2.5 py-1.5 rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 whitespace-nowrap border border-gray-700 pointer-events-none z-50">
                                    Expand Sidebar
                                </div>
                            )}
                        </button>
                        {isSidebarOpen && <span className="font-bold text-xl text-white tracking-tight truncate">Shifter</span>}
                    </div>

                    {/* Collapse Button - Only visible when expanded! */}
                    {isSidebarOpen && (
                        <button
                            onClick={() => setIsSidebarOpen(false)}
                            className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-800 focus:outline-none transition-colors"
                        >
                            <ChevronLeft size={20} />
                        </button>
                    )}
                </div>

                {/* Navigation Links */}
                {/* Changed to overflow-visible when collapsed so tooltips don't get cut off! */}
                <nav className={`flex-1 py-4 custom-scrollbar ${isSidebarOpen ? 'overflow-y-auto' : 'overflow-visible'}`}>
                    <ul className="space-y-1 px-3">
                        {currentMenu.map((item) => {
                            const isActive = location.pathname === item.path;
                            return (
                                <li key={item.path} className="relative">
                                    <Link
                                        to={item.path}
                                        className={`
                                            flex items-center gap-3 px-3 py-3 rounded-lg transition-colors group whitespace-nowrap relative
                                            ${!isSidebarOpen && 'justify-center'}
                                            ${isActive
                                            ? 'bg-gray-800 text-orange-500 font-medium border border-gray-700'
                                            : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                                        }
                                        `}
                                    >
                                        <item.icon
                                            size={20}
                                            className={`shrink-0 ${isActive ? 'text-orange-500' : 'text-gray-500 group-hover:text-gray-300'}`}
                                        />
                                        {isSidebarOpen && <span className="truncate">{item.name}</span>}

                                        {/* --- CUSTOM HOVER TOOLTIP --- */}
                                        {!isSidebarOpen && (
                                            <div className="absolute left-full top-1/2 -translate-y-1/2 ml-4 bg-gray-800 text-white text-xs font-medium px-2.5 py-1.5 rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 whitespace-nowrap z-50 border border-gray-700 pointer-events-none">
                                                {item.name}
                                                {/* Tooltip Arrow */}
                                                <div className="absolute top-1/2 -translate-y-1/2 -left-1 border-t-4 border-t-transparent border-b-4 border-b-transparent border-r-4 border-r-gray-800"></div>
                                            </div>
                                        )}
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>
                </nav>

                {/* User Profile / Logout Section */}
                <div className="p-4 border-t border-gray-800 shrink-0">
                    <div className={`flex items-center gap-3 ${!isSidebarOpen && 'justify-center flex-col gap-4'}`}>
                        {/* User Avatar Initials */}
                        <div className="w-10 h-10 rounded-full bg-gray-800 flex items-center justify-center text-gray-300 font-bold text-sm border border-gray-700 shrink-0">
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
                        <div className="relative group flex justify-center w-full">
                            <button
                                onClick={handleLogout}
                                className={`text-gray-500 hover:text-red-400 transition-colors p-2 rounded-md hover:bg-gray-800 ${!isSidebarOpen && 'w-full flex justify-center'}`}
                            >
                                <LogOut size={18} />
                            </button>

                            {/* Sign Out Tooltip */}
                            {!isSidebarOpen && (
                                <div className="absolute left-full top-1/2 -translate-y-1/2 ml-4 bg-gray-800 text-white text-xs font-medium px-2.5 py-1.5 rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 whitespace-nowrap border border-gray-700 pointer-events-none z-50">
                                    Sign Out
                                    <div className="absolute top-1/2 -translate-y-1/2 -left-1 border-t-4 border-t-transparent border-b-4 border-b-transparent border-r-4 border-r-gray-800"></div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </aside>

            {/* --- MAIN CONTENT AREA --- */}
            <div className="flex-1 flex flex-col min-w-0">

                {/* Top Navbar */}
                <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-end px-6 shadow-sm z-10 shrink-0">
                    {/* Right Side Info */}
                    <div className="flex items-center gap-4">
                        <span className="text-sm font-medium text-gray-500 hidden sm:block">
                            {new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
                        </span>
                    </div>
                </header>

                {/* Dynamic Page Content */}
                <main className="flex-1 overflow-auto p-6 bg-gray-50 relative">
                    {children}
                </main>
            </div>

            {/* Optional: Add custom CSS for a nicer scrollbar in the sidebar */}
            <style jsx>{`
                .custom-scrollbar::-webkit-scrollbar {
                    width: 4px;
                }
                .custom-scrollbar::-webkit-scrollbar-track {
                    background: transparent;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background-color: #374151;
                    border-radius: 20px;
                }
            `}</style>
        </div>
    );
};

export default ManagerLayout;