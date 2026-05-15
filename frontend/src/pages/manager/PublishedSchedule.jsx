import React, { useState, useEffect } from 'react';
import {
    ChevronLeft, ChevronRight, CalendarDays,
    AlertCircle, X, Search, Check, Send
} from 'lucide-react';
import { format, startOfWeek, addDays, subDays } from 'date-fns';
import Button from '../../components/common/Button';
import api from '../../api/axiosConfig';

const PublishedSchedule = () => {
    const [weekStart, setWeekStart] = useState(startOfWeek(new Date(), { weekStartsOn: 0 }));
    const currentWeekDates = Array.from({ length: 7 }).map((_, i) => addDays(weekStart, i));

    const [employees, setEmployees] = useState([]);
    const [schedule, setSchedule] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [currentShift, setCurrentShift] = useState(null);

    // --- STANDARD TOAST STATE ---
    const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

    const showToast = (message, type = 'success') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ show: false, message: '', type: 'success' }), 3000);
    };

    const [searchName, setSearchName] = useState('');
    const [filterDateFrom, setFilterDateFrom] = useState('');
    const [filterDateTo, setFilterDateTo] = useState('');

    // --- DERIVED FILTER STATE ---
    const filteredEmployees = employees.filter(emp =>
        emp.name.toLowerCase().includes(searchName.toLowerCase())
    );

    const isShiftVisible = (shift) => {
        if (filterDateFrom && shift.date < filterDateFrom) return false;
        if (filterDateTo && shift.date > filterDateTo) return false;
        return true;
    };

    const handleClearFilters = () => {
        setSearchName('');
        setFilterDateFrom('');
        setFilterDateTo('');
        setWeekStart(startOfWeek(new Date(), { weekStartsOn: 0 }));
    };

    const handleApplyFilters = () => {
        if (filterDateFrom) {
            const [year, month, day] = filterDateFrom.split('-');
            const targetDate = new Date(year, month - 1, day);
            setWeekStart(startOfWeek(targetDate, { weekStartsOn: 0 }));
        }
    };

    const hasActiveFilters = searchName || filterDateFrom || filterDateTo;

    // --- DATA LOADING ---
    // --- DATA LOADING (OPTIMIZED) ---
    const loadData = async () => {
        setIsLoading(true);
        try {
            const weekStartStr = format(currentWeekDates[0], 'yyyy-MM-dd');
            const weekEndStr = format(currentWeekDates[6], 'yyyy-MM-dd');

            // 🚀 FIX 1: Fetch Employees and Shifts at the EXACT SAME TIME
            const [empResponse, shiftResponse] = await Promise.all([
                api.get('/emp/employees').catch(() => ({ data: [] })),
                api.get('/scheduling/scheduled_shift/positions').catch(() => ({ data: [] }))
            ]);

            const empData = Array.isArray(empResponse.data) ? empResponse.data : (empResponse.data?.data || []);
            const allShifts = shiftResponse.data?.data || shiftResponse.data || [];

            // 🚀 FIX 2: Filter the heavy array efficiently
            const weekShifts = allShifts.filter(s =>
                s.date >= weekStartStr && s.date <= weekEndStr
            );

            const mappedShifts = weekShifts.map(shift => ({
                id: shift.shiftId,
                empId: shift.employeeId,
                date: shift.date,
                dayOfWeek: shift.dayOfWeek,
                start: shift.startTime.substring(0, 5),
                end: shift.endTime.substring(0, 5),
                positionName: shift.positionName || 'Staff',
                employeeName: shift.fullName
            }));

            setSchedule(mappedShifts);

            // Strict Role Filtering
            const filteredEmpData = empData.filter(emp => {
                const roleId = emp.roleId || emp.role_id || emp.role;

                if (roleId === 3 || roleId === '3') return true;
                if (roleId === 1 || roleId === 2 || roleId === '1' || roleId === '2') return false;

                let roleStr = '';
                if (typeof emp.role === 'string') roleStr = emp.role;
                else if (Array.isArray(emp.roles) && emp.roles[0]) roleStr = emp.roles[0].name || emp.roles[0].authority || '';
                else if (emp.role && emp.role.name) roleStr = emp.role.name;

                roleStr = roleStr.toUpperCase();
                if (roleStr.includes('ADMIN') || roleStr.includes('MANAGER')) return false;

                return true;
            });

            const mappedEmployees = filteredEmpData.map(emp => {
                const empId = emp.id || emp.userId || emp.employeeId;
                const position = emp.positionName || emp.position || 'Staff';
                return {
                    id: empId,
                    name: emp.fullName || emp.name || 'Unknown Employee',
                    position
                };
            });

            setEmployees(mappedEmployees);

        } catch (error) {
            console.error("Failed to load published schedule", error);
            showToast("Failed to load published schedule.", "error"); // Uses your new toast!
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [weekStart]);

    // --- NAVIGATION ---
    const handlePreviousWeek = () => setWeekStart(prev => subDays(prev, 7));
    const handleNextWeek = () => setWeekStart(prev => addDays(prev, 7));

    // --- SHIFT ACTIONS ---
    const handleShiftClick = (shift) => {
        setCurrentShift({ ...shift });
        setIsDeleteModalOpen(true);
    };

    const confirmDelete = async () => {
        try {
            await api.delete(`/schedule/${currentShift.id}`);

            setIsDeleteModalOpen(false);
            setCurrentShift(null);

            showToast("Published shift removed successfully", "success");

            loadData();
        } catch (error) {
            console.error("Delete error:", error.response?.status, error.response?.data);
            showToast("Failed to delete shift.", "error");
        }
    };

    // --- RENDER ---
    return (
        <div className="space-y-6 relative">

            {/* HEADER */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Published Schedule</h1>
                    <p className="text-gray-500">View and manage the final confirmed schedule for each week.</p>
                </div>
                <div className="flex items-center gap-2">
                    {/* Optional: Add header buttons here if needed */}
                </div>
            </div>

            {/* FILTER BAR */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
                <div className="flex flex-col md:flex-row gap-4 items-end">
                    <div className="flex-1">
                        <label className="text-xs font-semibold text-gray-500 mb-1.5 block">Search Employee</label>
                        <div className="relative">
                            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input
                                type="text"
                                placeholder="Type a name..."
                                value={searchName}
                                onChange={e => setSearchName(e.target.value)}
                                className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm outline-none focus:border-green-400 focus:ring-1 focus:ring-green-100 transition-all"
                            />
                        </div>
                    </div>
                    <div className="flex-1">
                        <label className="text-xs font-semibold text-gray-500 mb-1.5 block">Date From</label>
                        <div className="relative">
                            <CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input
                                type="date"
                                value={filterDateFrom}
                                onChange={e => setFilterDateFrom(e.target.value)}
                                className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm outline-none focus:border-green-400 focus:ring-1 focus:ring-green-100 transition-all"
                            />
                        </div>
                    </div>
                    <div className="flex-1">
                        <label className="text-xs font-semibold text-gray-500 mb-1.5 block">Date To</label>
                        <div className="relative">
                            <CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input
                                type="date"
                                value={filterDateTo}
                                onChange={e => setFilterDateTo(e.target.value)}
                                min={filterDateFrom || undefined}
                                className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm outline-none focus:border-green-400 focus:ring-1 focus:ring-green-100 transition-all"
                            />
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleClearFilters}
                            disabled={!hasActiveFilters}
                            className={`flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium border transition-all h-[42px] ${
                                hasActiveFilters ? 'border-gray-300 text-gray-700 bg-white hover:bg-gray-50' : 'border-gray-200 text-gray-400 bg-gray-50 cursor-not-allowed'
                            }`}
                        >
                            <X size={15} /> Clear
                        </button>
                        <button
                            onClick={handleApplyFilters}
                            disabled={!hasActiveFilters}
                            className={`flex items-center justify-center gap-2 px-5 py-2.5 rounded-lg text-sm font-medium border transition-all h-[42px] ${
                                hasActiveFilters ? 'border-green-600 text-white bg-green-600 hover:bg-green-700 shadow-sm' : 'border-gray-200 text-gray-400 bg-gray-50 cursor-not-allowed'
                            }`}
                        >
                            <Check size={15} /> Apply
                        </button>
                    </div>
                </div>
            </div>

            {/* CALENDAR TABLE */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                    <h3 className="font-bold text-gray-800 text-lg flex items-center gap-2">
                        <CalendarDays size={20} className="text-gray-500" />
                        {format(currentWeekDates[0], 'MMM d')} – {format(currentWeekDates[6], 'MMM d, yyyy')}
                    </h3>
                    <div className="flex items-center gap-2">
                        <button onClick={handlePreviousWeek} className="p-1.5 hover:bg-gray-200 rounded text-gray-600 transition-colors"><ChevronLeft size={20} /></button>
                        <button onClick={() => setWeekStart(startOfWeek(new Date(), { weekStartsOn: 0 }))} className="px-3 py-1 text-sm font-medium hover:bg-gray-200 rounded text-gray-600 transition-colors">Today</button>
                        <button onClick={handleNextWeek} className="p-1.5 hover:bg-gray-200 rounded text-gray-600 transition-colors"><ChevronRight size={20} /></button>
                    </div>
                </div>

                {/*<div className="px-4 py-2 bg-green-50 border-b border-green-100 flex items-center gap-2">*/}
                {/*    <div className="w-1.5 h-1.5 rounded-full bg-green-500"></div>*/}
                {/*    <span className="text-xs font-medium text-green-700">Showing confirmed shifts from the final schedule table. Click a shift to delete it.</span>*/}
                {/*</div>*/}

                {isLoading ? (
                    <div className="flex justify-center py-20">
                        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600"></div>
                    </div>
                ) : (
                    <div className="overflow-x-auto overflow-y-auto max-h-[75vh] custom-scrollbar bg-white relative">
                        <table className="w-full min-w-[1400px] border-collapse">
                            <thead className="sticky top-0 z-40 bg-gray-50 shadow-sm">
                            <tr>
                                <th className="p-4 text-left text-xs font-semibold text-gray-500 uppercase w-32 sticky left-0 z-50 bg-gray-50 border-b border-gray-200 shadow-[1px_0_0_0_#e5e7eb]">Position</th>
                                <th className="p-4 text-left text-xs font-semibold text-gray-500 uppercase w-48 sticky left-[128px] z-50 bg-gray-50 border-b border-gray-200 shadow-[1px_0_0_0_#e5e7eb]">Employee</th>
                                {currentWeekDates.map(date => (
                                    <th key={date.toString()} className="p-4 text-center text-xs font-semibold text-gray-500 uppercase min-w-[140px] bg-gray-50 border-b border-gray-200">
                                        <div className="flex flex-col">
                                            <span>{format(date, 'EEEE')}</span>
                                            <span className="text-gray-900 font-bold mt-0.5">{format(date, 'MMM d')}</span>
                                        </div>
                                    </th>
                                ))}
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                            {filteredEmployees.map(employee => (
                                <tr key={employee.id} className="hover:bg-gray-50 transition-colors">
                                    <td className="p-4 text-sm font-medium text-gray-600 bg-white sticky left-0 z-20 shadow-[1px_0_0_0_#e5e7eb] truncate">
                                        {employee.position}
                                    </td>
                                    <td className="p-4 font-medium text-gray-900 bg-white sticky left-[128px] z-20 flex items-center gap-3 shadow-[1px_0_0_0_#e5e7eb]">
                                        <div className="w-8 h-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold text-sm shrink-0">
                                            {employee.name?.charAt(0).toUpperCase() ?? '?'}
                                        </div>
                                        <span className="truncate">{employee.name}</span>
                                    </td>
                                    {currentWeekDates.map(date => {
                                        const dateStr = format(date, 'yyyy-MM-dd');
                                        const cellShifts = schedule.filter(
                                            s => s.empId === employee.id && s.date === dateStr && isShiftVisible(s)
                                        );
                                        return (
                                            <td key={dateStr} className="p-2 text-center h-24 relative border-l border-gray-100 align-top">
                                                {cellShifts.length > 0 ? (
                                                    <div className="flex flex-col gap-2 h-full justify-center">
                                                        {cellShifts.map(shift => (
                                                            <button
                                                                key={shift.id}
                                                                onClick={() => handleShiftClick(shift)}
                                                                className="group flex flex-col items-center justify-center p-2 rounded-lg transition-all shadow-sm w-full border border-green-200 bg-green-50 hover:bg-red-50 hover:border-red-200"
                                                            >
                                                                <span className="text-[10px] uppercase font-bold text-green-600 mb-0.5 group-hover:text-red-500">Published</span>
                                                                <span className="text-sm font-bold text-green-800 group-hover:text-red-700">{shift.start} – {shift.end}</span>
                                                            </button>
                                                        ))}
                                                    </div>
                                                ) : (
                                                    <div className="h-full flex items-center justify-center">
                                                        <span className="text-gray-300">–</span>
                                                    </div>
                                                )}
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}

                            {filteredEmployees.length === 0 && (
                                <tr>
                                    <td colSpan="9" className="p-12 text-center">
                                        <div className="flex flex-col items-center justify-center text-gray-400">
                                            <Search size={40} className="mb-3 text-gray-300" />
                                            <p className="text-base font-medium text-gray-500">No employees match your search.</p>
                                            <p className="text-sm mt-1">Try a different name or clear the filters.</p>
                                        </div>
                                    </td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* DELETE CONFIRMATION MODAL */}
            {isDeleteModalOpen && currentShift && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-sm p-6">
                        <div className="flex flex-col items-center text-center">
                            <div className="bg-red-100 p-3 rounded-full text-red-600 mb-4">
                                <AlertCircle size={32} />
                            </div>
                            <h3 className="text-lg font-bold text-gray-900 mb-2">Remove Published Shift?</h3>
                            <p className="text-gray-500 text-sm mb-1">
                                <span className="font-medium text-gray-700">{currentShift.employeeName}</span>
                            </p>
                            <p className="text-gray-400 text-xs mb-6">
                                {currentShift.date} · {currentShift.start} – {currentShift.end}
                            </p>
                            <p className="text-gray-500 text-sm mb-6">
                                This will permanently remove the shift from the final schedule.
                            </p>
                            <div className="flex gap-3 w-full">
                                <Button className="flex-1 justify-center" variant="outline" onClick={() => { setIsDeleteModalOpen(false); setCurrentShift(null); }}>
                                    Cancel
                                </Button>
                                <Button className="flex-1 justify-center bg-red-600 hover:bg-red-700 text-white" onClick={confirmDelete}>
                                    Remove
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- UNIFIED STANDARD TOAST NOTIFICATION --- */}
            {toast.show && (
                <div className="fixed bottom-5 right-5 z-[100] animate-in slide-in-from-right-10 duration-300">
                    <div className={`flex items-center gap-3 px-6 py-4 rounded-lg shadow-2xl border ${
                        toast.type === 'error'
                            ? 'bg-red-50 border-red-200 text-red-800'
                            : 'bg-green-50 border-green-200 text-green-800'
                    }`}>
                        {toast.type === 'error' ? <AlertCircle size={20} /> : <Send size={20} />}
                        <p className="font-semibold text-sm">{toast.message}</p>
                        <button
                            onClick={() => setToast({ ...toast, show: false })}
                            className="ml-4 opacity-50 hover:opacity-100 transition-opacity"
                        >
                            <X size={18} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PublishedSchedule;