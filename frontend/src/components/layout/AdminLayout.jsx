import React, { useState } from "react";
import { Outlet } from "react-router-dom";
import AdminSidebar from "./AdminSidebar.jsx";
import { Menu } from "lucide-react";

export default function AdminLayout() {
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    return (
        <div className="flex h-screen bg-gray-50 overflow-hidden">
            {/* Sidebar */}
            <AdminSidebar isSidebarOpen={isSidebarOpen} />

            {/* Main */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Topbar */}
                <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shadow-sm z-10">
                    <button
                        onClick={() => setIsSidebarOpen((v) => !v)}
                        className="p-2 rounded-lg hover:bg-gray-100 text-gray-600 focus:outline-none"
                        aria-label="Toggle sidebar"
                    >
                        <Menu size={20} />
                    </button>

                    <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500 hidden sm:block">
              {new Date().toLocaleDateString(undefined, {
                  weekday: "long",
                  year: "numeric",
                  month: "long",
                  day: "numeric",
              })}
            </span>
                    </div>
                </header>

                {/* Page content */}
                <main className="flex-1 overflow-auto p-6 bg-gray-50">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
