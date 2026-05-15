import React, { useEffect, useMemo, useState } from "react";
import { useOutletContext } from "react-router-dom";
import API from "../../services/api";
import {
    RefreshCw,
    Repeat,
    Clock,
    CalendarDays,
    Search,
    CheckCircle2,
    XCircle,
    Hourglass,
} from "lucide-react";

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

function normalizeTimeString(timeStr) {
    if (!timeStr) return null;

    const parts = String(timeStr).split(":");
    const h = parts[0]?.padStart(2, "0") || "00";
    const m = parts[1]?.padStart(2, "0") || "00";
    const s = parts[2]?.padStart(2, "0") || "00";

    return `${h}:${m}:${s}`;
}

function formatDayLabel(dateLike) {
    if (!dateLike) return "Unknown Date";

    const dt =
        typeof dateLike === "string" && /^\d{4}-\d{2}-\d{2}$/.test(dateLike)
            ? parseDateOnly(dateLike)
            : new Date(dateLike);

    if (!dt || Number.isNaN(dt.getTime())) return "Unknown Date";

    return dt.toLocaleDateString(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric",
        year: "numeric",
    });
}

function formatClockTime(timeStr) {
    if (!timeStr) return "—";

    const [h = "0", m = "0"] = normalizeTimeString(timeStr).split(":");
    const hour = Number(h);
    const minute = Number(m);

    const period = hour >= 12 ? "PM" : "AM";
    const displayHour = hour % 12 === 0 ? 12 : hour % 12;

    return `${displayHour}:${minute} ${period}`;
}

function formatTimeRange(start, end) {
    if (!start && !end) return "—";
    if (start && end) return `${formatClockTime(start)} - ${formatClockTime(end)}`;
    return formatClockTime(start || end);
}

function getStatusClasses(status) {
    switch ((status || "").toUpperCase()) {
        case "APPROVED":
            return "bg-green-100 text-green-700 border-green-200";
        case "REJECTED":
            return "bg-red-100 text-red-700 border-red-200";
        default:
            return "bg-orange-100 text-orange-700 border-orange-200";
    }
}

function getStatusIcon(status) {
    switch ((status || "").toUpperCase()) {
        case "APPROVED":
            return <CheckCircle2 size={16} />;
        case "REJECTED":
            return <XCircle size={16} />;
        default:
            return <Hourglass size={16} />;
    }
}

