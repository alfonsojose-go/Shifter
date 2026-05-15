import React, { useEffect, useMemo, useState } from "react";
import API from "../../services/api";

const DAYS = [
    { key: "MONDAY", label: "Mon" },
    { key: "TUESDAY", label: "Tue" },
    { key: "WEDNESDAY", label: "Wed" },
    { key: "THURSDAY", label: "Thu" },
    { key: "FRIDAY", label: "Fri" },
    { key: "SATURDAY", label: "Sat" },
    { key: "SUNDAY", label: "Sun" },
];

const emptyWeek = () =>
    DAYS.reduce((acc, d) => {
        acc[d.key] = [];
        return acc;
    }, {});

function normalizeTime(time) {
    if (!time) return "";
    return String(time).slice(0, 5);
}

function toApiTime(time) {
    const clean = normalizeTime(time);
    return clean ? `${clean}:00` : "";
}

function toAmPm(time24) {
    if (!time24) return "";
    const cleanTime = normalizeTime(time24);
    const [hStr, m] = cleanTime.split(":");
    let h = parseInt(hStr, 10);
    const ampm = h >= 12 ? "PM" : "AM";
    h = h % 12;
    if (h === 0) h = 12;
    return `${h}:${m} ${ampm}`;
}

function overlaps(aStart, aEnd, bStart, bEnd) {
    return aStart < bEnd && bStart < aEnd;
}

function formatDayLabel(dayKey) {
    const found = DAYS.find((d) => d.key === dayKey);
    return found ? found.label : dayKey;
}

