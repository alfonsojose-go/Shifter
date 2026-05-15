import React, { useEffect, useMemo, useState } from "react";
import { useOutletContext } from "react-router-dom";
import API from "../../services/api";
import { Users, Clock, Search, RefreshCw } from "lucide-react";

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

function normalizeTimeString(timeStr) {
    if (!timeStr) return null;

    const parts = String(timeStr).split(":");
    const h = parts[0]?.padStart(2, "0") || "00";
    const m = parts[1]?.padStart(2, "0") || "00";
    const s = parts[2]?.padStart(2, "0") || "00";

    return `${h}:${m}:${s}`;
}

function formatClockTime(timeStr) {
    if (!timeStr) return "—";

    const [h = "0", m = "0"] = String(timeStr).split(":");
    const hour = Number(h);
    const minute = Number(m);

    const period = hour >= 12 ? "PM" : "AM";
    const displayHour = hour % 12 === 0 ? 12 : hour % 12;

    return `${displayHour}:${String(minute).padStart(2, "0")} ${period}`;
}

function formatDayLabel(dateLike) {
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

function formatTimeRange(start, end) {
    if (!start && !end) return "—";

    const normalized = normalizeShiftTimes(start, end);
    return `${formatClockTime(normalized.startTime)} - ${formatClockTime(normalized.endTime)}`;
}

function getShiftDate(shift) {
    return shift.shiftDate || shift.date || shift.startDate || shift.day || null;
}

function getShiftStartTime(shift) {
    return shift.startTime || shift.start || null;
}

function getShiftEndTime(shift) {
    return shift.endTime || shift.end || null;
}

function getShiftEmployeeId(shift) {
    return (
        shift.employeeId ||
        shift.userId ||
        shift.employee?.id ||
        shift.employee?.employeeId ||
        null
    );
}

function getShiftEmployeeName(shift) {
    return (
        shift.fullName ||
        shift.employeeFullName ||
        shift.employeeName ||
        shift.name ||
        shift.userName ||
        shift.employeeUsername ||
        shift.employee?.fullName ||
        shift.employee?.name ||
        `${shift.employee?.firstName || ""} ${shift.employee?.lastName || ""}`.trim() ||
        "Employee"
    );
}

function getShiftRole(shift) {
    return (
        shift.positionName ||
        shift.role ||
        shift.position ||
        shift.title ||
        "Shift"
    );
}

function toDateKey(dateLike) {
    if (!dateLike) return null;

    if (typeof dateLike === "string" && /^\d{4}-\d{2}-\d{2}$/.test(dateLike)) {
        return dateLike;
    }

    const dt = new Date(dateLike);
    if (Number.isNaN(dt.getTime())) return null;

    const year = dt.getFullYear();
    const month = String(dt.getMonth() + 1).padStart(2, "0");
    const day = String(dt.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function getShiftId(shift, fallback = null) {
    return shift.schedulingId || shift.shiftId || shift.scheduleId || shift.id || fallback;
}

function buildShiftRequestKey(shift) {
    const shiftEmployeeId = Number(getShiftEmployeeId(shift));
    const date = toDateKey(getShiftDate(shift));

    const normalized = normalizeShiftTimes(
        getShiftStartTime(shift),
        getShiftEndTime(shift)
    );

    return JSON.stringify({
        swapWithUserId: Number.isNaN(shiftEmployeeId) ? null : shiftEmployeeId,
        date: date || null,
        startTime: normalizeTimeString(normalized.startTime),
        endTime: normalizeTimeString(normalized.endTime),
    });
}

function buildRequestMatchKey(request) {
    const rawSwapWithUserId =
        request.swapWithUserId ??
        request.swapUserId ??
        request.targetUserId ??
        request.requestedUserId ??
        null;

    const rawDate =
        request.date ??
        request.shiftDate ??
        request.requestDate ??
        request.startDate ??
        null;

    const rawStart =
        request.startTime ??
        request.shiftStartTime ??
        request.start ??
        null;

    const rawEnd =
        request.endTime ??
        request.shiftEndTime ??
        request.end ??
        null;

    const normalized = normalizeShiftTimes(rawStart, rawEnd);

    return JSON.stringify({
        swapWithUserId: rawSwapWithUserId != null ? Number(rawSwapWithUserId) : null,
        date: toDateKey(rawDate) || null,
        startTime: normalizeTimeString(normalized.startTime),
        endTime: normalizeTimeString(normalized.endTime),
    });
}

export default function ShiftSwap() {
    const outletContext = useOutletContext() || {};
    const {
        employeeId,
        loadingEmployee = false,
        role: contextRole,
    } = outletContext;

    const myRole = contextRole || localStorage.getItem("role") || "";

    const [isLoading, setIsLoading] = useState(true);
    const [allShifts, setAllShifts] = useState([]);
    const [myShifts, setMyShifts] = useState([]);
    const [myRequests, setMyRequests] = useState([]);
    const [searchTerm, setSearchTerm] = useState("");
    const [requestingId, setRequestingId] = useState(null);
    const [requestedShiftKeys, setRequestedShiftKeys] = useState(new Set());
    const [errorMessage, setErrorMessage] = useState("");

    const loadData = async () => {
        if (!employeeId || Number.isNaN(Number(employeeId))) {
            setIsLoading(false);
            setErrorMessage("Missing employee ID.");
            return;
        }

        setIsLoading(true);
        setErrorMessage("");

        try {
            console.log("🚀 Calling swap APIs with employeeId:", employeeId);

            const [allRes, myRes, myReqRes] = await Promise.all([
                API.get("/scheduling/scheduled_shift/positions"),
                API.get(`/scheduling/scheduled_shift/positions/employee/${employeeId}`),
                API.get(`/requests/user/${employeeId}`),
            ]);

            const allRows = safeArray(allRes);
            const myRows = safeArray(myRes);
            const requestRows = safeArray(myReqRes);

            console.log("✅ All schedules full:", JSON.stringify(allRes.data, null, 2));
            console.log("✅ My schedules full:", JSON.stringify(myRes.data, null, 2));
            console.log("✅ My requests full:", JSON.stringify(myReqRes.data, null, 2));
            console.log("✅ All rows parsed:", JSON.stringify(allRows, null, 2));
            console.log("✅ My rows parsed:", JSON.stringify(myRows, null, 2));
            console.log("✅ Request rows parsed:", JSON.stringify(requestRows, null, 2));

            const existingSwapRequests = requestRows.filter((req) => {
                const type = (req.type || "").toUpperCase();
                const status = (req.status || "").toUpperCase();

                return (
                    type === "SHIFT_SWAP" &&
                    (status === "PENDING" || status === "APPROVED")
                );
            });

            const existingRequestedKeys = new Set(
                existingSwapRequests.map((req) => buildRequestMatchKey(req))
            );

            console.log("✅ Existing swap requests:", JSON.stringify(existingSwapRequests, null, 2));
            console.log("✅ Existing requested keys:", JSON.stringify([...existingRequestedKeys], null, 2));

            setAllShifts(allRows);
            setMyShifts(myRows);
            setMyRequests(existingSwapRequests);
            setRequestedShiftKeys(existingRequestedKeys);
        } catch (error) {
            console.error("❌ Failed to load shift swap data:", error);

            const backendMessage =
                error?.response?.data?.message ||
                error?.response?.data?.error ||
                error?.response?.data?.details ||
                `Status: ${error?.response?.status || "No response"}`;

            setErrorMessage(`Could not load schedules. ${backendMessage}`);
            setAllShifts([]);
            setMyShifts([]);
            setMyRequests([]);
            setRequestedShiftKeys(new Set());
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

        loadData();
    }, [employeeId, loadingEmployee]);

    const myShiftDates = useMemo(() => {
        const set = new Set();

        myShifts.forEach((shift) => {
            const key = toDateKey(getShiftDate(shift));
            if (key) set.add(key);
        });

        return set;
    }, [myShifts]);

    const availableSwapShifts = useMemo(() => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return allShifts.filter((shift) => {
            const shiftEmployeeId = getShiftEmployeeId(shift);
            const rawDate = getShiftDate(shift);

            if (!rawDate) return false;

            const shiftDate =
                typeof rawDate === "string" && /^\d{4}-\d{2}-\d{2}$/.test(rawDate)
                    ? parseDateOnly(rawDate)
                    : new Date(rawDate);

            if (!shiftDate || Number.isNaN(shiftDate.getTime())) return false;

            const shiftDateKey = toDateKey(rawDate);
            if (!shiftDateKey) return false;

            if (Number(shiftEmployeeId) === Number(employeeId)) return false;

            shiftDate.setHours(0, 0, 0, 0);
            if (shiftDate < today) return false;

            if (!myShiftDates.has(shiftDateKey)) return false;

            return true;
        });
    }, [allShifts, employeeId, myShiftDates]);

    const filteredShifts = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();

        if (!term) return availableSwapShifts;

        return availableSwapShifts.filter((shift) => {
            const employeeName = getShiftEmployeeName(shift).toLowerCase();
            const role = getShiftRole(shift).toLowerCase();
            const dateText = formatDayLabel(getShiftDate(shift)).toLowerCase();
            const timeText = formatTimeRange(
                getShiftStartTime(shift),
                getShiftEndTime(shift)
            ).toLowerCase();

            return (
                employeeName.includes(term) ||
                role.includes(term) ||
                dateText.includes(term) ||
                timeText.includes(term)
            );
        });
    }, [availableSwapShifts, searchTerm]);

    const handleRequestSwap = async (shift) => {
        const shiftId = getShiftId(shift);
        const swapWithUserId = getShiftEmployeeId(shift);
        const date = toDateKey(getShiftDate(shift));

        const normalizedTimes = normalizeShiftTimes(
            getShiftStartTime(shift),
            getShiftEndTime(shift)
        );

        const startTime = normalizedTimes.startTime || null;
        const endTime = normalizedTimes.endTime || null;
        const shiftKey = buildShiftRequestKey(shift);

        if (!shiftId) {
            alert("This shift has no ID, so a swap request cannot be created yet.");
            return;
        }

        if (requestedShiftKeys.has(shiftKey)) {
            return;
        }

        if (!swapWithUserId) {
            alert("Could not determine which employee owns this shift.");
            return;
        }

        if (!date) {
            alert("Could not determine the shift date.");
            return;
        }

        setRequestingId(shiftId);

        try {
            const payload = {
                userId: Number(employeeId),
                type: "SHIFT_SWAP",
                date,
                startTime,
                endTime,
                startDate: null,
                endDate: null,
                swapWithUserId: Number(swapWithUserId),
                reason: `Swap request with ${getShiftEmployeeName(shift)}`,
            };

            console.log("📦 Swap request payload:", payload);
            console.log("📦 Shift key being saved:", shiftKey);

            await API.post("/requests", payload);

            setRequestedShiftKeys((prev) => {
                const next = new Set(prev);
                next.add(shiftKey);
                return next;
            });

            setMyRequests((prev) => [...prev, payload]);

            alert(
                `Swap request sent for ${getShiftEmployeeName(shift)} on ${formatDayLabel(
                    getShiftDate(shift)
                )}.`
            );
        } catch (error) {
            console.error("Failed to request swap:", error);
            console.error("Status:", error?.response?.status);
            console.error("Data:", error?.response?.data);

            alert(
                error?.response?.data?.message ||
                "Swap request failed. Check backend validation or payload."
            );
        } finally {
            setRequestingId(null);
        }
    };

    if (loadingEmployee || isLoading) {
        return (
            <div className="flex h-full items-center justify-center min-h-[300px]">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-10 h-10 border-4 border-orange-200 border-t-orange-600 rounded-full animate-spin" />
                    <p className="text-gray-500 font-medium animate-pulse">
                        Loading shift swaps...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Shift Swap</h1>
                <p className="text-gray-500">
                    Browse coworker shifts that match dates you are already scheduled to work.
                </p>
            </div>

            {errorMessage && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 text-sm">
                    {errorMessage}
                </div>
            )}

            <div className="bg-white p-5 rounded-xl shadow-sm border border-gray-100">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                    <div className="flex items-center gap-3 text-sm text-gray-600">
                        <div className="p-2 rounded-lg bg-orange-50 text-orange-600">
                            <Users size={18} />
                        </div>
                        <div>
                            <p className="font-medium text-gray-800">
                                {filteredShifts.length} available shift{filteredShifts.length !== 1 ? "s" : ""}
                            </p>
                            <p className="text-xs text-gray-500">
                                Your role: {myRole || "Unknown"}
                            </p>
                        </div>
                    </div>

                    <div className="relative w-full md:w-80">
                        <Search
                            size={16}
                            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
                        />
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            placeholder="Search by employee, role, date..."
                            className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
                        />
                    </div>
                </div>
            </div>

            {filteredShifts.length === 0 ? (
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 text-sm text-gray-600">
                    No swap-eligible shifts found right now.
                </div>
            ) : (
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                    {filteredShifts.map((shift, index) => {
                        const shiftId = getShiftId(shift, index);
                        const employeeName = getShiftEmployeeName(shift);
                        const shiftDate = getShiftDate(shift);
                        const role = getShiftRole(shift);
                        const time = formatTimeRange(
                            getShiftStartTime(shift),
                            getShiftEndTime(shift)
                        );
                        const shiftKey = buildShiftRequestKey(shift);
                        const alreadyRequested = requestedShiftKeys.has(shiftKey);
                        const isSubmitting = requestingId === shiftId;

                        console.log("Shift card key:", shiftKey, "alreadyRequested:", alreadyRequested);

                        return (
                            <div
                                key={shiftId}
                                className="bg-white p-6 rounded-xl shadow-sm border border-gray-100"
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <h3 className="text-lg font-bold text-gray-800">
                                            {employeeName}
                                        </h3>
                                        <p className="text-sm text-gray-500 mt-1">{role}</p>
                                    </div>

                                    <div className="px-3 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">
                                        Available
                                    </div>
                                </div>

                                <div className="mt-5 space-y-3">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 rounded-lg bg-orange-50 text-orange-600">
                                            <Clock size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-900">
                                                {formatDayLabel(shiftDate)}
                                            </p>
                                            <p className="text-xs text-gray-500">{time}</p>
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-5 flex items-center gap-3">
                                    <button
                                        type="button"
                                        onClick={() => handleRequestSwap(shift)}
                                        disabled={isSubmitting || alreadyRequested}
                                        className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
                                            isSubmitting || alreadyRequested
                                                ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                                : "bg-orange-600 text-white hover:bg-orange-700"
                                        }`}
                                    >
                                        {isSubmitting
                                            ? "Requesting..."
                                            : alreadyRequested
                                                ? "Requested"
                                                : "Request Swap"}
                                    </button>

                                    <button
                                        type="button"
                                        onClick={loadData}
                                        className="px-4 py-2 rounded-lg text-sm font-semibold bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 flex items-center gap-2"
                                    >
                                        <RefreshCw size={16} />
                                        Refresh
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}