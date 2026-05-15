import api from './axiosConfig';

/**
 * Fetch the combined weekly summary for reports.
 * @param {string} weekStart - Format "YYYY-MM-DD"
 */
const getWeeklySummary = async (weekStart) => {
    const response = await api.get(`/reports/weekly-summary?weekStart=${weekStart}`);
    return response.data;
};

/**
 * Download the weekly attendance CSV.
 * @param {string} weekStart - Format "YYYY-MM-DD"
 */
const downloadAttendanceCsv = async (weekStart) => {
    const response = await api.get(`/reports/attendance/week/csv?weekStart=${weekStart}`, {
        responseType: 'blob' // Crucial for handling file downloads!
    });
    return response.data;
};

export default {
    getWeeklySummary,
    downloadAttendanceCsv
};