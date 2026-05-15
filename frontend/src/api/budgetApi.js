import api from './axiosConfig';

/**
 * Fetch the Labor Cost vs Budget for a specific week.
 * @param {string} weekStart - Format "YYYY-MM-DD"
 */
const getWeeklyLaborCost = async (weekStart) => {
    const response = await api.get(`/reports/labor-cost?weekStart=${weekStart}`);
    return response.data;
};

/**
 * Update the budget for a single day.
 * @param {string} date - Format "YYYY-MM-DD"
 * @param {number} amount - The new budget amount
 */
const updateBudget = async (date, amount) => {
    // The backend uses @RequestParam, so we send it as query parameters
    const response = await api.post(`/budget/update?date=${date}&amount=${amount}`);
    return response.data;
};

export default {
    getWeeklyLaborCost,
    updateBudget
};