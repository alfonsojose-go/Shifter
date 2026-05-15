import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../../services/api";
import {
    Calendar,
    UserCheck,
    FileText,
    Clock,
    AlertCircle,
    Repeat,
    ClipboardList,
} from "lucide-react";

function MetricCard({ label, value, sub, Icon, iconBg, iconText, onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 text-left hover:shadow-md transition-shadow w-full"
        >
            <div className="flex justify-between items-start">
                <div>
                    <p className="text-sm font-medium text-gray-500">{label}</p>
                    <h3 className="text-3xl font-bold text-gray-800 mt-2">{value}</h3>
                </div>
                <div className={`p-2 rounded-lg ${iconBg} ${iconText}`}>
                    <Icon size={20} />
                </div>
            </div>
            <p className="text-xs text-gray-400 mt-4">{sub}</p>
        </button>
    );
}

function safeArray(res) {
    const d = res?.data;
    if (Array.isArray(d)) return d;
    if (Array.isArray(d?.data)) return d.data;
    return [];
}

function formatDayLabel(dateLike) {
    try {
        const dt = new Date(dateLike);
        if (Number.isNaN(dt.getTime())) return "Shift";
        return dt.toLocaleDateString(undefined, {
            weekday: "short",
            month: "short",
            day: "numeric",
        });
    } catch {
        return "Shift";
    }
}

function formatDateFull(dateLike) {
    try {
        const dt = new Date(dateLike);
        if (Number.isNaN(dt.getTime())) return dateLike || "—";
        return dt.toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
            year: "numeric",
        });
    } catch {
        return dateLike || "—";
    }
}

function formatTimeRange(start, end) {
    if (!start && !end) return "";
    if (start && end) return `${start} - ${end}`;
    return start || end;
}

function formatRequestLabel(type) {
    if (!type) return "Request";
    return type
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase());
}

function getRequestStatusClass(status) {
    switch ((status || "").toUpperCase()) {
        case "APPROVED":
            return "bg-green-100 text-green-700";
        case "REJECTED":
            return "bg-red-100 text-red-700";
        default:
            return "bg-orange-100 text-orange-700";
    }
}

