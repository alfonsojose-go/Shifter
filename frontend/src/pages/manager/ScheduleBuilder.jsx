import React, { useState, useEffect } from 'react';
import {
    Plus, AlertCircle, Clock, X, Trash2,
    ChevronLeft, ChevronRight, User, RefreshCw, CalendarDays,
    Wand2, Send
} from 'lucide-react';
import { format, startOfWeek, addDays, subDays } from 'date-fns';
import Button from '../../components/common/Button';
import api from '../../api/axiosConfig';

const ScheduleBuilder = () => {
    // --- STATE ---
    const [weekStart, setWeekStart] = useState(startOfWeek(new Date(), { weekStartsOn: 0 }));
    const currentWeekDates = Array.from({ length: 7 }).map((_, i) => addDays(weekStart, i));
    const [employees, setEmployees] = useState([]);
    const [schedule, setSchedule] = useState([]);
    const [publishedShifts, setPublishedShifts] = useState([]);

    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

    const [isRefreshing, setIsRefreshing] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);

    const [currentShift, setCurrentShift] = useState(null);
    const [newShift, setNewShift] = useState({ empId: '', date: '', start: '', end: '' });

    const [toast, setToast] = useState({ show: false, message: '', type: 'error' });

    const showToast = (message, type = 'error') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ show: false, message: '', type: 'error' }), 6000);
    };

    // Helper to safely pull exact error messages from the backend
    const extractErrorMessage = (error, fallback) => {
        const data = error.response?.data;
        if (typeof data === 'string') return data;
        return data?.message || data?.error || error.message || fallback;
    };

    const getDayOfWeekString = (dateInput) => {
        const localDate = new Date(dateInput.replace(/-/g, '\/'));
        return format(localDate, 'EEEE').toUpperCase();
    };

    const formatTimeForBackend = (timeStr) => {
        if (!timeStr) return "00:00";
        return timeStr.substring(0, 5);
    };

    // --- DATA LOADING ---
    const loadData = async () => {
        setIsRefreshing(true);
        try {
            const weekStartStr = format(currentWeekDates[0], 'yyyy-MM-dd');

            const [empResponse, pubResponse] = await Promise.all([
                api.get('/emp/employees').catch(() => ({ data: [] })),
                api.get(`/schedule/week?weekStart=${weekStartStr}`).catch(() => ({ data: [] }))
            ]);

            const empData = Array.isArray(empResponse.data) ? empResponse.data : (empResponse.data?.data || []);
            const pubShifts = pubResponse.data?.data || pubResponse.data || [];
            setPublishedShifts(pubShifts);

            const scheduleResponse = await api.get('/scheduling').catch(() => ({ data: [] }));
            const rawData = scheduleResponse.data;
            const backendShifts = Array.isArray(rawData) ? rawData : (rawData?.data || []);

            const mappedShifts = backendShifts.map(shift => {
                const matchingDateObj = currentWeekDates.find(
                    d => format(d, 'EEEE').toUpperCase() === (shift.dayOfWeek || '').toUpperCase()
                );
                return {
                    id: shift.id ?? shift.scheduleId ?? shift.schedulingId,
                    empId: shift.employeeId,
                    date: matchingDateObj ? format(matchingDateObj, 'yyyy-MM-dd') : '',
                    dayOfWeek: shift.dayOfWeek,
                    start: shift.startTime ? shift.startTime.substring(0, 5) : '',
                    end: shift.endTime ? shift.endTime.substring(0, 5) : '',
                    positionName: shift.positionName || 'Staff',
                    isDraft: true
                };
            }).filter(s => s.date !== '');

            const draftsToDisplay = mappedShifts.filter(draft => {
                const isAlreadyPublished = pubShifts.some(pub =>
                    String(pub.employeeId) === String(draft.empId) &&
                    String(pub.date) === String(draft.date) &&
                    (pub.startTime || '').substring(0, 5) === (draft.start || '').substring(0, 5) &&
                    (pub.endTime || '').substring(0, 5) === (draft.end || '').substring(0, 5)
                );
                return !isAlreadyPublished;
            });

            setSchedule(draftsToDisplay);

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

            const mappedEmployees = filteredEmpData.map(emp => ({
                id: emp.id || emp.userId || emp.employeeId,
                name: emp.fullName || emp.name || 'Unknown Employee',
                position: emp.position || emp.positionName || 'Staff'
            }));

            setEmployees(mappedEmployees);

        } catch (error) {
            console.error("Failed to load data", error);
        } finally {
            setIsRefreshing(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [weekStart]);

    const checkConflicts = async (empId, date, startTime, endTime, excludeShiftId = null) => {
        const dayOfWeek = getDayOfWeekString(date).trim().toUpperCase();
        try {
            const availResponse = await api.get(`/employee/availabilities/employee/${empId}`);
            const availabilityRules = Array.isArray(availResponse.data) ? availResponse.data : (availResponse.data?.data || []);
            const dayRule = availabilityRules.find(rule => (rule.dayOfWeek || "").trim().toUpperCase() === dayOfWeek);

            if (!dayRule) return { hasConflict: true, message: `No availability record found for ${dayOfWeek}.` };

            const requestResponse = await api.get(`/requests/user/${empId}`);
            const employeeRequests = Array.isArray(requestResponse.data) ? requestResponse.data : (requestResponse.data?.data || []);
            const leaveConflict = employeeRequests.find(req => req.date === date && req.status?.toUpperCase() === 'APPROVED');
            if (leaveConflict) return { hasConflict: true, message: "Approved leave request exists for this date." };

            const isDraftOverlapping = schedule.some(shift => {
                if (excludeShiftId && String(shift.id) === String(excludeShiftId)) return false;
                if (String(shift.empId) === String(empId) && shift.date === date) {
                    return (startTime < shift.end) && (endTime > shift.start);
                }
                return false;
            });
            if (isDraftOverlapping) return { hasConflict: true, message: "Overlaps with another draft shift." };

            const isPublishedOverlapping = publishedShifts.some(pub => {
                if (String(pub.employeeId) === String(empId) && String(pub.date) === date) {
                    const pubStart = (pub.startTime || '').substring(0, 5);
                    const pubEnd = (pub.endTime || '').substring(0, 5);
                    return (startTime < pubEnd) && (endTime > pubStart);
                }
                return false;
            });
            if (isPublishedOverlapping) return { hasConflict: true, message: "Overlaps with an already published shift." };

            return { hasConflict: false };
        } catch (error) {
            return { hasConflict: true, message: "Error verifying availability." };
        }
    };

    // --- 🚀 SEQUENTIAL PUBLISH HANDLER ---
    const handlePublish = async () => {
        const draftsToPublish = schedule.filter(shift => shift.isDraft);
        if (draftsToPublish.length === 0) return;

        setIsPublishing(true);
        try {
            let successCount = 0;
            let failCount = 0;
            let lastErrorMessage = "";

            // Process one by one so successful ones publish, and failures stay as drafts
            for (const draft of draftsToPublish) {
                try {
                    // 1. Post to calendar
                    await api.post('/scheduled-shifts', {
                        employeeId: parseInt(draft.empId),
                        date: draft.date,
                        dayOfWeek: getDayOfWeekString(draft.date),
                        startTime: formatTimeForBackend(draft.start) + ':00',
                        endTime: formatTimeForBackend(draft.end) + ':00'
                    });

                    // 2. If successful, delete the draft template
                    if (!String(draft.id).startsWith('draft-')) {
                        await api.delete(`/scheduling/${draft.id}`).catch(() => {});
                    }
                    successCount++;
                } catch (error) {
                    failCount++;
                    lastErrorMessage = extractErrorMessage(error, "Overlap detected");
                }
            }

            // Provide a clear summary to the user
            if (failCount === 0) {
                showToast(`Successfully published ${successCount} shifts!`, 'success');
            } else if (successCount > 0) {
                showToast(`Published ${successCount} shifts. ${failCount} failed: ${lastErrorMessage}`, 'error');
            } else {
                showToast(`Publishing failed: ${lastErrorMessage}`, 'error');
            }

            // Reload data. Failures will remain perfectly intact as drafts!
            await loadData();
        } catch (error) {
            console.error("Master publishing error:", error);
            showToast("An unexpected error occurred during publishing.", 'error');
        } finally {
            setIsPublishing(false);
        }
    };

    // --- HANDLERS FOR CREATING & EDITING ---
    const handleAddClick = () => {
        setNewShift({ empId: employees[0]?.id || '', date: format(currentWeekDates[0], 'yyyy-MM-dd'), start: '09:00', end: '17:00' });
        setIsAddModalOpen(true);
    };

    const saveNewShift = async () => {
        const conflict = await checkConflicts(newShift.empId, newShift.date, newShift.start, newShift.end);
        if (conflict.hasConflict) { showToast(conflict.message, 'error'); return; }

        try {
            await api.post('/scheduling', {
                employeeId: parseInt(newShift.empId),
                schedules: [{
                    dayOfWeek: getDayOfWeekString(newShift.date),
                    startTime: formatTimeForBackend(newShift.start) + ':00',
                    endTime: formatTimeForBackend(newShift.end) + ':00'
                }]
            });
            showToast("Draft shift added successfully", "success");
            loadData();
            setIsAddModalOpen(false);
        } catch (error) {
            const errorMsg = extractErrorMessage(error, "Failed to add draft shift");
            showToast(errorMsg, "error");
        }
    };

    const saveEditedShift = async () => {
        const conflict = await checkConflicts(currentShift.empId, currentShift.date, currentShift.start, currentShift.end, currentShift.id);
        if (conflict.hasConflict) { showToast(conflict.message, 'error'); return; }

        if (currentShift.id.toString().startsWith('draft-')) {
            setSchedule(schedule.map(s => s.id === currentShift.id ? currentShift : s));
            setIsEditModalOpen(false);
        } else {
            try {
                await api.put(`/scheduling/${currentShift.id}`, {
                    dayOfWeek: getDayOfWeekString(currentShift.date),
                    startTime: formatTimeForBackend(currentShift.start) + ':00',
                    endTime: formatTimeForBackend(currentShift.end) + ':00'
                });

                showToast("Draft updated successfully", "success");
                loadData();
                setIsEditModalOpen(false);
            } catch (error) {
                // If it fails, the modal stays open so the user doesn't lose their edits!
                const errorMsg = extractErrorMessage(error, "Update failed. The shift may have invalid times.");
                showToast(`Update failed: ${errorMsg}`, "error");
            }
        }
    };

    const confirmDelete = async () => {
        if (currentShift.id.toString().startsWith('draft-')) {
            setSchedule(schedule.filter(s => s.id !== currentShift.id));
        } else {
            try {
                await api.delete(`/scheduling/${currentShift.id}`);
                showToast("Draft deleted", "success");
                loadData();
            } catch (error) {
                const errorMsg = extractErrorMessage(error, "Failed to delete draft");
                showToast(errorMsg, "error");
            }
        }
        setIsDeleteModalOpen(false);
        setIsEditModalOpen(false);
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Schedule Builder</h1>
                    <p className="text-gray-500 text-sm text-balance">Draft and publish shifts. Conflict detection is active.</p>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline" onClick={loadData} disabled={isRefreshing}>
                        {isRefreshing ? 'Updating...' : 'Refresh'}
                    </Button>
                    <Button variant="primary" icon={Plus} onClick={handleAddClick}>Add Shift</Button>
                    <Button icon={Send} onClick={handlePublish} disabled={isPublishing || schedule.filter(s => s.isDraft).length === 0} className="bg-orange-600 hover:bg-orange-700 text-white">
                        {isPublishing ? "Publishing..." : `Publish (${schedule.filter(s => s.isDraft).length})`}
                    </Button>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                    <h3 className="font-bold text-gray-800 text-lg flex items-center gap-2">
                        <CalendarDays size={20} className="text-gray-500" />
                        {format(currentWeekDates[0], 'MMM d')} – {format(currentWeekDates[6], 'MMM d, yyyy')}
                    </h3>
                    <div className="flex items-center gap-2">
                        <button onClick={() => setWeekStart(subDays(weekStart, 7))} className="p-1.5 hover:bg-gray-200 rounded"><ChevronLeft size={20} /></button>
                        <button onClick={() => setWeekStart(startOfWeek(new Date(), { weekStartsOn: 0 }))} className="px-3 py-1 text-sm font-medium hover:bg-gray-200 rounded">Today</button>
                        <button onClick={() => setWeekStart(addDays(weekStart, 7))} className="p-1.5 hover:bg-gray-200 rounded"><ChevronRight size={20} /></button>
                    </div>
                </div>

                <div className="overflow-x-auto overflow-y-auto max-h-[75vh] custom-scrollbar bg-white relative">
                    <table className="w-full min-w-[1400px] border-collapse">
                        <thead className="sticky top-0 z-40 bg-gray-50 shadow-sm">
                        <tr>
                            <th className="p-4 text-left text-xs font-semibold text-gray-500 uppercase w-32 sticky left-0 z-50 bg-gray-50 border-b border-gray-200 shadow-[1px_0_0_0_#e5e7eb]">Position</th>
                            <th className="p-4 text-left text-xs font-semibold text-gray-500 uppercase w-48 sticky left-[128px] z-50 bg-gray-50 border-b border-gray-200 shadow-[1px_0_0_0_#e5e7eb]">Employee</th>
                            {currentWeekDates.map(date => (
                                <th key={date.toString()} className="p-4 text-center text-xs font-semibold text-gray-500 uppercase min-w-[140px] bg-gray-50 border-b border-gray-200">
                                    {format(date, 'EEEE')}<br/><span className="text-gray-900 font-bold">{format(date, 'MMM d')}</span>
                                </th>
                            ))}
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {employees.map(employee => (
                            <tr key={employee.id} className="hover:bg-gray-50 transition-colors">
                                <td className="p-4 text-sm text-gray-600 bg-white sticky left-0 z-20 shadow-[1px_0_0_0_#e5e7eb]">{employee.position}</td>
                                <td className="p-4 font-medium text-gray-900 bg-white sticky left-[128px] z-20 shadow-[1px_0_0_0_#e5e7eb]">{employee.name}</td>
                                {currentWeekDates.map(date => {
                                    const dateString = format(date, 'yyyy-MM-dd');
                                    const cellShifts = schedule.filter(s => String(s.empId) === String(employee.id) && s.date === dateString);
                                    return (
                                        <td key={dateString} className="p-2 text-center h-24 border-l border-gray-100 align-top">
                                            <div className="flex flex-col gap-2 h-full justify-center">
                                                {cellShifts.map(shift => {
                                                    const hasOverlap = publishedShifts.some(pub =>
                                                        String(pub.employeeId) === String(shift.empId) && String(pub.date) === String(shift.date) &&
                                                        (shift.start < (pub.endTime||'').substring(0,5) && shift.end > (pub.startTime||'').substring(0,5))
                                                    );
                                                    return (
                                                        <button key={shift.id} onClick={() => { setCurrentShift(shift); setIsEditModalOpen(true); }}
                                                                className={`flex flex-col items-center p-2 rounded-lg shadow-sm w-full border-2 ${
                                                                    hasOverlap ? 'border-red-500 bg-red-50 animate-pulse' : 'border-orange-200 bg-orange-50 hover:bg-orange-100'
                                                                }`}>
                                                            {hasOverlap && <span className="text-[10px] uppercase font-bold text-red-600">CONFLICT</span>}
                                                            {!hasOverlap && <span className="text-[10px] uppercase font-bold text-orange-500">Draft</span>}
                                                            <span className={`text-sm font-bold ${hasOverlap ? 'text-red-900' : 'text-orange-900'}`}>{shift.start} - {shift.end}</span>
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        </td>
                                    );
                                })}
                            </tr>
                        ))}
                        {employees.length === 0 && !isRefreshing && (
                            <tr>
                                <td colSpan="9" className="p-8 text-center text-gray-500">
                                    No active employees available to schedule.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* --- MODALS --- */}
            {isAddModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-4">
                        <div className="flex justify-between items-center"><h3 className="text-xl font-bold">Draft New Shift</h3><button onClick={() => setIsAddModalOpen(false)}><X /></button></div>
                        <select value={newShift.empId} onChange={(e) => setNewShift({...newShift, empId: e.target.value})} className="w-full p-3 border rounded-lg">
                            {employees.map(e => <option key={e.id} value={e.id}>{e.name} ({e.position})</option>)}
                        </select>
                        <input type="date" value={newShift.date} onChange={(e) => setNewShift({...newShift, date: e.target.value})} className="w-full p-3 border rounded-lg" />
                        <div className="grid grid-cols-2 gap-4">
                            <input type="time" value={newShift.start} onChange={(e) => setNewShift({...newShift, start: e.target.value})} className="p-3 border rounded-lg" />
                            <input type="time" value={newShift.end} onChange={(e) => setNewShift({...newShift, end: e.target.value})} className="p-3 border rounded-lg" />
                        </div>
                        <div className="flex justify-end gap-3"><Button variant="ghost" onClick={() => setIsAddModalOpen(false)}>Cancel</Button><Button variant="primary" onClick={saveNewShift}>Add Draft</Button></div>
                    </div>
                </div>
            )}

            {isEditModalOpen && currentShift && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-4">
                        <div className="flex justify-between items-center"><h3 className="text-xl font-bold">Edit Shift</h3><button onClick={() => setIsEditModalOpen(false)}><X /></button></div>
                        <input type="date" value={currentShift.date} onChange={(e) => setCurrentShift({...currentShift, date: e.target.value})} className="w-full p-3 border rounded-lg" />
                        <div className="grid grid-cols-2 gap-4">
                            <input type="time" value={currentShift.start} onChange={(e) => setCurrentShift({...currentShift, start: e.target.value})} className="p-3 border rounded-lg" />
                            <input type="time" value={currentShift.end} onChange={(e) => setCurrentShift({...currentShift, end: e.target.value})} className="p-3 border rounded-lg" />
                        </div>
                        <div className="flex justify-between items-center">
                            <button onClick={() => setIsDeleteModalOpen(true)} className="text-red-600 flex items-center gap-2"><Trash2 size={18} /> Delete</button>
                            <div className="flex gap-3"><Button variant="ghost" onClick={() => setIsEditModalOpen(false)}>Cancel</Button><Button variant="primary" onClick={saveEditedShift}>Update</Button></div>
                        </div>
                    </div>
                </div>
            )}

            {isDeleteModalOpen && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-sm p-6 text-center">
                        <AlertCircle size={32} className="text-red-600 mx-auto mb-4" />
                        <h3 className="text-lg font-bold mb-2">Are you sure?</h3>
                        <p className="text-gray-500 text-sm mb-6">This shift will be removed from the draft schedule.</p>
                        <div className="flex gap-3"><Button className="flex-1" variant="outline" onClick={() => setIsDeleteModalOpen(false)}>Cancel</Button><Button className="flex-1 bg-red-600 text-white" onClick={confirmDelete}>Delete</Button></div>
                    </div>
                </div>
            )}

            {toast.show && (
                <div className="fixed bottom-5 right-5 z-[100] animate-in slide-in-from-right-10 duration-300">
                    <div className={`flex items-center gap-3 px-6 py-4 rounded-lg shadow-2xl border ${
                        toast.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' : 'bg-green-50 border-green-200 text-green-800'
                    }`}>
                        {toast.type === 'error' ? <AlertCircle size={20} className="shrink-0" /> : <Send size={20} className="shrink-0" />}
                        <p className="font-semibold">{toast.message}</p>
                        <button onClick={() => setToast({ ...toast, show: false })} className="ml-4 opacity-50 hover:opacity-100 shrink-0"><X size={18} /></button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ScheduleBuilder;