export default function Availability() {
    const employeeId = Number(localStorage.getItem("employeeId"));
    const canUseBackend = Number.isFinite(employeeId) && employeeId > 0;

    const [week, setWeek] = useState(() => emptyWeek());
    const [selectedDay, setSelectedDay] = useState("MONDAY");
    const [startTime, setStartTime] = useState("09:00");
    const [endTime, setEndTime] = useState("17:00");

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [editingBlock, setEditingBlock] = useState(null);

    const AVAIL_BASE = "/employee/availabilities";

    const loadAvailability = async () => {
        setLoading(true);
        try {
            if (!canUseBackend) {
                setWeek(emptyWeek());
                return;
            }

            const res = await API.get(`${AVAIL_BASE}/employee/${employeeId}`);
            const rows = res?.data?.data ?? [];

            const next = emptyWeek();

            for (const r of rows) {
                const dayKey = r.dayOfWeek;
                const realId = r.availabilityId ?? r.id;

                if (!next[dayKey]) next[dayKey] = [];

                next[dayKey].push({
                    id: String(realId),
                    start: normalizeTime(r.startTime),
                    end: normalizeTime(r.endTime),
                    _temp: false,
                });
            }

            for (const d of Object.keys(next)) {
                next[d].sort((a, b) => a.start.localeCompare(b.start));
            }

            setWeek(next);
        } catch (err) {
            console.error("LOAD AVAILABILITY ERROR:", err);
            setWeek(emptyWeek());
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAvailability();
    }, [employeeId, canUseBackend]);

    const totalBlocks = useMemo(() => {
        return Object.values(week).reduce((sum, blocks) => sum + blocks.length, 0);
    }, [week]);

    const hasUnsavedBlocks = useMemo(() => {
        return Object.values(week).some((blocks) =>
            blocks.some((b) => String(b.id).startsWith("tmp-"))
        );
    }, [week]);

    const resetForm = () => {
        setSelectedDay("MONDAY");
        setStartTime("09:00");
        setEndTime("17:00");
        setEditingBlock(null);
    };

    const startEdit = (dayKey, block) => {
        setSelectedDay(dayKey);
        setStartTime(normalizeTime(block.start));
        setEndTime(normalizeTime(block.end));
        setEditingBlock({
            id: String(block.id),
            dayKey,
            isTemp: !!block._temp,
        });
    };

    const validateBlock = (dayKey, sTime, eTime, ignoreBlockId = null) => {
        const start = normalizeTime(sTime);
        const end = normalizeTime(eTime);
        const ignoreId = ignoreBlockId != null ? String(ignoreBlockId) : null;

        if (start >= end) {
            alert("End time must be after start time.");
            return false;
        }

        const allBlocks = week[dayKey] || [];

        const hasOverlap = allBlocks.some((b) => {
            const blockId = String(b.id);
            const blockStart = normalizeTime(b.start);
            const blockEnd = normalizeTime(b.end);

            if (ignoreId && blockId === ignoreId) {
                return false;
            }

            return overlaps(start, end, blockStart, blockEnd);
        });

        if (hasOverlap) {
            alert("That time overlaps an existing block on this day.");
            return false;
        }

        return true;
    };

    const addBlock = () => {
        const ok = validateBlock(selectedDay, startTime, endTime);
        if (!ok) return;

        setWeek((prev) => {
            const copy = {
                ...prev,
                [selectedDay]: [...prev[selectedDay]],
            };

            copy[selectedDay].push({
                id: `tmp-${Date.now()}`,
                start: normalizeTime(startTime),
                end: normalizeTime(endTime),
                _temp: true,
            });

            copy[selectedDay].sort((a, b) => a.start.localeCompare(b.start));
            return copy;
        });

        resetForm();
    };

    const updateBlock = async () => {
        if (!editingBlock) return;

        const ok = validateBlock(selectedDay, startTime, endTime, editingBlock.id);
        if (!ok) return;

        try {
            const numericId = Number(editingBlock.id);

            if (!editingBlock.isTemp && canUseBackend && Number.isFinite(numericId)) {
                const payload = {
                    dayOfWeek: selectedDay,
                    startTime: toApiTime(startTime),
                    endTime: toApiTime(endTime),
                };

                console.log("UPDATE AVAILABILITY PAYLOAD:", payload);
                const res = await API.put(`${AVAIL_BASE}/${numericId}`, payload);
                console.log("UPDATE AVAILABILITY RESPONSE:", res?.data);
            }

            resetForm();
            await loadAvailability();
        } catch (err) {
            console.error("UPDATE AVAILABILITY ERROR:", err);

            const backendMsg =
                err.response?.data?.message ||
                err.response?.data?.error ||
                (typeof err.response?.data === "string" ? err.response.data : null) ||
                err.message;

            alert(
                `Failed to update availability.\n\nStatus: ${
                    err.response?.status ?? "n/a"
                }\n${backendMsg}`
            );
        }
    };

    const deleteBlock = async (dayKey, blockId) => {
        const block = week[dayKey].find((b) => String(b.id) === String(blockId));

        if (editingBlock && String(editingBlock.id) === String(blockId)) {
            resetForm();
        }

        setWeek((prev) => ({
            ...prev,
            [dayKey]: prev[dayKey].filter((b) => String(b.id) !== String(blockId)),
        }));

        const numericId = Number(blockId);

        if (canUseBackend && block && !block._temp && Number.isFinite(numericId)) {
            try {
                await API.delete(`${AVAIL_BASE}/${numericId}`);
            } catch (err) {
                console.error("DELETE AVAILABILITY ERROR:", err);
                alert("Failed to delete on server. Refresh to confirm.");
            }
        }
    };

    const submitAll = async () => {
        if (!canUseBackend) {
            alert("Missing employeeId (userId). Login must store it for availability saving.");
            return;
        }

        const payload = {
            employeeId,
            availabilities: [],
        };

        for (const d of DAYS) {
            for (const b of week[d.key]) {
                payload.availabilities.push({
                    dayOfWeek: d.key,
                    startTime: toApiTime(b.start),
                    endTime: toApiTime(b.end),
                });
            }
        }

        setSaving(true);
        try {
            console.log("AVAILABILITY PAYLOAD:", payload);

            const res = await API.post(`${AVAIL_BASE}`, payload);

            alert(res?.data?.message || "Availability saved!");
            resetForm();
            await loadAvailability();
        } catch (err) {
            console.error("SAVE AVAILABILITY ERROR:", err);

            const backendMsg =
                err.response?.data?.message ||
                err.response?.data?.error ||
                (typeof err.response?.data === "string" ? err.response.data : null) ||
                err.message;

            alert(
                `Failed to save availability.\n\nStatus: ${
                    err.response?.status ?? "n/a"
                }\n${backendMsg}`
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="max-w-6xl space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Availability</h1>
                <p className="text-gray-500">
                    Add, edit, or delete your available hours for each day.
                </p>
                {!canUseBackend && (
                    <p className="mt-2 text-sm text-orange-600">
                        Backend saving is disabled because <b>employeeId</b> is missing in
                        localStorage.
                    </p>
                )}
            </div>

            <div className="bg-gray-50 border border-gray-200 rounded-2xl p-6 shadow-sm">
                <div className="flex flex-col gap-4">
                    <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-3">
                        <div>
                            <div className="flex items-center gap-2">
                                <h2 className="text-xl font-semibold text-gray-800">
                                    {editingBlock ? "Edit Availability" : "Add Availability"}
                                </h2>

                                {editingBlock && (
                                    <span className="inline-flex items-center rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                                        Editing Mode
                                    </span>
                                )}
                            </div>

                            <p className="mt-1 text-sm text-gray-500">
                                {editingBlock
                                    ? "Update this block, then save your changes."
                                    : "Choose a day and time range to add a new availability block."}
                            </p>
                        </div>

                        <div className="text-sm text-gray-500 md:text-right">
                            <p>
                                Total blocks this week: <b>{totalBlocks}</b>
                            </p>
                            <p className="mt-1">
                                {hasUnsavedBlocks
                                    ? "You have unsaved availability changes."
                                    : "Your availability is up to date."}
                            </p>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1.5">
                                Day
                            </label>
                            <select
                                value={selectedDay}
                                onChange={(e) => setSelectedDay(e.target.value)}
                                className="w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-gray-800 shadow-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
                            >
                                {DAYS.map((d) => (
                                    <option key={d.key} value={d.key}>
                                        {d.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1.5">
                                Start Time
                            </label>
                            <input
                                type="time"
                                value={startTime}
                                onChange={(e) => setStartTime(e.target.value)}
                                className="w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-gray-800 shadow-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1.5">
                                End Time
                            </label>
                            <input
                                type="time"
                                value={endTime}
                                onChange={(e) => setEndTime(e.target.value)}
                                className="w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-gray-800 shadow-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
                            />
                        </div>
                    </div>

                    <div className="flex flex-col md:flex-row gap-3">
                        {!editingBlock ? (
                            <button
                                type="button"
                                onClick={addBlock}
                                className="w-full md:w-auto min-w-[180px] rounded-xl bg-orange-600 px-5 py-2.5 font-semibold text-white shadow-sm transition hover:bg-orange-700"
                            >
                                Add Block
                            </button>
                        ) : (
                            <>
                                <button
                                    type="button"
                                    onClick={updateBlock}
                                    className="w-full md:w-auto min-w-[180px] rounded-xl bg-blue-600 px-5 py-2.5 font-semibold text-white shadow-sm transition hover:bg-blue-700"
                                >
                                    Update Block
                                </button>

                                <button
                                    type="button"
                                    onClick={resetForm}
                                    className="w-full md:w-auto min-w-[140px] rounded-xl border border-gray-300 bg-white px-5 py-2.5 font-semibold text-gray-700 shadow-sm transition hover:bg-gray-50"
                                >
                                    Cancel Edit
                                </button>
                            </>
                        )}

                        <button
                            type="button"
                            onClick={submitAll}
                            disabled={saving || !hasUnsavedBlocks}
                            className="w-full md:w-auto min-w-[180px] rounded-xl bg-gray-900 px-5 py-2.5 font-semibold text-white shadow-sm transition hover:bg-black disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {saving ? "Saving..." : "Save Availability"}
                        </button>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {DAYS.map((d) => (
                    <div
                        key={d.key}
                        className="bg-white rounded-xl shadow-sm border border-gray-100 p-5"
                    >
                        <div className="flex items-center justify-between">
                            <h3 className="font-bold text-gray-800">{d.label}</h3>
                            <span className="text-xs text-gray-400">
                                {week[d.key].length} blocks
                            </span>
                        </div>

                        <div className="mt-3 min-h-[88px]">
                            {loading ? (
                                <div className="h-20 bg-gray-100 rounded animate-pulse" />
                            ) : week[d.key].length === 0 ? (
                                <div className="h-20 bg-red-100/70 rounded flex items-center justify-center text-xs text-red-600">
                                    No availability
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    {week[d.key].map((b) => {
                                        const isEditing =
                                            editingBlock &&
                                            String(editingBlock.id) === String(b.id);

                                        return (
                                            <div
                                                key={b.id}
                                                className={`rounded-lg p-3 text-xs border ${
                                                    isEditing
                                                        ? "bg-blue-50 border-blue-300"
                                                        : "bg-green-100 border-green-200 text-gray-800"
                                                }`}
                                            >
                                                <div className="flex items-start justify-between gap-3">
                                                    <div>
                                                        <div className="font-semibold">
                                                            {toAmPm(b.start)} - {toAmPm(b.end)}
                                                        </div>
                                                        {b._temp && (
                                                            <div className="mt-1 text-[10px] text-gray-500">
                                                                Not saved yet
                                                            </div>
                                                        )}
                                                    </div>

                                                    <div className="flex gap-2 shrink-0">
                                                        <button
                                                            type="button"
                                                            onClick={() => startEdit(d.key, b)}
                                                            className="px-2.5 py-1 rounded-md bg-white border border-gray-200 text-gray-700 hover:bg-gray-50"
                                                        >
                                                            Edit
                                                        </button>

                                                        <button
                                                            type="button"
                                                            onClick={() => deleteBlock(d.key, b.id)}
                                                            className="px-2.5 py-1 rounded-md bg-white border border-red-200 text-red-600 hover:bg-red-50"
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}