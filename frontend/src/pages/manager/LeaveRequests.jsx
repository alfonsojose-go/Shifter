import React, { useState, useEffect } from 'react';
import { Check, X, Clock, CalendarDays, RefreshCw, Filter, Send, AlertCircle } from 'lucide-react';
import Button from '../../components/common/Button';
import RequestApi from '../../api/requestApi';
import { format, parseISO } from 'date-fns';

const LeaveRequests = () => {
    const [requests, setRequests] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('PENDING');

    // --- TOAST STATE ---
    const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

    // --- DATA LOADING ---
    const loadRequests = async () => {
        setIsLoading(true);
        try {
            const data = await RequestApi.getAllRequests();
            const sortedData = (data || []).sort((a, b) => b.id - a.id);
            setRequests(sortedData);
        } catch (error) {
            console.error("Failed to load requests:", error);
            showToast("Failed to load requests from server.", "error");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadRequests();
    }, []);

    // --- HANDLERS ---
    const showToast = (message, type = 'success') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ show: false, message: '', type: 'success' }), 3000);
    };

    const handleAction = async (id, newStatus) => {
        try {
            await RequestApi.updateRequestStatus(id, newStatus);

            // Trigger the standard notification toast
            if (newStatus === 'APPROVED') {
                showToast('Request Approved Successfully', 'success');
            } else {
                showToast('Request Rejected', 'error');
            }

            // Refresh the grid
            loadRequests();
        } catch (error) {
            console.error(`Failed to mark request as ${newStatus}:`, error);
            showToast(`Failed to update request status.`, 'error');
        }
    };

    // --- FORMATTERS ---
    const formatType = (typeEnum) => {
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

    const getTypeColor = (typeEnum) => {
        switch(typeEnum) {
            case 'SICK_DAY': return 'bg-red-50 text-red-600 border-red-200';
            case 'LEAVE_OF_ABSENCE': return 'bg-purple-50 text-purple-600 border-purple-200';
            case 'SHIFT_SWAP': return 'bg-orange-50 text-orange-600 border-orange-200';
            case 'WANT_TO_WORK': return 'bg-blue-50 text-blue-600 border-blue-200';
            default: return 'bg-gray-50 text-gray-600 border-gray-200';
        }
    };

    const formatDateTime = (req) => {
        try {
            if (req.startDate && req.endDate) {
                return `${format(parseISO(req.startDate), 'MMM do')} - ${format(parseISO(req.endDate), 'MMM do, yyyy')}`;
            }
            if (req.date) {
                let timeString = '';
                if (req.startTime && req.endTime) {
                    timeString = ` (${req.startTime.substring(0,5)} - ${req.endTime.substring(0,5)})`;
                }
                return `${format(parseISO(req.date), 'MMM do, yyyy')}${timeString}`;
            }
            return 'N/A';
        } catch (e) {
            return 'Invalid Date';
        }
    };

    // --- FILTERING ---
    const filteredRequests = requests.filter(req => {
        if (activeTab === 'PENDING') return req.status === 'PENDING';
        if (activeTab === 'HISTORY') return req.status === 'APPROVED' || req.status === 'REJECTED';
        return true;
    });

    // --- RENDER ---
    return (
        <div className="space-y-6 relative">

            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Request Center</h1>
                    <p className="text-gray-500">Review and manage time-off, availability, and shift swaps.</p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        icon={RefreshCw}
                        onClick={loadRequests}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Updating...' : 'Refresh Inbox'}
                    </Button>
                </div>
            </div>

            {/* TAB NAVIGATION */}
            <div className="flex items-center gap-6 border-b border-gray-200">
                <button
                    onClick={() => setActiveTab('PENDING')}
                    className={`pb-3 text-sm font-semibold transition-colors relative ${activeTab === 'PENDING' ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}
                >
                    Pending Inbox
                    {activeTab === 'PENDING' && <div className="absolute bottom-0 left-0 w-full h-0.5 bg-blue-600 rounded-t-full"></div>}
                </button>
                <button
                    onClick={() => setActiveTab('HISTORY')}
                    className={`pb-3 text-sm font-semibold transition-colors relative ${activeTab === 'HISTORY' ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}
                >
                    Request History
                    {activeTab === 'HISTORY' && <div className="absolute bottom-0 left-0 w-full h-0.5 bg-blue-600 rounded-t-full"></div>}
                </button>
            </div>

            {/* REQUESTS LIST */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">

                {/* UX ENHANCEMENT: Scrollable Body, Sticky Header */}
                <div className="overflow-x-auto overflow-y-auto max-h-[75vh] custom-scrollbar bg-white relative">
                    <table className="w-full text-left border-collapse min-w-[900px]">
                        <thead className="sticky top-0 z-40 bg-gray-50 shadow-sm border-b border-gray-200">
                        <tr>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Employee</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Request Type</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Date / Time</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Reason</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Status</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 text-right bg-gray-50 border-b border-gray-200">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {filteredRequests.map(req => (
                            <tr key={req.id} className="hover:bg-gray-50 transition-colors">
                                <td className="p-4 text-sm font-medium text-gray-900 bg-white">
                                    {req.employeeName || `Employee #${req.userId}`}
                                </td>
                                <td className="p-4 bg-white">
                                    <span className={`px-2.5 py-1 text-xs font-semibold rounded-md border ${getTypeColor(req.type)}`}>
                                        {formatType(req.type)}
                                    </span>
                                    {req.type === 'SHIFT_SWAP' && req.swapWithUserId && (
                                        <span className="block mt-1 text-xs text-gray-400">
                                            Swapping w/ ID: {req.swapWithUserId}
                                        </span>
                                    )}
                                </td>
                                <td className="p-4 text-sm text-gray-600 bg-white">
                                    <div className="flex items-center gap-1.5">
                                        <CalendarDays size={14} className="text-gray-400" />
                                        {formatDateTime(req)}
                                    </div>
                                </td>
                                <td className="p-4 text-sm text-gray-600 max-w-xs truncate bg-white" title={req.reason}>
                                    {req.reason || <span className="text-gray-400 italic">No reason provided</span>}
                                </td>
                                <td className="p-4 bg-white">
                                    {req.status === 'PENDING' && <span className="text-yellow-600 font-semibold text-sm flex items-center gap-1"><Clock size={14}/> Pending</span>}
                                    {req.status === 'APPROVED' && <span className="text-green-600 font-semibold text-sm flex items-center gap-1"><Check size={14}/> Approved</span>}
                                    {req.status === 'REJECTED' && <span className="text-red-600 font-semibold text-sm flex items-center gap-1"><X size={14}/> Rejected</span>}
                                </td>
                                <td className="p-4 text-right bg-white">
                                    {req.status === 'PENDING' ? (
                                        <div className="flex justify-end gap-2">
                                            <button
                                                onClick={() => handleAction(req.id, 'APPROVED')}
                                                className="p-1.5 text-green-600 bg-green-50 hover:bg-green-100 rounded-md transition-colors"
                                                title="Approve"
                                            >
                                                <Check size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleAction(req.id, 'REJECTED')}
                                                className="p-1.5 text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-colors"
                                                title="Reject"
                                            >
                                                <X size={18} />
                                            </button>
                                        </div>
                                    ) : (
                                        <span className="text-gray-400 text-sm">Actioned</span>
                                    )}
                                </td>
                            </tr>
                        ))}

                        {isLoading && filteredRequests.length === 0 && (
                            <tr>
                                <td colSpan="6" className="p-12 text-center bg-white">
                                    <div className="flex flex-col items-center justify-center text-gray-400">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mb-4"></div>
                                        <p className="text-sm">Fetching requests...</p>
                                    </div>
                                </td>
                            </tr>
                        )}

                        {!isLoading && filteredRequests.length === 0 && (
                            <tr>
                                <td colSpan="6" className="p-12 text-center bg-white">
                                    <div className="flex flex-col items-center justify-center text-gray-400">
                                        <Filter size={40} className="mb-3 text-gray-300" />
                                        <p className="text-base font-medium text-gray-500">No {activeTab.toLowerCase()} requests found.</p>
                                        <p className="text-sm mt-1">You're all caught up!</p>
                                    </div>
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* --- STANDARD TOAST NOTIFICATION --- */}
            {toast.show && (
                <div className="fixed bottom-5 right-5 z-[100] animate-in slide-in-from-right-10 duration-300">
                    <div className={`flex items-center gap-3 px-6 py-4 rounded-lg shadow-2xl border ${
                        toast.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' : 'bg-green-50 border-green-200 text-green-800'
                    }`}>
                        {toast.type === 'error' ? <AlertCircle size={20} /> : <Send size={20} />}
                        <p className="font-semibold text-sm">{toast.message}</p>
                        <button onClick={() => setToast({ ...toast, show: false })} className="ml-4 opacity-50 hover:opacity-100 transition-opacity"><X size={18} /></button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default LeaveRequests;