import React, { useEffect, useMemo, useState } from "react";
import { useOutletContext } from "react-router-dom";
import API from "../../services/api";
import {
    CalendarDays,
    Clock,
    Briefcase,
    ChevronLeft,
    ChevronRight,
} from "lucide-react";

const DAYS = [
    { key: "MONDAY", label: "Mon" },
    { key: "TUESDAY", label: "Tue" },
    { key: "WEDNESDAY", label: "Wed" },
    { key: "THURSDAY", label: "Thu" },
    { key: "FRIDAY", label: "Fri" },
    { key: "SATURDAY", label: "Sat" },
    { key: "SUNDAY", label: "Sun" },
];

const HOURS = Array.from({ length: 18 }, (_, i) => i + 6);

function safeArray(res) {
    const d = res?.data;
    if (Array.isArray(d)) return d;
    if (Array.isArray(d?.data)) return d.data;
    return [];
}

function parseDateOnly(dateStr) {
    if (!dateStr) return null;

    const [year, month, day] = String(dateStr).split("-").map(Number);
    if (!year || !month || !day) return null;

    return new Date(year, month - 1, day);
}

function timeToMinutes(timeStr) {
    if (!timeStr) return 0;
    const [h = "0", m = "0"] = String(timeStr).split(":");
    return Number(h) * 60 + Number(m);
}

function normalizeShiftTimes(startTime, endTime) {
    const start = timeToMinutes(startTime);
    const end = timeToMinutes(endTime);

    if (end < start) {
        return {
            startTime: endTime,
            endTime: startTime,
        };
    }

    return { startTime, endTime };
}

function isHourInShift(hour, start, end) {
    const startMin = timeToMinutes(start);
    const endMin = timeToMinutes(end);

    const hourStart = hour * 60;
    const hourEnd = hourStart + 60;

    return startMin < hourEnd && endMin > hourStart;
}

function formatHour(hour) {
    const period = hour >= 12 ? "PM" : "AM";
    const display = hour % 12 === 0 ? 12 : hour % 12;
    return `${display}:00 ${period}`;
}

function formatTime(timeStr) {
    if (!timeStr) return "—";

    const [h = "0", m = "0"] = String(timeStr).split(":");
    const hour = Number(h);
    const minute = Number(m);

    const period = hour >= 12 ? "PM" : "AM";
    const displayHour = hour % 12 === 0 ? 12 : hour % 12;

    return `${displayHour}:${String(minute).padStart(2, "0")} ${period}`;
}

function formatTimeRange(start, end) {
    const normalized = normalizeShiftTimes(start, end);
    return `${formatTime(normalized.startTime)} - ${formatTime(normalized.endTime)}`;
}

function formatDate(dateStr) {
    if (!dateStr) return "—";

    const dt = parseDateOnly(dateStr);
    if (!dt || Number.isNaN(dt.getTime())) return dateStr;

    return dt.toLocaleDateString(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric",
    });
}

function getStartOfWeek(date = new Date()) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
}

function getEndOfWeek(startOfWeek) {
    const d = new Date(startOfWeek);
    d.setDate(d.getDate() + 6);
    d.setHours(23, 59, 59, 999);
    return d;
}

function addDays(date, days) {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
}

function formatWeekRange(start, end) {
    if (!start || !end) return "";

    const sameMonth = start.getMonth() === end.getMonth();
    const sameYear = start.getFullYear() === end.getFullYear();

    if (sameMonth && sameYear) {
        return `${start.toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
        })} - ${end.toLocaleDateString(undefined, {
            day: "numeric",
        })}`;
    }

    if (sameYear) {
        return `${start.toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
        })} - ${end.toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
        })}`;
    }

    return `${start.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
    })} - ${end.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
    })}`;
}

function getShiftForCell(schedules, dayKey, hour) {
    const rawShift = schedules.find((s) => {
        const normalized = normalizeShiftTimes(s.startTime, s.endTime);

        return (
            s.dayOfWeek === dayKey &&
            isHourInShift(hour, normalized.startTime, normalized.endTime)
        );
    });

    if (!rawShift) return null;

    const normalized = normalizeShiftTimes(rawShift.startTime, rawShift.endTime);

    return {
        ...rawShift,
        startTime: normalized.startTime,
        endTime: normalized.endTime,
    };
}

function SummaryCard({ icon: Icon, label, value, iconBg, iconText }) {
    return (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <div className="flex items-center gap-3">
                <div className={`p-2 rounded-lg ${iconBg} ${iconText}`}>
                    <Icon size={18} />
                </div>
                <div>
                    <p className="text-sm text-gray-500">{label}</p>
                    <p className="text-xl font-bold text-gray-800">{value}</p>
                </div>
            </div>
        </div>
    );
}

