import api from '../api/axiosConfig';

const BASE_URL = '/scheduling';

/**
 * GET ALL SCHEDULES
 */
const getAllSchedules = async () => {
    const response = await api.get(BASE_URL);
    // Backend wraps response in ApiResponse, so the list is likely inside .data
    return response.data.data || response.data;
};

/**
 * GET SCHEDULES BY EMPLOYEE ID
 */
const getSchedulesByEmployee = async (employeeId) => {
    const response = await api.get(`${BASE_URL}/employee/${employeeId}`);
    return response.data.data || response.data;
};

/**
 * CREATE A SINGLE SCHEDULE
 * @param {number} employeeId
 * @param {Array} schedules - Array of objects: { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' }
 */
const createSchedule = async (employeeId, schedules) => {
    const payload = {
        employeeId: employeeId,
        // Using "schedules" because your backend DTO getter is getSchedules()
        schedules: schedules
    };
    const response = await api.post(BASE_URL, payload);
    return response.data;
};

/**
 * CREATE BATCH SCHEDULES (Multiple employees at once)
 * @param {Array} employees - Array of objects: { employeeId: 1, schedules: [{ dayOfWeek... }] }
 */
const createBatchSchedules = async (employees) => {
    const payload = { employees };
    const response = await api.post(`${BASE_URL}/batch`, payload);
    return response.data;
};

/**
 * UPDATE A SCHEDULE
 * @param {number} scheduleId
 * @param {Object} data - { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' }
 */
const updateSchedule = async (scheduleId, data) => {
    const response = await api.put(`${BASE_URL}/${scheduleId}`, data);
    return response.data;
};

/**
 * DELETE A SCHEDULE
 */
const deleteSchedule = async (scheduleId) => {
    const response = await api.delete(`${BASE_URL}/${scheduleId}`);
    return response.data;
};

/**
 * GET AVAILABLE EMPLOYEES BY DAY
 * @param {string} day - e.g., 'MONDAY'
 */
const getAvailableEmployeesByDay = async (day) => {
    const response = await api.get(`${BASE_URL}/day/${day}/employees`);
    return response.data.data || response.data;
};

export default {
    getAllSchedules,
    getSchedulesByEmployee,
    createSchedule,
    createBatchSchedules,
    updateSchedule,
    deleteSchedule,
    getAvailableEmployeesByDay
};