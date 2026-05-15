import React, { useState, useEffect } from 'react';
import { Users, AlertCircle, Calendar } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { format, parseISO, startOfWeek, addDays, isSameDay } from 'date-fns';
import DashboardApi from '../../api/dashboardApi';
import ReportApi from '../../api/reportApi';
import api from '../../api/axiosConfig';

const ManagerDashboard = () => {
    const [stats, setStats] = useState({ totalShifts: 0, pendingRequests: 0, activeEmployees: 0 });
    const [laborCostData, setLaborCostData] = useState([]);
    const [upcomingShifts, setUpcomingShifts] = useState([]);
    const [pendingApprovals, setPendingApprovals] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadDashboardData = async () => {
            setIsLoading(true);
            try {
                const today = new Date();
                const weekStart = startOfWeek(today, { weekStartsOn: 0 });
                const weekStartStr = format(weekStart, 'yyyy-MM-dd');
                const weekEndStr = format(addDays(weekStart, 6), 'yyyy-MM-dd');

                const [activeCount, reportRes, employeesRes, shiftsRes, requestsRes] = await Promise.all([
                    DashboardApi.getActiveEmployeesCount().catch(() => 0),
                    ReportApi.getWeeklySummary(weekStartStr).catch(() => ({})),
                    api.get('/emp/employees').catch(() => ({ data: [] })),
                    api.get(`/schedule/range?start=${weekStartStr}&end=${weekEndStr}`).catch(() => ({ data: [] })),
                    // Fetch ALL requests instead of relying on the limited oldest/preview endpoint
                    api.get('/requests').catch(() => ({ data: [] }))
                ]);

                const reportData = reportRes || {};
                const allShifts = shiftsRes.data?.data || shiftsRes.data || [];
                const allEmployees = employeesRes.data?.data || employeesRes.data || [];
                const allRequests = requestsRes.data?.data || requestsRes.data || [];

                // --- 1. SHIFTS & EMPLOYEES MAPPING ---
                const enrichedShifts = allShifts.map(shift => {
                    const shiftEmpId = shift.employee_id || shift.employeeId || shift.userId;
                    const emp = allEmployees.find(e => String(e.id || e.employeeId || e.userId) === String(shiftEmpId));

                    return {
                        ...shift,
                        employeeName: emp ? (emp.fullName || emp.name) : 'System User',
                        positionName: shift.positionName || (emp ? (emp.positionName || emp.position) : 'Staff'),
                        parsedDate: shift.date ? parseISO(shift.date) : null
                    };
                });

                const todayShiftsList = enrichedShifts.filter(s => s.parsedDate && isSameDay(s.parsedDate, today));

                // --- 2. REQUESTS MAPPING (Show all pending) ---
                // Filter requests to only show "PENDING" status
                const pendingOnly = allRequests.filter(req =>
                    (req.status || '').toUpperCase() === 'PENDING'
                );

                // Map the employee name into the request for display
                const enrichedPending = pendingOnly.map(req => {
                    const reqEmpId = req.userId || req.employeeId;
                    const emp = allEmployees.find(e => String(e.id || e.employeeId || e.userId) === String(reqEmpId));
                    return {
                        ...req,
                        employeeName: emp ? (emp.fullName || emp.name) : `User #${reqEmpId}`
                    };
                });

                // Update stats and lists
                setStats({
                    activeEmployees: activeCount || 0,
                    pendingRequests: enrichedPending.length, // True exact count
                    totalShifts: todayShiftsList.length
                });

                setUpcomingShifts(todayShiftsList.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || '')));
                setPendingApprovals(enrichedPending);

                // --- 3. EXACT CHART LOGIC ---
                const daysOrder = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
                const shortDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

                const laborCost = reportData.laborCost || reportData.labor_cost || {};
                const scheduledCostsMap = laborCost.dailyScheduledCost || laborCost.daily_scheduled_cost || {};
                const budgetsMap = laborCost.dailyBudget || laborCost.daily_budget || {};

                const formattedChartData = daysOrder.map((day, index) => ({
                    day: shortDays[index],
                    Budget: budgetsMap[day] || 0,
                    Scheduled: scheduledCostsMap[day] || 0
                }));

                setLaborCostData(formattedChartData);

            } catch (error) {
                console.error("Dashboard error:", error);
            } finally {
                setIsLoading(false);
            }
        };

        loadDashboardData();
    }, []);

    // Helper to format the request type beautifully
    const formatRequestType = (typeEnum) => {
        if (!typeEnum) return 'Unknown';
        const labels = {
            'WANT_TO_WORK': 'Wants to Work',
            'UNAVAILABLE': 'Unavailable',
            'LEAVE_OF_ABSENCE': 'Leave of Absence',
            'SICK_DAY': 'Sick Day',
            'SHIFT_SWAP': 'Shift Swap'
        };
        return labels[typeEnum] || typeEnum.replace(/_/g, ' ');
    };

    if (isLoading) return <div className="p-20 text-center animate-pulse">Loading Dashboard...</div>;

    return (
        <div className="space-y-6 p-4">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Welcome, Manager!</h1>
                <p className="text-gray-500">Summary for {format(new Date(), 'EEEE, MMMM do')}</p>
            </div>

            {/* STATS CARDS */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex justify-between">
                    <div>
                        <p className="text-sm font-medium text-gray-500">Shifts Today</p>
                        <h3 className="text-3xl font-bold text-blue-600 mt-2">{stats.totalShifts}</h3>
                    </div>
                    <div className="p-3 bg-blue-50 text-blue-600 rounded-full h-12 w-12 flex items-center justify-center"><Calendar size={24} /></div>
                </div>

                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex justify-between">
                    <div>
                        <p className="text-sm font-medium text-gray-500">Pending Requests</p>
                        <h3 className="text-3xl font-bold text-orange-600 mt-2">{stats.pendingRequests}</h3>
                    </div>
                    <div className="p-3 bg-orange-50 text-orange-600 rounded-full h-12 w-12 flex items-center justify-center"><AlertCircle size={24} /></div>
                </div>

                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex justify-between">
                    <div>
                        <p className="text-sm font-medium text-gray-500">Active Employees</p>
                        <h3 className="text-3xl font-bold text-green-600 mt-2">{stats.activeEmployees}</h3>
                    </div>
                    <div className="p-3 bg-green-50 text-green-600 rounded-full h-12 w-12 flex items-center justify-center"><Users size={24} /></div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* CHART */}
                <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <h3 className="font-bold text-gray-700 mb-6">Labor Cost: Budget vs. Scheduled</h3>
                    <div className="h-[300px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={laborCostData} margin={{ top: 20, right: 0, left: -20, bottom: 0 }} barGap={2}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dy={10} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} tickFormatter={(value) => `$${value}`} />
                                <Tooltip cursor={{ fill: '#F3F4F6' }} formatter={(value) => [`$${value.toFixed(2)}`, undefined]} />
                                <Legend iconType="square" wrapperStyle={{ paddingTop: '20px' }} />

                                <Bar dataKey="Budget" name="Budget" fill="#2E9E8B" radius={[2, 2, 0, 0]} />
                                <Bar dataKey="Scheduled" name="Scheduled" fill="#E57358" radius={[2, 2, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                <div className="space-y-6">
                    {/* SCHEDULED TODAY */}
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col h-auto">
                        <h3 className="font-bold text-gray-700 mb-4 shrink-0">Scheduled Today</h3>
                        <div className="space-y-3 max-h-[350px] overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent">
                            {upcomingShifts.length === 0 ? (
                                <p className="text-sm text-gray-400 text-center py-6">No shifts for today.</p>
                            ) : (
                                upcomingShifts.map((shift, idx) => (
                                    <div key={idx} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border">
                                        <div className="flex items-center gap-3">
                                            <div className="w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-bold">
                                                {shift.employeeName?.charAt(0)}
                                            </div>
                                            <div>
                                                <p className="text-sm font-bold text-gray-800">{shift.employeeName}</p>
                                                <p className="text-[10px] text-gray-500 uppercase">{shift.positionName}</p>
                                            </div>
                                        </div>
                                        <p className="text-xs font-bold text-blue-600">{shift.startTime?.substring(0,5)}</p>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* ALL PENDING REQUESTS */}
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col h-auto">
                        <h3 className="font-bold text-gray-700 mb-4 shrink-0">Pending Requests</h3>
                        <div className="space-y-3 max-h-[250px] overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent">
                            {pendingApprovals.length === 0 ? (
                                <p className="text-sm text-gray-400 text-center py-4">No pending requests.</p>
                            ) : (
                                pendingApprovals.map((req) => (
                                    <div key={req.id} className="flex items-center justify-between p-2 border-b last:border-0 hover:bg-gray-50 transition-colors rounded-lg">
                                        <div>
                                            <p className="text-sm font-bold text-gray-800">{req.employeeName}</p>
                                            <p className="text-[10px] text-gray-500">
                                                {formatRequestType(req.type || req.requestType)} • {req.date || req.requestDate || 'N/A'}
                                            </p>
                                        </div>
                                        <button className="text-[10px] font-bold text-orange-600 hover:text-orange-800 px-3 py-1.5 bg-orange-50 hover:bg-orange-100 rounded-md transition-colors">
                                            Review
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* INLINE CSS FOR SCROLLBARS */}
            <style jsx>{`
                .scrollbar-thin::-webkit-scrollbar { width: 6px; }
                .scrollbar-thin::-webkit-scrollbar-track { background: transparent; }
                .scrollbar-thin::-webkit-scrollbar-thumb { background-color: #d1d5db; border-radius: 20px; }
                .scrollbar-thin::-webkit-scrollbar-thumb:hover { background-color: #9ca3af; }
            `}</style>
        </div>
    );
};

export default ManagerDashboard;