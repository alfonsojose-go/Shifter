import api from './axiosConfig';

// ============================================================================
// 1. WEEKLY TEMPLATES (Generic rules, e.g., "Every Monday")
// ============================================================================
const TEMPLATE_URL = '/scheduling';

const getAllSchedules = async () => {
    const response = await api.get(TEMPLATE_URL);
    return response.data.data || response.data;
};

const createSchedule = async (employeeId, schedules) => {
    const payload = {
        employeeId: employeeId,
        schedules: schedules
    };
    const response = await api.post(TEMPLATE_URL, payload);
    return response.data;
};

const updateSchedule = async (scheduleId, data) => {
    const response = await api.put(`${TEMPLATE_URL}/${scheduleId}`, data);
    return response.data;
};

const deleteSchedule = async (scheduleId) => {
    const response = await api.delete(`${TEMPLATE_URL}/${scheduleId}`);
    return response.data;
};

const deleteAllEmployeeSchedules = async (employeeId) => {
    const response = await api.delete(`${TEMPLATE_URL}/employee/${employeeId}`);
    return response.data;
};


// ============================================================================
// 2. CALENDAR SHIFTS (Specific dates, e.g., "March 15, 2026")
// ============================================================================
const SHIFT_URL = '/scheduling/scheduled_shift';

/**
 * Retrieves all specific calendar shifts WITH position and wage data attached.
 */
const getAllShifts = async () => {
    const response = await api.get(`${SHIFT_URL}/positions`);
    return response.data.data;
};

/**
 * Creates a specific shift on the calendar.
 * @param {object} shiftData - { employeeId, date, startTime, endTime }
 */
const createShift = async (shiftData) => {
    const response = await api.post(SHIFT_URL, shiftData);
    return response.data.data;
};

/**
 * Deletes a specific calendar shift.
 */
const deleteShift = async (shiftId) => {
    const response = await api.delete(`${SHIFT_URL}/${shiftId}`);
    return response.data;
};

export default {
    // Template Exports
    getAllSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    deleteAllEmployeeSchedules,

    // Calendar Shift Exports
    getAllShifts,
    createShift,
    deleteShift
};