function getSwapTargetLabel(req) {
    return (
        req.swapWithEmployeeName ||
        req.swapWithName ||
        req.targetEmployeeName ||
        (req.swapWithUserId ? `Employee #${req.swapWithUserId}` : "Unknown employee")
    );
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

export default function MySwapRequests() {
    const outletContext = useOutletContext() || {};
    const { employeeId, loadingEmployee = false } = outletContext;

    const [isLoading, setIsLoading] = useState(true);
    const [requests, setRequests] = useState([]);
    const [errorMessage, setErrorMessage] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [searchTerm, setSearchTerm] = useState("");

    const loadRequests = async () => {
        if (!employeeId || Number.isNaN(Number(employeeId))) {
            setIsLoading(false);
            setErrorMessage("Missing employee ID.");
            return;
        }

        setIsLoading(true);
        setErrorMessage("");

        try {
            const res = await API.get(`/requests/user/${employeeId}`);
            const rows = safeArray(res);
            setRequests(rows);
        } catch (error) {
            console.error("Failed to load swap requests:", error);
            console.error("Status:", error?.response?.status);
            console.error("Data:", error?.response?.data);

            setRequests([]);
            setErrorMessage("Could not load your swap requests.");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (loadingEmployee) return;

        if (!employeeId || Number.isNaN(Number(employeeId))) {
            setIsLoading(false);
            setErrorMessage("Missing employee ID.");
            return;
        }

        loadRequests();
    }, [employeeId, loadingEmployee]);

    const swapRequests = useMemo(() => {
        return requests
            .filter((req) => (req.type || "").toUpperCase() === "SHIFT_SWAP")
            .sort((a, b) => {
                const aDate =
                    parseDateOnly(a.date || a.startDate)?.getTime() ??
                    new Date(a.createdAt || 0).getTime();
                const bDate =
                    parseDateOnly(b.date || b.startDate)?.getTime() ??
                    new Date(b.createdAt || 0).getTime();

                return bDate - aDate;
            });
    }, [requests]);

    const counts = useMemo(() => {
        return {
            all: swapRequests.length,
            pending: swapRequests.filter((r) => (r.status || "PENDING").toUpperCase() === "PENDING").length,
            approved: swapRequests.filter((r) => (r.status || "").toUpperCase() === "APPROVED").length,
            rejected: swapRequests.filter((r) => (r.status || "").toUpperCase() === "REJECTED").length,
        };
    }, [swapRequests]);

    const filteredRequests = useMemo(() => {
        let rows = [...swapRequests];

        if (statusFilter !== "ALL") {
            rows = rows.filter(
                (req) => (req.status || "PENDING").toUpperCase() === statusFilter
            );
        }

        const term = searchTerm.trim().toLowerCase();
        if (!term) return rows;

        return rows.filter((req) => {
            const idText = String(req.id || "");
            const reasonText = String(req.reason || "").toLowerCase();
            const statusText = String(req.status || "PENDING").toLowerCase();
            const dateText = String(req.date || req.startDate || "").toLowerCase();
            const timeText = `${req.startTime || ""} ${req.endTime || ""}`.toLowerCase();
            const targetText = getSwapTargetLabel(req).toLowerCase();

            return (
                idText.includes(term) ||
                reasonText.includes(term) ||
                statusText.includes(term) ||
                dateText.includes(term) ||
                timeText.includes(term) ||
                targetText.includes(term)
            );
        });
    }, [swapRequests, statusFilter, searchTerm]);

    if (loadingEmployee || isLoading) {
        return (
            <div className="flex h-full items-center justify-center min-h-[300px]">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-10 h-10 border-4 border-orange-200 border-t-orange-600 rounded-full animate-spin" />
                    <p className="text-gray-500 font-medium animate-pulse">
                        Loading your swap requests...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">My Swap Requests</h1>
                <p className="text-gray-500">
                    Track the status of your submitted shift swap requests.
                </p>
            </div>

            {errorMessage && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 text-sm">
                    {errorMessage}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <SummaryCard
                    icon={Repeat}
                    label="All Requests"
                    value={counts.all}
                    iconBg="bg-blue-50"
                    iconText="text-blue-600"
                />
                <SummaryCard
                    icon={Hourglass}
                    label="Pending"
                    value={counts.pending}
                    iconBg="bg-orange-50"
                    iconText="text-orange-600"
                />
                <SummaryCard
                    icon={CheckCircle2}
                    label="Approved"
                    value={counts.approved}
                    iconBg="bg-green-50"
                    iconText="text-green-600"
                />
                <SummaryCard
                    icon={XCircle}
                    label="Rejected"
                    value={counts.rejected}
                    iconBg="bg-red-50"
                    iconText="text-red-600"
                />
            </div>

            <div className="bg-white p-5 rounded-xl shadow-sm border border-gray-100">
                <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                    <div className="flex flex-wrap gap-2">
                        {[
                            { key: "ALL", label: `All (${counts.all})` },
                            { key: "PENDING", label: `Pending (${counts.pending})` },
                            { key: "APPROVED", label: `Approved (${counts.approved})` },
                            { key: "REJECTED", label: `Rejected (${counts.rejected})` },
                        ].map((item) => (
                            <button
                                key={item.key}
                                type="button"
                                onClick={() => setStatusFilter(item.key)}
                                className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
                                    statusFilter === item.key
                                        ? "bg-orange-600 text-white"
                                        : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                                }`}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>

                    <div className="flex flex-col sm:flex-row gap-3">
                        <div className="relative w-full sm:w-72">
                            <Search
                                size={16}
                                className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
                            />
                            <input
                                type="text"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                placeholder="Search by date, status, employee..."
                                className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
                            />
                        </div>

                        <button
                            type="button"
                            onClick={loadRequests}
                            className="px-4 py-2 rounded-lg text-sm font-semibold bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 flex items-center gap-2"
                        >
                            <RefreshCw size={16} />
                            Refresh
                        </button>
                    </div>
                </div>
            </div>

            {filteredRequests.length === 0 ? (
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 text-sm text-gray-600">
                    No swap requests match your current filter.
                </div>
            ) : (
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                    {filteredRequests.map((req) => {
                        const requestId = req.id;
                        const dateLabel = req.date
                            ? formatDayLabel(req.date)
                            : req.startDate
                                ? `${formatDayLabel(req.startDate)} - ${formatDayLabel(req.endDate)}`
                                : "Unknown Date";

                        const timeLabel = formatTimeRange(req.startTime, req.endTime);
                        const status = (req.status || "PENDING").toUpperCase();

                        return (
                            <div
                                key={requestId}
                                className="bg-white p-6 rounded-xl shadow-sm border border-gray-100"
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <h3 className="text-lg font-bold text-gray-800">
                                            Swap Request #{requestId}
                                        </h3>
                                        <p className="text-sm text-gray-500 mt-1">
                                            Target: {getSwapTargetLabel(req)}
                                        </p>
                                    </div>

                                    <span
                                        className={`px-3 py-1 rounded-full text-xs font-semibold border inline-flex items-center gap-1 ${getStatusClasses(
                                            status
                                        )}`}
                                    >
                                        {getStatusIcon(status)}
                                        {status}
                                    </span>
                                </div>

                                <div className="mt-5 space-y-3">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 rounded-lg bg-orange-50 text-orange-600">
                                            <CalendarDays size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-900">{dateLabel}</p>
                                            <p className="text-xs text-gray-500">Requested date</p>
                                        </div>
                                    </div>

                                    <div className="flex items-center gap-3">
                                        <div className="p-2 rounded-lg bg-orange-50 text-orange-600">
                                            <Clock size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-900">{timeLabel}</p>
                                            <p className="text-xs text-gray-500">Requested shift time</p>
                                        </div>
                                    </div>

                                    {req.reason && (
                                        <div className="rounded-lg bg-gray-50 border border-gray-100 p-3">
                                            <p className="text-xs uppercase tracking-wide text-gray-500 mb-1">
                                                Reason
                                            </p>
                                            <p className="text-sm text-gray-700">{req.reason}</p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}