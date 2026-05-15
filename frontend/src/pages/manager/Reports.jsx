import React, { useState, useEffect, forwardRef, useCallback } from 'react';
import { Calendar, Download, DollarSign, Clock, FileJson, AlertTriangle } from 'lucide-react';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import { format, startOfWeek, endOfWeek, parseISO, getDay } from 'date-fns';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import Button from '../../components/common/Button';
import ReportApi from '../../api/reportApi';
import api from '../../api/axiosConfig';

const Reports = () => {
    // --- DATE STATE ---
    const today = new Date();
    const currentWeekStart = startOfWeek(today, { weekStartsOn: 0 });
    const currentWeekEnd = endOfWeek(today, { weekStartsOn: 0 });

    const [dateRange, setDateRange] = useState([currentWeekStart, currentWeekEnd]);
    const [startDate, endDate] = dateRange;

    // --- DATA STATE ---
    const [isLoading, setIsLoading] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);

    // Enhanced Debugging State
    const [rawBackendData, setRawBackendData] = useState(null);
    const [fetchedSchedules, setFetchedSchedules] = useState([]);
    const [showDebug, setShowDebug] = useState(false);

    const [summary, setSummary] = useState({
        scheduledCost: 0,
        actualCost: 0,
        variance: 0,
        variancePercent: "0.00%"
    });

    const [attendanceData, setAttendanceData] = useState([]);
    const [chartData, setChartData] = useState([]);

    // --- MATH HELPERS ---
    const extractMinutes = (timeString) => {
        if (!timeString || timeString === 'N/A' || timeString === '-') return null;
        let timePart = timeString;

        if (timeString.includes('T')) timePart = timeString.split('T')[1];
        else if (timeString.includes(' ')) timePart = timeString.split(' ')[1];

        const parts = timePart.split(':');
        if (parts.length >= 2) {
            return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
        }
        return null;
    };

    const calculateVariance = (sched, actual) => {
        const schedMins = extractMinutes(sched);
        const actualMins = extractMinutes(actual);

        if (schedMins === null || actualMins === null) return '-';

        const diff = actualMins - schedMins;
        if (diff > 0) return `+${diff}m`;
        if (diff < 0) return `${diff}m`;
        return `0m`;
    };

    // Formats ISO 8601 Duration (e.g., PT8H26M57S) into "Xh Ym"
    const formatDuration = (isoString) => {
        if (!isoString || isoString === 'N/A' || isoString === '-') return '-';

        // Regex to extract Hours, Minutes, and Seconds
        const regex = /PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?/;
        const matches = isoString.match(regex);

        if (!matches) return isoString;

        let hours = parseInt(matches[1] || 0, 10);
        let minutes = parseInt(matches[2] || 0, 10);
        const seconds = parseFloat(matches[3] || 0);

        // Round seconds to the nearest minute
        minutes += Math.round(seconds / 60);

        // Handle rollover if rounding seconds pushed minutes to 60
        if (minutes >= 60) {
            hours += Math.floor(minutes / 60);
            minutes = minutes % 60;
        }

        // If they clocked in and out in less than 30 seconds
        if (hours === 0 && minutes === 0 && seconds > 0) {
            return '< 1m';
        }

        return `${hours}h ${minutes}m`;
    };


    // --- DATA LOADING & AGGRESSIVE FRONTEND JOIN ---
    const loadReportData = useCallback(async () => {
        if (!startDate) return;

        setIsLoading(true);
        try {
            const formattedDate = format(startDate, 'yyyy-MM-dd');

            // AGGRESSIVE FETCH: Hit every possible schedule endpoint just to be safe!
            const [reportRes, empRes, shiftRes1, shiftRes2, shiftRes3] = await Promise.all([
                ReportApi.getWeeklySummary(formattedDate).catch(() => ({})),
                api.get('/emp/employees').catch(() => ({ data: [] })),
                api.get('/scheduling').catch(() => ({ data: [] })), // Template schedules
                api.get('/scheduling/scheduled_shift').catch(() => ({ data: [] })), // Singular
                api.get('/scheduling/scheduled_shifts').catch(() => ({ data: [] })) // Plural
            ]);

            const reportData = reportRes || {};
            setRawBackendData(reportData);

            // Extract lists
            const employees = Array.isArray(empRes.data) ? empRes.data : (empRes.data?.data || []);

            // Combine all successfully fetched shifts into one massive pool
            const list1 = Array.isArray(shiftRes1.data) ? shiftRes1.data : (shiftRes1.data?.data || []);
            const list2 = Array.isArray(shiftRes2.data) ? shiftRes2.data : (shiftRes2.data?.data || []);
            const list3 = Array.isArray(shiftRes3.data) ? shiftRes3.data : (shiftRes3.data?.data || []);
            const allScheduledShifts = [...list1, ...list2, ...list3];

            // Save to debug state so we can physically see if the backend sent them!
            setFetchedSchedules(allScheduledShifts);

            // 1. Process Summary Totals
            const schedCost = reportData.totalScheduledCost || reportData.total_scheduled_cost || 0;
            const actCost = reportData.totalActualCost || reportData.total_actual_cost || 0;
            const variance = actCost - schedCost;
            const variancePercent = schedCost > 0 ? ((variance / schedCost) * 100).toFixed(2) : "0.00";

            setSummary({
                scheduledCost: schedCost,
                actualCost: actCost,
                variance: Math.abs(variance),
                isOverBudget: variance > 0,
                variancePercent: `${variance > 0 ? '+' : ''}${variancePercent}%`
            });

            // 2. Process Attendance & Perform the Frontend JOIN + Math
            const rawAttendanceList = reportData.attendanceReport || reportData.attendance_report || [];

            const enrichedAttendance = rawAttendanceList.map(row => {
                const uid = row.userId || row.user_id || row.employeeId || row.employee_id;
                const uidStr = String(uid);

                const clockInStr = row.clockIn || row.clock_in || row.clock_in_time || row.clockInTime || '';
                const clockOutStr = row.clockOut || row.clock_out || row.clock_out_time || row.clockOutTime || '';

                let sDate = row.shiftDate || row.shift_date || row.date || row.work_date;
                if (!sDate && clockInStr && typeof clockInStr === 'string') {
                    sDate = clockInStr.split(' ')[0].split('T')[0];
                }

                const normalizedAttDate = sDate ? sDate.split('T')[0].split(' ')[0] : '';

                let targetDayOfWeek = '';
                if (normalizedAttDate) {
                    const dObj = new Date(normalizedAttDate + 'T12:00:00');
                    const daysOrder = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
                    targetDayOfWeek = daysOrder[dObj.getDay()];
                }

                let finalEmpName = row.employeeName || row.employee_name || row.fullName || row.full_name;
                if (!finalEmpName && uid) {
                    const matchedEmp = employees.find(e => String(e.id || e.userId || e.employeeId) === uidStr);
                    if (matchedEmp) finalEmpName = matchedEmp.fullName || matchedEmp.name || matchedEmp.email;
                }

                // --- FIX: Added row.scheduledStartTime and row.scheduledEndTime to the front ---
                let finalSchedIn = row.scheduledStartTime || row.scheduledIn || row.scheduled_in || row.start_time || row.startTime || row.shiftStart || row.shift_start;
                let finalSchedOut = row.scheduledEndTime || row.scheduledOut || row.scheduled_out || row.end_time || row.endTime || row.shiftEnd || row.shift_end;

                // THE JOIN: If the report didn't have the schedule, grab it from our massive shift pool
                if (!finalSchedIn && uidStr && normalizedAttDate) {
                    const matchedShift = allScheduledShifts.find(s => {
                        const sEmpIdStr = String(s.employeeId || s.employee_id || s.userId || s.user_id);
                        if (sEmpIdStr !== uidStr) return false;

                        // Match by Exact Date first
                        const shiftDateRaw = s.date || s.shiftDate || s.shift_date;
                        const normalizedShiftDate = shiftDateRaw ? shiftDateRaw.split('T')[0].split(' ')[0] : '';
                        if (normalizedShiftDate && normalizedShiftDate === normalizedAttDate) return true;

                        // Match by Day of Week as fallback
                        const sDay = String(s.dayOfWeek || s.day_of_week || '').toUpperCase();
                        if (sDay && sDay === targetDayOfWeek) return true;

                        return false;
                    });

                    if (matchedShift) {
                        finalSchedIn = matchedShift.startTime || matchedShift.start_time || matchedShift.scheduledIn;
                        finalSchedOut = matchedShift.endTime || matchedShift.end_time || matchedShift.scheduledOut;
                    }
                }

                // Calculate Variances
                const calculatedInVar = calculateVariance(finalSchedIn, clockInStr);
                const calculatedOutVar = calculateVariance(finalSchedOut, clockOutStr);

                // --- NEW STATUS MAPPING ---
                // Grab the exact status from the backend, default to 'Unknown' if missing
                let backendStatus = row.attendanceStatus || row.attendance_status || 'Unknown';

                // Format it nicely (e.g., "NO_CLOCK_OUT" -> "No Clock Out", "ON_TIME" -> "On Time")
                let nicelyFormattedStatus = backendStatus
                    .replace(/_/g, ' ')
                    .toLowerCase()
                    .replace(/\b\w/g, char => char.toUpperCase());

                return {
                    ...row,
                    resolvedEmployeeName: finalEmpName || `User #${uid || 'Unknown'}`,
                    resolvedDate: normalizedAttDate,
                    resolvedSchedIn: finalSchedIn,
                    resolvedSchedOut: finalSchedOut,
                    resolvedClockIn: clockInStr,
                    resolvedClockOut: clockOutStr,
                    inVariance: calculatedInVar,
                    outVariance: calculatedOutVar,
                    status: nicelyFormattedStatus, // <-- Using the formatted backend status!
                    resolvedTotalWorked: formatDuration(row.totalWorked || row.total_worked)
                };
            });

            // Deduplicate the array AND filter out completely blank rows
            const uniqueAttendance = [];
            const seenSignatures = new Set();

            enrichedAttendance.forEach(row => {
                // 1. Check if the row actually has any time/date data
                const hasData = row.resolvedDate || row.resolvedClockIn || row.resolvedSchedIn;

                // 2. Only add the row if it has data AND isn't a duplicate
                if (hasData) {
                    const signature = `${row.resolvedEmployeeName}_${row.resolvedDate}_${row.resolvedClockIn}_${row.resolvedClockOut}`;

                    if (!seenSignatures.has(signature)) {
                        seenSignatures.add(signature);
                        uniqueAttendance.push(row);
                    }
                }
            });

            // Replace setAttendanceData(enrichedAttendance) with the unique array
            setAttendanceData(uniqueAttendance);

            // --- END OF NEW CODE ---

            // 3. Process Chart Data
            const daysOrder = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
            const shortDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

            const laborCost = reportData.laborCost || reportData.labor_cost || {};
            const scheduledCostsMap = laborCost.dailyScheduledCost || laborCost.daily_scheduled_cost || {};
            const budgetsMap = laborCost.dailyBudget || laborCost.daily_budget || {};

            const actualCostsMap = { SUNDAY: 0, MONDAY: 0, TUESDAY: 0, WEDNESDAY: 0, THURSDAY: 0, FRIDAY: 0, SATURDAY: 0 };

            rawAttendanceList.forEach(record => {
                const dateString = record.shiftDate || record.shift_date || record.date || record.work_date;
                const actualC = record.actualCost || record.actual_cost || 0;

                if (dateString && actualC) {
                    const dateObj = parseISO(dateString);
                    const dayIndex = getDay(dateObj);
                    const dayName = daysOrder[dayIndex];
                    actualCostsMap[dayName] += actualC;
                }
            });

            const formattedChartData = daysOrder.map((day, index) => ({
                name: shortDays[index],
                Scheduled: scheduledCostsMap[day] || 0,
                Actual: actualCostsMap[day] || 0,
                Budget: budgetsMap[day] || 0
            }));

            setChartData(formattedChartData);

        } catch (error) {
            console.error("Failed to load report data:", error);
        } finally {
            setIsLoading(false);
        }
    }, [startDate]);

    useEffect(() => {
        if (startDate && endDate) {
            loadReportData();
        }
    }, [startDate, endDate, loadReportData]);

    const handleDownloadCsv = async () => {
        if (!startDate) return;
        setIsDownloading(true);
        try {
            const formattedDate = format(startDate, 'yyyy-MM-dd');
            const blobData = await ReportApi.downloadAttendanceCsv(formattedDate);

            const url = window.URL.createObjectURL(new Blob([blobData]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `Attendance_Report_${formattedDate}.csv`);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Failed to download CSV:", error);
            alert("Failed to download the report.");
        } finally {
            setIsDownloading(false);
        }
    };

    const CustomDateInput = forwardRef(({ value, onClick }, ref) => (
        <button onClick={onClick} ref={ref} className="flex items-center gap-2 bg-white border border-gray-200 text-gray-700 px-4 py-2.5 rounded-lg shadow-sm hover:bg-gray-50 focus:ring-2 focus:ring-blue-500 transition-colors">
            <Calendar size={18} className="text-gray-500" />
            <span className="font-medium text-sm">
                {startDate && endDate ? `${format(startDate, 'MMM dd, yyyy')} - ${format(endDate, 'MMM dd, yyyy')}` : "Select Week"}
            </span>
        </button>
    ));

    const getVarianceColor = (varianceStr) => {
        if (!varianceStr || varianceStr === '-') return 'text-gray-500';
        const lowerStr = String(varianceStr).toLowerCase();

        if (lowerStr.includes('early') || lowerStr.includes('on time') || lowerStr.startsWith('-') || lowerStr === '0m') return 'text-green-600';
        if (lowerStr.includes('late') || lowerStr.startsWith('+')) return 'text-red-500';

        return 'text-gray-500';
    };

    const getStatusPill = (status) => {
        const safeStatus = status || 'Unknown';
        const lowerStatus = safeStatus.toLowerCase();

        if (lowerStatus.includes('on time') || lowerStatus.includes('early')) {
            return <span className="px-3 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-full">{safeStatus}</span>;
        }
        if (lowerStatus.includes('late')) {
            return <span className="px-3 py-1 bg-red-100 text-red-700 text-xs font-bold rounded-full">{safeStatus}</span>;
        }
        if (lowerStatus.includes('absent')) {
            return <span className="px-3 py-1 bg-gray-800 text-white text-xs font-bold rounded-full">{safeStatus}</span>;
        }
        if (lowerStatus.includes('no clock out')) {
            return <span className="px-3 py-1 bg-yellow-100 text-yellow-700 text-xs font-bold rounded-full">{safeStatus}</span>;
        }

        return <span className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-bold rounded-full">{safeStatus}</span>;
    };

    const formatTime = (timeString) => {
        if (!timeString || timeString === 'N/A' || timeString === '-') return timeString;
        if (timeString.includes('T')) return timeString.split('T')[1].substring(0, 5);
        if (timeString.includes(' ')) return timeString.split(' ')[1].substring(0, 5);
        return timeString.substring(0, 5);
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Labor Reports</h1>
                    <p className="text-gray-500">Analyze labor costs and efficiency for the selected date range.</p>

                    {/* --- COMMENTED OUT ADVANCED DEBUG TOGGLE --- */}
                    {/*
                    <div className="mt-2 flex items-center gap-2">
                        <input type="checkbox" id="debugToggle" checked={showDebug} onChange={(e) => setShowDebug(e.target.checked)} className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"/>
                        <label htmlFor="debugToggle" className="text-xs font-semibold text-gray-600 flex items-center gap-1 cursor-pointer hover:text-blue-600">
                            <FileJson size={14} /> Show Advanced Debug View
                        </label>
                    </div>
                    */}
                </div>

                <div className="flex items-center gap-3 relative z-50">
                    <DatePicker
                        selected={startDate}
                        onChange={(date) => {
                            const weekStart = startOfWeek(date, { weekStartsOn: 0 });
                            const weekEnd = endOfWeek(date, { weekStartsOn: 0 });
                            setDateRange([weekStart, weekEnd]);
                        }}
                        startDate={startDate}
                        endDate={endDate}
                        monthsShown={1}
                        customInput={<CustomDateInput />}
                    />
                    <Button variant="outline" onClick={handleDownloadCsv} disabled={isDownloading || !startDate} className="bg-white text-gray-700 hover:bg-gray-50">
                        <Download size={16} className="mr-2" />
                        {isDownloading ? 'Downloading...' : 'Attendance Report'}
                    </Button>
                </div>
            </div>

            {/* --- COMMENTED OUT ADVANCED DEBUG VIEW --- */}
            {/* {showDebug && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-gray-900 rounded-xl p-4 shadow-inner border border-gray-800 overflow-x-auto max-h-[300px] overflow-y-auto">
                        <h3 className="text-gray-400 text-xs font-bold uppercase mb-2 tracking-wider flex items-center gap-2">
                            <FileJson size={14} /> 1. Raw Report JSON
                        </h3>
                        <pre className="text-green-400 text-xs font-mono whitespace-pre-wrap">
                            {JSON.stringify(rawBackendData, null, 2)}
                        </pre>
                    </div>
                    <div className="bg-gray-900 rounded-xl p-4 shadow-inner border border-gray-800 overflow-x-auto max-h-[300px] overflow-y-auto">
                        <h3 className={`text-xs font-bold uppercase mb-2 tracking-wider flex items-center gap-2 ${fetchedSchedules.length === 0 ? 'text-red-400' : 'text-gray-400'}`}>
                            {fetchedSchedules.length === 0 ? <AlertTriangle size={14}/> : <FileJson size={14}/>}
                            2. Fetched Schedules (Count: {fetchedSchedules.length})
                        </h3>
                        {fetchedSchedules.length === 0 ? (
                            <p className="text-red-400 text-xs font-mono">WARNING: 0 schedules were fetched! The backend schedule endpoints might be returning 404 or empty arrays.</p>
                        ) : (
                            <pre className="text-blue-400 text-xs font-mono whitespace-pre-wrap">
                                {JSON.stringify(fetchedSchedules, null, 2)}
                            </pre>
                        )}
                    </div>
                </div>
            )}
            */}

            {isLoading ? (
                <div className="flex justify-center py-20">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-600"></div>
                </div>
            ) : (
                <>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex justify-between items-start">
                            <div>
                                <p className="text-sm font-medium text-gray-500 mb-1">Scheduled Labor Cost</p>
                                <h3 className="text-2xl font-bold text-gray-800">${summary.scheduledCost.toFixed(2)}</h3>
                                <p className="text-xs text-gray-400 mt-1">for selected period</p>
                            </div>
                            <DollarSign className="text-gray-300" size={24} />
                        </div>
                        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex justify-between items-start">
                            <div>
                                <p className="text-sm font-medium text-gray-500 mb-1">Actual Labor Cost</p>
                                <h3 className="text-2xl font-bold text-gray-800">${summary.actualCost.toFixed(2)}</h3>
                                <p className="text-xs text-gray-400 mt-1">Based on simulated actual hours</p>
                            </div>
                            <DollarSign className="text-gray-300" size={24} />
                        </div>
                        <div className={`bg-white rounded-xl shadow-sm border p-6 flex justify-between items-start ${summary.isOverBudget ? 'border-red-200' : 'border-green-200'}`}>
                            <div>
                                <p className={`text-sm font-medium mb-1 ${summary.isOverBudget ? 'text-red-400' : 'text-green-500'}`}>Cost Variance</p>
                                <h3 className={`text-2xl font-bold ${summary.isOverBudget ? 'text-red-600' : 'text-green-600'}`}>
                                    {summary.isOverBudget ? '+' : '-'}${summary.variance.toFixed(2)}
                                </h3>
                                <p className={`text-xs mt-1 ${summary.isOverBudget ? 'text-red-500' : 'text-green-500'}`}>{summary.variancePercent} from scheduled</p>
                            </div>
                            <Clock className="text-gray-300" size={24} />
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <div className="p-6 border-b border-gray-100">
                            <h2 className="text-xl font-bold text-gray-800">Attendance Report</h2>
                            <p className="text-sm text-gray-500 mt-1">Employee attendance summary for the selected date range.</p>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse min-w-[900px]">
                                <thead>
                                <tr className="border-b border-gray-200 bg-gray-50/50">
                                    <th className="p-4 text-sm font-medium text-gray-500">Employee</th>
                                    <th className="p-4 text-sm font-medium text-gray-500">Shift Date</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Scheduled In</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Clock-In</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">In Variance</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Scheduled Out</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Clock-Out</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Out Variance</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-center">Total Worked</th>
                                    <th className="p-4 text-sm font-medium text-gray-500 text-right">Status</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                {attendanceData.map((row, index) => (
                                    <tr key={row.id || index} className="hover:bg-gray-50 transition-colors">
                                        <td className="p-4 text-sm text-gray-800 font-medium">{row.resolvedEmployeeName}</td>
                                        <td className="p-4 text-sm text-gray-600">{row.resolvedDate ? format(parseISO(row.resolvedDate), 'MMM do, yyyy') : '-'}</td>
                                        <td className="p-4 text-sm text-gray-600 text-center">{formatTime(row.resolvedSchedIn)}</td>
                                        <td className="p-4 text-sm text-gray-600 text-center">{formatTime(row.resolvedClockIn)}</td>
                                        <td className={`p-4 text-sm font-medium text-center ${getVarianceColor(row.inVariance)}`}>{row.inVariance}</td>
                                        <td className="p-4 text-sm text-gray-600 text-center">{formatTime(row.resolvedSchedOut)}</td>
                                        <td className="p-4 text-sm text-gray-600 text-center">{formatTime(row.resolvedClockOut)}</td>
                                        <td className={`p-4 text-sm font-medium text-center ${getVarianceColor(row.outVariance)}`}>{row.outVariance}</td>
                                        <td className="p-4 text-sm text-gray-800 font-semibold text-center">{row.resolvedTotalWorked}</td>
                                        <td className="p-4 text-right">{getStatusPill(row.status)}</td>
                                    </tr>
                                ))}
                                {attendanceData.length === 0 && (
                                    <tr>
                                        <td colSpan="9" className="p-8 text-center text-gray-500">No attendance records found for this week.</td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                        <div className="mb-6">
                            <h2 className="text-xl font-bold text-gray-800">Labor Cost: Budget vs. Actual</h2>
                            <p className="text-sm text-gray-500 mt-1">Comparison of labor costs for the selected period.</p>
                        </div>
                        <div className="h-[400px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={chartData} margin={{ top: 20, right: 0, left: -20, bottom: 0 }} barGap={2}>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dy={10} />
                                    <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} tickFormatter={(value) => `$${value}`} />
                                    <Tooltip cursor={{ fill: '#F3F4F6' }} formatter={(value) => [`$${value.toFixed(2)}`, undefined]} />
                                    <Legend iconType="square" wrapperStyle={{ paddingTop: '20px' }} />
                                    <Bar dataKey="Budget" name="Budget" fill="#2E9E8B" radius={[2, 2, 0, 0]} />
                                    <Bar dataKey="Scheduled" name="Scheduled" fill="#E57358" radius={[2, 2, 0, 0]} />
                                    {/*<Bar dataKey="Actual" name="Actual" fill="#2E9E8B" radius={[2, 2, 0, 0]} />*/}
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default Reports;