export default function Dashboard() {
    const navigate = useNavigate();

    const username = localStorage.getItem("username") || "Employee";
    const employeeId = Number(localStorage.getItem("employeeId"));
    const canUseBackend = Number.isFinite(employeeId) && employeeId > 0;

    const [isLoading, setIsLoading] = useState(true);

    const [stats, setStats] = useState({
        upcomingShifts: 0,
        availabilityBlocks: 0,
        pendingRequests: 0,
    });

    const [nextShifts, setNextShifts] = useState([]);
    const [recentRequests, setRecentRequests] = useState([]);

    const [currentTime, setCurrentTime] = useState(new Date());
    const [isClockedIn, setIsClockedIn] = useState(false);
    const [clockStatusMessage, setClockStatusMessage] = useState("Checking status...");
    const [clockLoading, setClockLoading] = useState(false);

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentTime(new Date());
        }, 1000);

        return () => clearInterval(timer);
    }, []);

    const loadClockStatus = async () => {
        try {
            const res = await API.get("/clock/status");

            const raw =
                typeof res?.data === "string"
                    ? res.data
                    : res?.data?.message ||
                      res?.data?.data?.message ||
                      res?.data?.status ||
                      res?.data?.data ||
                      "";

            const message = String(raw).toLowerCase();

            if (message.includes("clocked-in") || message.includes("clocked in")) {
                setIsClockedIn(true);
                setClockStatusMessage("You are clocked in");
            } else {
                setIsClockedIn(false);
                setClockStatusMessage("You are clocked out");
            }
        } catch (error) {
            console.error("Failed to load clock status:", error);
            console.error("Status:", error?.response?.status);
            console.error("Data:", error?.response?.data);
            setIsClockedIn(false);
            setClockStatusMessage("Unable to load clock status");
        }
    };

    useEffect(() => {
        loadClockStatus();
    }, []);

    useEffect(() => {
        let alive = true;

        const load = async () => {
            setIsLoading(true);

            try {
                if (!canUseBackend) {
                    if (!alive) return;
                    setStats({ upcomingShifts: 0, availabilityBlocks: 0, pendingRequests: 0 });
                    setNextShifts([]);
                    setRecentRequests([]);
                    return;
                }

                const [availabilityRes, shiftsRes, requestsRes] = await Promise.all([
                    API.get(`/employee/availabilities/employee/${employeeId}`),
                    API.get(`/scheduling/employee/${employeeId}`),
                    API.get(`/requests/user/${employeeId}`),
                ]);

                const availabilityRows = safeArray(availabilityRes);
                const shiftRows = safeArray(shiftsRes);
                const requestRows = safeArray(requestsRes);

                const nextShiftCards = shiftRows
                    .slice(0, 2)
                    .map((s) => ({
                        day:
                            s.day ||
                            s.shiftDate ||
                            s.date ||
                            s.startDate ||
                            s.startTime ||
                            "Shift",
                        time:
                            s.time ||
                            formatTimeRange(s.startTime || s.start, s.endTime || s.end) ||
                            "—",
                        role: s.role || s.position || s.title || "Shift",
                    }))
                    .map((x) => ({
                        ...x,
                        day:
                            typeof x.day === "string" && x.day.includes("-")
                                ? formatDayLabel(x.day)
                                : x.day,
                    }));

                const pendingCount = requestRows.filter(
                    (r) => (r.status || "").toUpperCase() === "PENDING"
                ).length;

                const recentRequestCards = [...requestRows]
                    .sort((a, b) => (b.id || 0) - (a.id || 0))
                    .slice(0, 4)
                    .map((r) => ({
                        id: r.id,
                        type: formatRequestLabel(r.type),
                        date: r.startDate
                            ? `${formatDateFull(r.startDate)}${
                                  r.endDate ? ` → ${formatDateFull(r.endDate)}` : ""
                              }`
                            : formatDateFull(r.date),
                        status: (r.status || "PENDING").toUpperCase(),
                    }));

                if (!alive) return;

                setStats({
                    upcomingShifts: shiftRows.length,
                    availabilityBlocks: availabilityRows.length,
                    pendingRequests: pendingCount,
                });

                setNextShifts(nextShiftCards);
                setRecentRequests(recentRequestCards);
            } catch (err) {
                console.error("Dashboard load failed:", err);

                if (!alive) return;
                setStats({ upcomingShifts: 0, availabilityBlocks: 0, pendingRequests: 0 });
                setNextShifts([]);
                setRecentRequests([]);
            } finally {
                if (alive) setIsLoading(false);
            }
        };

        load();

        return () => {
            alive = false;
        };
    }, [employeeId, canUseBackend]);

    const handleClockIn = async () => {
        if (isClockedIn) return;

        try {
            setClockLoading(true);
            await API.post("/clock/in");
            await loadClockStatus();
        } catch (error) {
            console.error("Clock in failed:", error);
            console.error("Status:", error?.response?.status);
            console.error("Data:", error?.response?.data);
            alert(error?.response?.data?.message || error?.response?.data || "Clock in failed.");
        } finally {
            setClockLoading(false);
        }
    };

    const handleClockOut = async () => {
        if (!isClockedIn) return;

        try {
            setClockLoading(true);
            await API.post("/clock/out");
            await loadClockStatus();
        } catch (error) {
            console.error("Clock out failed:", error);
            alert("Clock out failed.");
        } finally {
            setClockLoading(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex h-full items-center justify-center min-h-[300px]">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-10 h-10 border-4 border-orange-200 border-t-orange-600 rounded-full animate-spin" />
                    <p className="text-gray-500 font-medium animate-pulse">Loading Dashboard...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Welcome, {username}!</h1>
                <p className="text-gray-500">Here's your summary for today.</p>

                {!canUseBackend && (
                    <p className="mt-2 text-sm text-orange-600">
                        Missing <b>employeeId</b> in localStorage — dashboard will show 0s until login stores it.
                    </p>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <MetricCard
                    label="Upcoming Shifts"
                    value={stats.upcomingShifts}
                    sub="view your schedule"
                    Icon={Calendar}
                    iconBg="bg-blue-50"
                    iconText="text-blue-600"
                    onClick={() => navigate("/emp/schedule")}
                />

                <MetricCard
                    label="Availability"
                    value={stats.availabilityBlocks}
                    sub="time blocks set"
                    Icon={UserCheck}
                    iconBg="bg-green-50"
                    iconText="text-green-600"
                    onClick={() => navigate("/emp/availability")}
                />

                <MetricCard
                    label="Requests"
                    value={stats.pendingRequests}
                    sub="pending approvals"
                    Icon={AlertCircle}
                    iconBg="bg-orange-50"
                    iconText="text-orange-600"
                    onClick={() => navigate("/emp/requests")}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-gray-800">Next Shifts</h3>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/schedule")}
                            className="text-sm font-semibold text-orange-600 hover:text-orange-700"
                        >
                            View all
                        </button>
                    </div>

                    {nextShifts.length === 0 ? (
                        <div className="p-3 rounded-lg bg-gray-50 border border-gray-100 text-sm text-gray-600">
                            No upcoming shifts found.
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {nextShifts.map((s, i) => (
                                <div
                                    key={i}
                                    className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition"
                                >
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-orange-50 text-orange-600 rounded-lg">
                                            <Clock size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-900">{s.day}</p>
                                            <p className="text-xs text-gray-500">{s.role}</p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-sm font-medium text-gray-900">{s.time}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-gray-800">Recent Requests</h3>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/requests")}
                            className="text-sm font-semibold text-orange-600 hover:text-orange-700"
                        >
                            New request
                        </button>
                    </div>

                    {recentRequests.length === 0 ? (
                        <div className="p-3 rounded-lg bg-gray-50 border border-gray-100 text-sm text-gray-600">
                            No recent requests found.
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {recentRequests.map((r) => (
                                <div
                                    key={r.id}
                                    className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition"
                                >
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                                            <FileText size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-900">{r.type}</p>
                                            <p className="text-xs text-gray-500">{r.date}</p>
                                        </div>
                                    </div>

                                    <span
                                        className={`px-3 py-1 rounded-full text-xs font-semibold ${getRequestStatusClass(
                                            r.status
                                        )}`}
                                    >
                                        {r.status}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <h3 className="text-lg font-bold text-gray-800 mb-4">Quick Actions</h3>

                    <div className="flex flex-wrap gap-3">
                        <button
                            type="button"
                            onClick={() => navigate("/emp/schedule")}
                            className="px-4 py-2 rounded-lg bg-white border border-gray-200 shadow-sm hover:shadow-md transition text-sm font-semibold text-gray-700"
                        >
                            Open Schedule
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/availability")}
                            className="px-4 py-2 rounded-lg bg-white border border-gray-200 shadow-sm hover:shadow-md transition text-sm font-semibold text-gray-700"
                        >
                            Edit Availability
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/requests")}
                            className="px-4 py-2 rounded-lg bg-white border border-gray-200 shadow-sm hover:shadow-md transition text-sm font-semibold text-gray-700 flex items-center gap-2"
                        >
                            <FileText size={16} />
                            Request Time Off
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/shiftswap")}
                            className="px-4 py-2 rounded-lg bg-white border border-gray-200 shadow-sm hover:shadow-md transition text-sm font-semibold text-gray-700 flex items-center gap-2"
                        >
                            <Repeat size={16} />
                            Shift Swap
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/emp/myswaprequests")}
                            className="px-4 py-2 rounded-lg bg-white border border-gray-200 shadow-sm hover:shadow-md transition text-sm font-semibold text-gray-700 flex items-center gap-2"
                        >
                            <ClipboardList size={16} />
                            My Swap Requests
                        </button>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                    <div
                        className={`px-5 py-4 flex items-center justify-between ${
                            isClockedIn ? "bg-green-50" : "bg-red-50"
                        }`}
                    >
                        <div className="flex items-center gap-2">
                            <span
                                className={`w-3 h-3 rounded-full animate-pulse ${
                                    isClockedIn ? "bg-green-500" : "bg-red-500"
                                }`}
                            />
                            <span
                                className={`text-sm font-semibold ${
                                    isClockedIn ? "text-green-700" : "text-red-700"
                                }`}
                            >
                                {isClockedIn ? "CLOCKED IN" : "CLOCKED OUT"}
                            </span>
                        </div>

                        <Clock
                            size={18}
                            className={isClockedIn ? "text-green-700" : "text-red-700"}
                        />
                    </div>

                    <div className="p-6">
                        <p className="text-xs uppercase tracking-wide text-gray-500 mb-1">
                            Time Clock
                        </p>

                        <h2 className="text-3xl font-bold text-gray-800 leading-none">
                            {currentTime.toLocaleTimeString()}
                        </h2>

                        <p className="text-sm text-gray-500 mt-2">
                            {currentTime.toLocaleDateString(undefined, {
                                weekday: "long",
                                year: "numeric",
                                month: "long",
                                day: "numeric",
                            })}
                        </p>

                        <div className="grid grid-cols-2 gap-3 mt-5">
                            <button
                                type="button"
                                onClick={handleClockIn}
                                disabled={isClockedIn || clockLoading}
                                className={`py-2.5 rounded-xl text-sm font-semibold transition ${
                                    isClockedIn || clockLoading
                                        ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                        : "bg-green-600 text-white hover:bg-green-700"
                                }`}
                            >
                                {clockLoading && !isClockedIn ? "Working..." : "Clock In"}
                            </button>

                            <button
                                type="button"
                                onClick={handleClockOut}
                                disabled={!isClockedIn || clockLoading}
                                className={`py-2.5 rounded-xl text-sm font-semibold transition ${
                                    !isClockedIn || clockLoading
                                        ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                        : "bg-red-600 text-white hover:bg-red-700"
                                }`}
                            >
                                {clockLoading && isClockedIn ? "Working..." : "Clock Out"}
                            </button>
                        </div>

                        <div className="mt-4 p-3 rounded-xl bg-gray-50 border border-gray-100">
                            <p className="text-xs text-gray-500">Latest Activity</p>
                            <p className="text-sm font-medium text-gray-800 mt-1">
                                {clockStatusMessage}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}