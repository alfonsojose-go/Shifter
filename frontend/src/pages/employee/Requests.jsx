import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../../services/api";
import { RefreshCw } from "lucide-react";

const REQUEST_OPTIONS = [
    { value: "LEAVE_OF_ABSENCE", label: "Leave of Absence" },
    { value: "WANT_TO_WORK", label: "Want to Work" },
];

function safeArray(res) {
    const d = res?.data;
    if (Array.isArray(d)) return d;
    if (Array.isArray(d?.data)) return d.data;
    return [];
}

function statusClasses(status) {
    switch ((status || "").toUpperCase()) {
        case "APPROVED":
            return "bg-green-100 text-green-700";
        case "REJECTED":
            return "bg-red-100 text-red-700";
        default:
            return "bg-yellow-100 text-yellow-700";
    }
}

function formatRequestLabel(type) {
    return (
        REQUEST_OPTIONS.find((opt) => opt.value === type)?.label ||
        type?.replaceAll("_", " ") ||
        "Request"
    );
}

export default function Requests() {
    const navigate = useNavigate();
    const employeeId = Number(localStorage.getItem("employeeId"));

    const [form, setForm] = useState({
        requestType: "LEAVE_OF_ABSENCE",
        date: "",
        startDate: "",
        endDate: "",
        reason: "",
    });

    const [submitting, setSubmitting] = useState(false);
    const [requests, setRequests] = useState([]);
    const [loadingRequests, setLoadingRequests] = useState(true);

    const isLeaveOfAbsence = form.requestType === "LEAVE_OF_ABSENCE";

    const onChange = (e) => {
        const { name, value } = e.target;

        setForm((prev) => {
            const next = { ...prev, [name]: value };

            if (name === "requestType") {
                if (value === "LEAVE_OF_ABSENCE") {
                    next.date = "";
                } else if (value === "WANT_TO_WORK") {
                    next.startDate = "";
                    next.endDate = "";
                }
            }

            return next;
        });
    };

    const resetForm = () => {
        setForm({
            requestType: "LEAVE_OF_ABSENCE",
            date: "",
            startDate: "",
            endDate: "",
            reason: "",
        });
    };

    const loadRequests = async () => {
        if (!employeeId || Number.isNaN(employeeId)) {
            setRequests([]);
            setLoadingRequests(false);
            return;
        }

        try {
            setLoadingRequests(true);
            const res = await API.get(`/requests/user/${employeeId}`);
            setRequests(safeArray(res));
        } catch (err) {
            console.error("Failed to load requests:", err);
            setRequests([]);
        } finally {
            setLoadingRequests(false);
        }
    };

    useEffect(() => {
        loadRequests();
    }, [employeeId]);

    const sortedRequests = useMemo(() => {
        return [...requests].sort((a, b) => (b.id || 0) - (a.id || 0));
    }, [requests]);

    const submit = async (e) => {
        e.preventDefault();

        if (!employeeId || Number.isNaN(employeeId)) {
            alert("Missing employeeId. Please log in again.");
            return;
        }

        if (isLeaveOfAbsence) {
            if (!form.startDate || !form.endDate) {
                alert("Please select a start date and end date.");
                return;
            }

            if (form.endDate < form.startDate) {
                alert("End date must be after start date.");
                return;
            }
        } else {
            if (!form.date) {
                alert("Please select a date.");
                return;
            }
        }

        try {
            setSubmitting(true);

            let payload;

            if (isLeaveOfAbsence) {
                payload = {
                    userId: employeeId,
                    type: "LEAVE_OF_ABSENCE",
                    date: form.startDate, // backend sample still includes date
                    startTime: null,
                    endTime: null,
                    startDate: form.startDate,
                    endDate: form.endDate,
                    swapWithUserId: null,
                    reason: form.reason?.trim() || null,
                };
            } else {
                payload = {
                    userId: employeeId,
                    type: "WANT_TO_WORK",
                    date: form.date,
                    startTime: "09:00",
                    endTime: "17:00",
                    startDate: null,
                    endDate: null,
                    swapWithUserId: null,
                    reason: form.reason?.trim() || null,
                };
            }

            console.log("REQUEST PAYLOAD:", payload);

            const res = await API.post("/requests", payload);
            console.log("REQUEST RESPONSE:", res?.data);

            alert(res?.data?.message || "Request submitted successfully.");

            resetForm();
            await loadRequests();
        } catch (err) {
            console.error("Failed to submit request:", err);

            const backendMsg =
                err.response?.data?.message ||
                err.response?.data?.error ||
                (typeof err.response?.data === "string" ? err.response.data : null) ||
                err.message;

            alert(
                `Failed to submit request.\n\nStatus: ${
                    err.response?.status ?? "n/a"
                }\n${backendMsg}`
            );
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
            {/* LEFT: FORM */}
            <div className="w-full max-w-xl">
                <div className="bg-white rounded-2xl shadow-sm border border-black/5 p-6">
                    <h1 className="text-2xl font-bold text-black">Requests</h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Submit a leave of absence or let your manager know you want to work.
                    </p>

                    <form onSubmit={submit} className="mt-6 space-y-5">
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">
                                Request Type
                            </label>
                            <select
                                name="requestType"
                                value={form.requestType}
                                onChange={onChange}
                                className="w-full px-4 py-3 rounded-lg border border-gray-300 bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none"
                            >
                                {REQUEST_OPTIONS.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {isLeaveOfAbsence ? (
                            <>
                                <div>
                                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                                        Start Date
                                    </label>
                                    <input
                                        type="date"
                                        name="startDate"
                                        value={form.startDate}
                                        onChange={onChange}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-300 bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                                        End Date
                                    </label>
                                    <input
                                        type="date"
                                        name="endDate"
                                        value={form.endDate}
                                        onChange={onChange}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-300 bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none"
                                        required
                                    />
                                </div>
                            </>
                        ) : (
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    Date
                                </label>
                                <input
                                    type="date"
                                    name="date"
                                    value={form.date}
                                    onChange={onChange}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-300 bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none"
                                    required
                                />
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">
                                Reason (Optional)
                            </label>
                            <textarea
                                name="reason"
                                value={form.reason}
                                onChange={onChange}
                                rows={4}
                                placeholder={
                                    isLeaveOfAbsence
                                        ? "Optional note for your leave of absence..."
                                        : "Optional note about wanting more shifts..."
                                }
                                className="w-full px-4 py-3 rounded-lg border border-gray-300 bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none resize-none"
                            />
                        </div>

                        <div className="flex items-center justify-between pt-2">
                            <button
                                type="button"
                                onClick={() => navigate("/emp/dashboard")}
                                className="px-6 py-3 rounded-lg font-semibold text-white bg-red-500 hover:bg-red-600 transition"
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                disabled={submitting}
                                className="px-6 py-3 rounded-lg font-semibold text-white bg-indigo-700 hover:bg-indigo-800 transition disabled:opacity-70 disabled:cursor-not-allowed"
                            >
                                {submitting ? "Submitting..." : "Submit Request"}
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            {/* RIGHT: REQUESTS */}
            <div className="w-full">
                <div className="bg-white rounded-2xl shadow-sm border border-black/5 p-6">
                    <div className="flex items-center justify-between">
                        <h2 className="text-2xl font-bold text-black">My Requests</h2>

                        <button
                            type="button"
                            onClick={loadRequests}
                            className="p-2 rounded-lg border border-gray-200 hover:bg-gray-50"
                        >
                            <RefreshCw size={18} />
                        </button>
                    </div>

                    <div className="mt-6">
                        {loadingRequests ? (
                            <p className="text-gray-500 text-sm">Loading...</p>
                        ) : sortedRequests.length === 0 ? (
                            <p className="text-gray-500 text-sm">No requests yet.</p>
                        ) : (
                            <div className="space-y-4 max-h-[600px] overflow-y-auto">
                                {sortedRequests.map((req) => (
                                    <div key={req.id} className="border rounded-xl p-4">
                                        <div className="flex justify-between gap-3">
                                            <p className="font-semibold">
                                                {formatRequestLabel(req.type)}
                                            </p>
                                            <span
                                                className={`px-3 py-1 text-xs rounded-full whitespace-nowrap ${statusClasses(
                                                    req.status
                                                )}`}
                                            >
                                                {req.status || "PENDING"}
                                            </span>
                                        </div>

                                        <p className="text-sm text-gray-500 mt-1">
                                            {req.startDate
                                                ? `${req.startDate} → ${req.endDate}`
                                                : req.date}
                                        </p>

                                        {req.reason && (
                                            <p className="text-sm mt-2 text-gray-700">
                                                {req.reason}
                                            </p>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}