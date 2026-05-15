import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Settings from './components/common/Settings';

import AdminLayout from "./components/layout/AdminLayout";
import AdminDashboard from "./pages/admin/Dashboard";
import UserManagement from "./pages/admin/UserManagement";
import AddUser from "./pages/admin/AddUser";

import MainLayout from "./components/layout/ManagerLayout";
import ManagerDashboard from "./pages/manager/Dashboard";
import ScheduleBuilder from "./pages/manager/ScheduleBuilder";
import PublishedSchedule from './pages/manager/PublishedSchedule';
// import PublishedSchedules from './pages/manager/PublishedSchedules';
import LeaveRequests from "./pages/manager/LeaveRequests.jsx";
import BusinessRules from "./pages/manager/BusinessRules.jsx";
import Employees from './pages/manager/Employees';
import Budget from './pages/manager/Budget';
import Reports from './pages/manager/Reports';

import EmpDashboard from "./pages/employee/Dashboard";
import EmpSchedule from "./pages/employee/MySchedule.jsx";
import EmpAvailability from "./pages/employee/Availability";
import EmpRequests from "./pages/employee/Requests";
import EmpLayout from "./components/layout/EmpLayout.jsx";
import ShiftSwap from "./pages/employee/ShiftSwap";
import MySwapRequests from "./pages/employee/MySwapRequests";

function App() {
    return (
        <Router>
            <Routes>
                {/* Redirect Root to Login */}
                <Route path="/" element={<Navigate to="/login" replace />} />

                {/* Login */}
                <Route path="/login" element={<Login />} />

                {/* ADMIN (nested layout) */}
                <Route path="/admin" element={<AdminLayout />}>
                    <Route path="dashboard" element={<AdminDashboard />} />
                    <Route path="users" element={<UserManagement />} />
                    <Route path="add-user" element={<AddUser />} />
                </Route>

                {/* MANAGER (kept as-is with wrapper layout) */}
                <Route
                    path="/manager/dashboard"
                    element={
                        <MainLayout role="MANAGER">
                            <ManagerDashboard />
                        </MainLayout>
                    }
                />
                <Route
                    path="/manager/schedule"
                    element={
                        <MainLayout role="MANAGER">
                            <ScheduleBuilder />
                        </MainLayout>
                    }
                />
                <Route
                    path="/manager/leave-requests"
                    element={
                        <MainLayout role="MANAGER">
                            <LeaveRequests />
                        </MainLayout>
                    }
                />
                <Route
                    path="/manager/business-rules"
                    element={
                        <MainLayout role="MANAGER">
                            <BusinessRules />
                        </MainLayout>
                    }
                />

                <Route
                    path="/manager/Employees"
                    element={
                        <MainLayout role="MANAGER">
                            <Employees />
                        </MainLayout>
                    }
                />

                <Route
                    path="/manager/reports"
                    element={
                        <MainLayout role="MANAGER">
                            <Reports/>
                        </MainLayout>
                    }
                />

                <Route
                    path="/manager/budget"
                    element={
                        <MainLayout role="MANAGER">
                            <Budget />
                        </MainLayout>
                    }
                />

                <Route
                    path="/manager/settings"
                    element={
                        <MainLayout role="MANAGER">
                            <Settings />
                        </MainLayout>
                    }
                />

                <Route
                    path="/manager/published-schedule"
                    element={
                        <MainLayout role="MANAGER">
                            <PublishedSchedule />
                        </MainLayout>
                    }
                />

                {/*<Route*/}
                {/*    path="/manager/published-schedules"*/}
                {/*    element={*/}
                {/*        <MainLayout role="MANAGER">*/}
                {/*            <PublishedSchedules />*/}
                {/*        </MainLayout>*/}
                {/*    }*/}
                {/*/>*/}




                {/* EMPLOYEE (nested layout) */}
                <Route path="/emp" element={<EmpLayout />}>
                    <Route path="dashboard" element={<EmpDashboard />} />
                    <Route path="schedule" element={<EmpSchedule />} />
                    <Route path="availability" element={<EmpAvailability />} />
                    <Route path="shiftswap" element={<ShiftSwap />} />
                    <Route path="myswaprequests" element={<MySwapRequests />} />
                    <Route path="requests" element={<EmpRequests />} />
                    <Route path="settings" element={<Settings />} />
                </Route>

                {/* 404 Not Found (MUST be last) */}
                <Route
                    path="*"
                    element={
                        <div className="p-10 text-red-500">
                            <h1>404 - Page Not Found</h1>
                            <p>Current URL: {window.location.pathname}</p>
                        </div>
                    }
                />
            </Routes>
        </Router>
    );
}

export default App;
