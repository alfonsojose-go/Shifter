import React from 'react';
import {
    LayoutDashboard,
    Calendar,
    Clock,
    FileText,
    BarChart3,
    UserCheck,
    Layers,
    LogOut,
    Users,
    Briefcase
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../../api/axiosConfig';

const ManagerSidebar = ({ isSidebarOpen = true, role = 'MANAGER' }) => {
    const location = useLocation();
    const navigate = useNavigate();

    // --- Define Menu Items ---
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

    const currentMenu = menuItems[role.toUpperCase()] || menuItems['EMPLOYEE'];

    // --- Logout Handler ---
    const handleLogout = async () => {
        try {
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
        <aside
            className={`bg-gray-900 border-r border-gray-800 transition-all duration-300 ease-in-out flex flex-col z-20 h-screen
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
                    {/* Avatar - kept neutral/dark compatible */}
                    <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-gray-300 font-bold text-sm border border-gray-600">
                        {role.charAt(0).toUpperCase()}
                    </div>

                    {isSidebarOpen && (
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-white truncate capitalize">
                                {localStorage.getItem('username') || 'User'}
                            </p>
                            <p className="text-xs text-gray-500 truncate capitalize">{role.toLowerCase()}</p>
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
};

export default ManagerSidebar;