export default function MySchedule() {
    const outletContext = useOutletContext() || {};
    const { employeeId, loadingEmployee = false } = outletContext;

    const [schedules, setSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [selectedWeekStart, setSelectedWeekStart] = useState(getStartOfWeek(new Date()));

    useEffect(() => {
        const load = async () => {
            if (loadingEmployee) return;

            if (!employeeId) {
                setError("Employee ID not found.");
                setSchedules([]);
                setLoading(false);
                return;
            }

            try {
                setLoading(true);
                setError("");

                const res = await API.get(
                    `/scheduling/scheduled_shift/positions/employee/${employeeId}`
                );

                const rows = safeArray(res);
                setSchedules(rows);
            } catch (err) {
                const backendMessage =
                    err?.response?.data?.message ||
                    err?.response?.data?.error ||
                    err?.response?.data?.details ||
                    err?.message ||
                    "Failed to load schedule.";

                setError(`Failed to load schedule. ${backendMessage}`);
                setSchedules([]);
            } finally {
                setLoading(false);
            }
        };

        load();
    }, [employeeId, loadingEmployee]);

    const sortedSchedules = useMemo(() => {
        return [...schedules].sort((a, b) => {
            const aDate = parseDateOnly(a.date)?.getTime() ?? 0;
            const bDate = parseDateOnly(b.date)?.getTime() ?? 0;
            return aDate - bDate;
        });
    }, [schedules]);

    const upcomingShifts = useMemo(() => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return sortedSchedules
            .filter((s) => {
                const dt = parseDateOnly(s.date);
                return dt && !Number.isNaN(dt.getTime()) && dt >= today;
            })
            .slice(0, 5);
    }, [sortedSchedules]);

    const latestPosition = useMemo(() => {
        return upcomingShifts[0]?.positionName || schedules[0]?.positionName || "—";
    }, [upcomingShifts, schedules]);

    const selectedWeekEnd = useMemo(
        () => getEndOfWeek(selectedWeekStart),
        [selectedWeekStart]
    );

    const weekLabel = useMemo(
        () => formatWeekRange(selectedWeekStart, selectedWeekEnd),
        [selectedWeekStart, selectedWeekEnd]
    );

    const weekSchedules = useMemo(() => {
        return schedules.filter((s) => {
            const dt = parseDateOnly(s.date);
            return dt && !Number.isNaN(dt.getTime()) && dt >= selectedWeekStart && dt <= selectedWeekEnd;
        });
    }, [schedules, selectedWeekStart, selectedWeekEnd]);

    const goToPreviousWeek = () => {
        setSelectedWeekStart((prev) => getStartOfWeek(addDays(prev, -7)));
    };

    const goToNextWeek = () => {
        setSelectedWeekStart((prev) => getStartOfWeek(addDays(prev, 7)));
    };

    const goToCurrentWeek = () => {
        setSelectedWeekStart(getStartOfWeek(new Date()));
    };

    if (loadingEmployee || loading) {
        return (
            <div className="flex items-center justify-center min-h-[300px]">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-10 h-10 border-4 border-orange-200 border-t-orange-600 rounded-full animate-spin" />
                    <p className="text-gray-500 font-medium animate-pulse">
                        Loading schedule...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">My Schedule</h1>
                <p className="text-sm text-gray-500">
                    Only published shifts will appear here.
                </p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 text-sm">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <SummaryCard
                    icon={CalendarDays}
                    label="Published Shifts"
                    value={schedules.length}
                    iconBg="bg-blue-50"
                    iconText="text-blue-600"
                />
                <SummaryCard
                    icon={Clock}
                    label="Upcoming Shifts"
                    value={upcomingShifts.length}
                    iconBg="bg-green-50"
                    iconText="text-green-600"
                />
                <SummaryCard
                    icon={Briefcase}
                    label="Current Position"
                    value={latestPosition}
                    iconBg="bg-purple-50"
                    iconText="text-purple-600"
                />
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-[380px_minmax(0,1fr)] gap-6 items-start">
                <div className="bg-white rounded-xl shadow-sm border border-gray-100">
                    <div className="px-6 py-4 border-b border-gray-100">
                        <h2 className="text-lg font-bold text-gray-800">Upcoming Shifts</h2>
                        <p className="text-sm text-gray-500">
                            Your next published shifts.
                        </p>
                    </div>

                    <div className="p-6">
                        {upcomingShifts.length === 0 ? (
                            <div className="p-4 rounded-lg bg-gray-50 border border-gray-100 text-sm text-gray-600">
                                No published shifts yet.
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {upcomingShifts.map((shift) => (
                                    <div
                                        key={shift.shiftId}
                                        className="rounded-xl border border-gray-100 p-4 hover:bg-gray-50 transition"
                                    >
                                        <div className="flex items-start justify-between gap-3">
                                            <div>
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {formatDate(shift.date)}
                                                </p>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    {shift.positionName || "Position"}
                                                </p>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    ${Number(shift.hourlyWage || 0).toFixed(2)}/hr
                                                </p>
                                            </div>

                                            <div className="text-right">
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {formatTimeRange(shift.startTime, shift.endTime)}
                                                </p>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    {shift.dayOfWeek}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-100">
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <h2 className="text-lg font-bold text-gray-800">Weekly View</h2>
                                <p className="text-sm text-gray-500">
                                    View your schedule one week at a time.
                                </p>
                            </div>

                            <div className="flex items-center gap-2 flex-wrap">
                                <button
                                    onClick={goToPreviousWeek}
                                    className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                                >
                                    <ChevronLeft size={16} />
                                    Prev
                                </button>

                                <button
                                    onClick={goToCurrentWeek}
                                    className="rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm font-medium text-orange-700 hover:bg-orange-100"
                                >
                                    This Week
                                </button>

                                <button
                                    onClick={goToNextWeek}
                                    className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                                >
                                    Next
                                    <ChevronRight size={16} />
                                </button>
                            </div>
                        </div>

                        <div className="mt-3 text-sm font-medium text-gray-600">
                            {weekLabel}
                        </div>
                    </div>

                    <div className="overflow-x-auto">
                        <div className="min-w-[860px]">
                            <div className="grid grid-cols-8 bg-gray-50 border-b border-gray-100">
                                <div className="p-3 text-xs font-semibold text-gray-500">Time</div>
                                {DAYS.map((d) => (
                                    <div
                                        key={d.key}
                                        className="p-3 text-xs font-semibold text-center text-gray-700"
                                    >
                                        {d.label}
                                    </div>
                                ))}
                            </div>

                            {HOURS.map((hour) => (
                                <div
                                    key={hour}
                                    className="grid grid-cols-8 border-b border-gray-100 last:border-b-0"
                                >
                                    <div className="p-3 text-xs text-gray-500 bg-gray-50">
                                        {formatHour(hour)}
                                    </div>

                                    {DAYS.map((day) => {
                                        const shift = getShiftForCell(weekSchedules, day.key, hour);
                                        const isShiftStart =
                                            shift &&
                                            hour === Math.floor(timeToMinutes(shift.startTime) / 60);

                                        return (
                                            <div
                                                key={`${day.key}-${hour}`}
                                                className="border-l border-gray-100 min-h-[56px] p-1"
                                            >
                                                {shift && (() => {
                                                    const startHour = Math.floor(timeToMinutes(shift.startTime) / 60);
                                                    const endHour = Math.ceil(timeToMinutes(shift.endTime) / 60) - 1;

                                                    const isStart = hour === startHour;
                                                    const isEnd = hour === endHour;

                                                    return (
                                                        <div
                                                            className={`h-full px-2 py-1 text-xs flex items-center justify-center text-center
                                                                bg-green-100 text-green-800
                                                                ${isStart ? "rounded-t-lg" : ""}
                                                                ${isEnd ? "rounded-b-lg" : ""}
                                                                ${!isStart && !isEnd ? "rounded-none" : ""}
                                                            `}
                                                        >
                                                            {isStart ? (
                                                                <div className="leading-tight">
                                                                    <div className="font-semibold">
                                                                        {shift.positionName || "Shift"}
                                                                    </div>
                                                                    <div className="text-[11px] text-green-700 mt-0.5">
                                                                        {formatTime(shift.startTime)} - {formatTime(shift.endTime)}
                                                                    </div>
                                                                </div>
                                                            ) : null}
                                                        </div>
                                                    );
                                                })()}
                                            </div>
                                        );
                                    })}
                                </div>
                            ))}
                        </div>
                    </div>

                    {weekSchedules.length === 0 && (
                        <div className="border-t border-gray-100 px-6 py-4 text-sm text-gray-500">
                            No shifts scheduled for this week.
                        </div>
                    )}
                </div>
            </div>

            {schedules.length === 0 && !error && (
                <p className="text-sm text-gray-400 italic">
                    No published shifts available yet.
                </p>
            )}
        </div>
    );
}